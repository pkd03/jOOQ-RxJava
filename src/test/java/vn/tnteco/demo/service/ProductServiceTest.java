package vn.tnteco.demo.service;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.tnteco.demo.data.dto.response.ProductAvailabilityResponse;
import vn.tnteco.demo.data.dto.response.ProductResponse;
import vn.tnteco.demo.data.repository.ProductRepository;
import vn.tnteco.demo.exception.AppException;
import vn.tnteco.demo.exception.ErrorCode;

import java.math.BigDecimal;
import java.sql.SQLTransientException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test cho ProductService (Retry và Fallback) bằng TestObserver")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    @Test
    @DisplayName("retry: nguồn ném lỗi tạm thời 2 lần đầu và thành công ở lần thứ 3")
    void shouldRetryTwiceAndSucceedOnThirdAttempt() {
        Long productId = 1L;
        AtomicInteger attemptCounter = new AtomicInteger(0);

        // Giả lập: Lần 1 và Lần 2 ném lỗi tạm thời SQLTransientException, Lần 3 thành công
        when(productRepository.findById(productId)).thenAnswer(invocation -> {
            int attempt = attemptCounter.incrementAndGet();
            if (attempt <= 2) {
                return Single.error(new SQLTransientException("Mất kết nối tạm thời lần " + attempt));
            } else {
                return Single.just(Optional.of(new ProductResponse(
                        productId,
                        "Ban phim co Keychron K2",
                        new BigDecimal("1750000"),
                        15
                )));
            }
        });

        // Act: Gọi checkAvailability và gắn TestObserver
        TestObserver<ProductAvailabilityResponse> observer = productService.checkAvailability(productId).test();

        // Assert
        observer.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        observer.assertComplete();
        observer.assertNoErrors();
        observer.assertValue(response -> {
            assertEquals(productId, response.getProductId());
            assertTrue(response.isAvailable());
            assertEquals(15, response.getStock());
            assertTrue(response.getNote().contains("Còn hàng"));
            return true;
        });

        // Xác nhận đã thực sự thử lại đúng 3 lần (1 lần gốc + 2 lần retry)
        assertEquals(3, attemptCounter.get());
    }

    @Test
    @DisplayName("fallback với onErrorResumeNext: lỗi tạm thời vượt quá số lần retry thì trả về dữ liệu fallback an toàn")
    void shouldTriggerFallback_WhenRetryExhausted() {
        Long productId = 1L;
        AtomicInteger attemptCounter = new AtomicInteger(0);

        // Ném lỗi tạm thời liên tục ở tất cả các lần gọi
        when(productRepository.findById(productId)).thenAnswer(invocation -> {
            attemptCounter.incrementAndGet();
            return Single.error(new SQLTransientException("Hệ thống database đang bảo trì khẩn cấp"));
        });

        // Act
        TestObserver<ProductAvailabilityResponse> observer = productService.checkAvailability(productId).test();

        // Assert: Stream vẫn hoàn thành thành công (không bắn lỗi 500) nhờ onErrorResumeNext fallback
        observer.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        observer.assertComplete();
        observer.assertNoErrors();
        observer.assertValue(response -> {
            assertEquals(productId, response.getProductId());
            assertFalse(response.isAvailable());
            assertTrue(response.getNote().contains("bảo trì"));
            return true;
        });

        // Đã thử 3 lần (1 gốc + 2 retry) rồi mới fallback
        assertEquals(3, attemptCounter.get());
    }

    @Test
    @DisplayName("Lỗi PRODUCT_NOT_FOUND là lỗi nghiệp vụ: không retry và giữ nguyên ngoại lệ cho client")
    void shouldNotRetry_WhenProductNotFound() {
        Long nonExistentId = 99L;
        AtomicInteger attemptCounter = new AtomicInteger(0);

        when(productRepository.findById(nonExistentId)).thenAnswer(invocation -> {
            attemptCounter.incrementAndGet();
            return Single.just(Optional.empty()); // Sẽ biến đổi thành PRODUCT_NOT_FOUND
        });

        TestObserver<ProductAvailabilityResponse> observer = productService.checkAvailability(nonExistentId).test();

        observer.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        observer.assertNotComplete();
        observer.assertError(throwable -> {
            if (throwable instanceof AppException appEx) {
                return appEx.getErrorCode() == ErrorCode.PRODUCT_NOT_FOUND;
            }
            return false;
        });

        // Không retry với lỗi nghiệp vụ, chỉ gọi đúng 1 lần
        assertEquals(1, attemptCounter.get());
    }
}
