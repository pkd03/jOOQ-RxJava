package vn.tnteco.demo.service;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.tnteco.demo.data.dto.response.OrderResponse;
import vn.tnteco.demo.data.dto.response.UserDashboardResponse;
import vn.tnteco.demo.data.dto.response.UserProfileResponse;
import vn.tnteco.demo.data.dto.response.UserResponse;
import vn.tnteco.demo.data.repository.OrderRepository;
import vn.tnteco.demo.data.repository.UserRepository;
import vn.tnteco.demo.exception.AppException;
import vn.tnteco.demo.exception.ErrorCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test cho UserService bằng TestObserver")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, orderRepository);
    }

    @Test
    @DisplayName("flatMap thành công: tìm thấy user và trả về UserResponse")
    void shouldReturnUser_WhenUserExists() {
        // Arrange
        Long userId = 1L;
        UserResponse mockUser = new UserResponse(userId, "Nguyen Van A", "a@tnteco.vn", 28, "ACTIVE");
        when(userRepository.findById(userId)).thenReturn(Single.just(Optional.of(mockUser)));

        // Act: Đăng ký TestObserver vào Single
        TestObserver<UserResponse> observer = userService.getUserById(userId).test();

        // Assert: Kiểm tra bằng TestObserver
        observer.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        observer.assertComplete();
        observer.assertNoErrors();
        observer.assertValue(user -> {
            assertEquals(userId, user.getId());
            assertEquals("Nguyen Van A", user.getName());
            return true;
        });

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("flatMap gặp USER_NOT_FOUND: không tìm thấy user thì ném AppException(USER_NOT_FOUND)")
    void shouldEmitUserNotFound_WhenUserDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        when(userRepository.findById(nonExistentId)).thenReturn(Single.just(Optional.empty()));

        // Act
        TestObserver<UserResponse> observer = userService.getUserById(nonExistentId).test();

        // Assert: RxJava stream phải phát lỗi AppException
        observer.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        observer.assertNotComplete();
        observer.assertError(throwable -> {
            if (throwable instanceof AppException appEx) {
                return appEx.getErrorCode() == ErrorCode.USER_NOT_FOUND;
            }
            return false;
        });
    }

    @Test
    @DisplayName("flatMap + Pair trong Profile: lấy user rồi lấy orders, gộp bằng Pair thành công")
    void shouldReturnUserProfile_UsingFlatMapAndPair() {
        // Arrange
        Long userId = 1L;
        UserResponse mockUser = new UserResponse(userId, "Nguyen Van A", "a@tnteco.vn", 28, "ACTIVE");
        List<OrderResponse> mockOrders = List.of(
                new OrderResponse(101L, userId, 1L, 1, new BigDecimal("1750000"), LocalDateTime.now()),
                new OrderResponse(102L, userId, 2L, 2, new BigDecimal("4600000"), LocalDateTime.now())
        );

        when(userRepository.findById(userId)).thenReturn(Single.just(Optional.of(mockUser)));
        when(orderRepository.findByUserId(userId)).thenReturn(Single.just(mockOrders));

        // Act
        TestObserver<UserProfileResponse> observer = userService.getUserProfile(userId).test();

        // Assert
        observer.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        observer.assertComplete();
        observer.assertNoErrors();
        observer.assertValue(response -> {
            assertEquals(userId, response.getUser().getId());
            assertEquals(2, response.getOrders().size());
            assertEquals(101L, response.getOrders().get(0).getId());
            return true;
        });

        verify(userRepository, times(1)).findById(userId);
        verify(orderRepository, times(1)).findByUserId(userId);
    }

    @Test
    @DisplayName("Single.zip thành công: chạy 3 nguồn song song và gộp đủ kết quả vào UserDashboardResponse")
    void shouldZipSuccessfully_WhenAllThreeSourcesSucceed() {
        // Arrange
        Long userId = 1L;
        UserResponse mockUser = new UserResponse(userId, "Nguyen Van A", "a@tnteco.vn", 28, "ACTIVE");
        List<OrderResponse> mockRecentOrders = List.of(
                new OrderResponse(101L, userId, 1L, 1, new BigDecimal("1750000"), LocalDateTime.now())
        );
        BigDecimal mockTotalSpent = new BigDecimal("6350000");

        when(userRepository.findById(userId)).thenReturn(Single.just(Optional.of(mockUser)));
        when(orderRepository.findRecentOrdersByUserId(userId, 5)).thenReturn(Single.just(mockRecentOrders));
        when(orderRepository.calculateTotalSpentByUserId(userId)).thenReturn(Single.just(mockTotalSpent));

        // Act
        TestObserver<UserDashboardResponse> observer = userService.getUserDashboard(userId).test();

        // Assert
        observer.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        observer.assertComplete();
        observer.assertNoErrors();
        observer.assertValue(dashboard -> {
            assertEquals(userId, dashboard.getUser().getId());
            assertEquals(1, dashboard.getRecentOrders().size());
            assertEquals(new BigDecimal("6350000"), dashboard.getTotalSpent());
            return true;
        });
    }

    @Test
    @DisplayName("Single.zip khi một nguồn lỗi: stream ngắt ngay lập tức và phát ra lỗi của nguồn đó")
    void shouldEmitError_WhenOneSourceFailsInZip() {
        // Arrange: User thành công, RecentOrders thành công, nhưng TotalSpent gặp lỗi DATABASE_ERROR
        Long userId = 1L;
        UserResponse mockUser = new UserResponse(userId, "Nguyen Van A", "a@tnteco.vn", 28, "ACTIVE");
        List<OrderResponse> mockRecentOrders = List.of();

        when(userRepository.findById(userId)).thenReturn(Single.just(Optional.of(mockUser)));
        when(orderRepository.findRecentOrdersByUserId(userId, 5)).thenReturn(Single.just(mockRecentOrders));
        when(orderRepository.calculateTotalSpentByUserId(userId))
                .thenReturn(Single.error(new AppException(ErrorCode.DATABASE_ERROR, "Lỗi tính tổng tiền")));

        // Act
        TestObserver<UserDashboardResponse> observer = userService.getUserDashboard(userId).test();

        // Assert: Toàn bộ zip phải thất bại do 1 nguồn bị lỗi
        observer.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        observer.assertNotComplete();
        observer.assertError(throwable -> {
            if (throwable instanceof AppException appEx) {
                return appEx.getErrorCode() == ErrorCode.DATABASE_ERROR;
            }
            return false;
        });
    }
}
