package vn.tnteco.demo.service;

import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import vn.tnteco.demo.data.dto.request.CreateOrderRequest;
import vn.tnteco.demo.data.dto.response.OrderResponse;
import vn.tnteco.demo.data.dto.response.ProductResponse;
import vn.tnteco.demo.data.repository.ProductRepository;
import vn.tnteco.demo.exception.AppException;
import vn.tnteco.demo.exception.ErrorCode;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Integration Test cho OrderService và Transaction Rollback")
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("Tạo order khi kho không đủ -> ném INSUFFICIENT_STOCK và xác nhận Transaction Rollback (kho không bị trừ)")
    void shouldRollbackTransaction_WhenStockIsInsufficient() {
        // Product 3 có stock = 3 trong data.sql
        Long userId = 1L; // User 1 ACTIVE
        Long productId = 3L;
        int initialStock = 3;

        // Xác nhận tồn kho ban đầu
        Optional<ProductResponse> initialProduct = productRepository.findById(productId).blockingGet();
        assertTrue(initialProduct.isPresent());
        assertEquals(initialStock, initialProduct.get().getStock());

        // Yêu cầu mua số lượng 10 (vượt quá tồn kho 3)
        CreateOrderRequest request = new CreateOrderRequest(userId, productId, 10);

        // Act: Đăng ký TestObserver
        TestObserver<OrderResponse> observer = orderService.createOrder(request).test();

        // Assert: Stream phải nhận lỗi INSUFFICIENT_STOCK
        observer.awaitDone(3, java.util.concurrent.TimeUnit.SECONDS);
        observer.assertNotComplete();
        observer.assertError(throwable -> {
            if (throwable instanceof AppException appEx) {
                return appEx.getErrorCode() == ErrorCode.INSUFFICIENT_STOCK;
            }
            return false;
        });

        // QUAN TRỌNG: Kiểm tra lại trong DB xem tồn kho có bị trừ hay không (xác nhận transaction đã rollback)
        Optional<ProductResponse> productAfterRollback = productRepository.findById(productId).blockingGet();
        assertTrue(productAfterRollback.isPresent());
        assertEquals(initialStock, productAfterRollback.get().getStock(),
                "Tồn kho của sản phẩm không được thay đổi do Transaction jOOQ đã rollback thành công!");
    }

    @Test
    @DisplayName("Tạo order thành công: trừ kho chính xác và lưu đơn hàng")
    void shouldCreateOrderAndDeductStockSuccessfully() {
        // Product 1 có stock = 15, giá = 1750000.00
        Long userId = 1L;
        Long productId = 1L;
        int orderQuantity = 2;

        CreateOrderRequest request = new CreateOrderRequest(userId, productId, orderQuantity);

        // Act
        TestObserver<OrderResponse> observer = orderService.createOrder(request).test();

        // Assert
        observer.awaitDone(3, java.util.concurrent.TimeUnit.SECONDS);
        observer.assertComplete();
        observer.assertNoErrors();
        observer.assertValue(order -> {
            assertNotNull(order.getId());
            assertEquals(userId, order.getUserId());
            assertEquals(productId, order.getProductId());
            assertEquals(orderQuantity, order.getQuantity());
            assertEquals(new BigDecimal("3500000.00"), order.getAmount());
            return true;
        });

        // Kiểm tra tồn kho đã bị trừ từ 15 xuống 13
        Optional<ProductResponse> updatedProduct = productRepository.findById(productId).blockingGet();
        assertTrue(updatedProduct.isPresent());
        assertEquals(13, updatedProduct.get().getStock());
    }

    @Test
    @DisplayName("Tạo order cho User không hoạt động (INACTIVE) -> ném USER_INACTIVE")
    void shouldThrowUserInactive_WhenUserIsInactive() {
        // User 3 là INACTIVE trong data.sql
        CreateOrderRequest request = new CreateOrderRequest(3L, 1L, 1);

        TestObserver<OrderResponse> observer = orderService.createOrder(request).test();

        observer.awaitDone(3, java.util.concurrent.TimeUnit.SECONDS);
        observer.assertNotComplete();
        observer.assertError(throwable -> {
            if (throwable instanceof AppException appEx) {
                return appEx.getErrorCode() == ErrorCode.USER_INACTIVE;
            }
            return false;
        });
    }
}
