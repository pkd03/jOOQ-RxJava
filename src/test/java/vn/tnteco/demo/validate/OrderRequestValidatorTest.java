package vn.tnteco.demo.validate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.tnteco.demo.data.dto.CreateOrderRequest;
import vn.tnteco.demo.exception.AppException;
import vn.tnteco.demo.exception.ErrorCode;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Test cho OrderRequestValidator")
class OrderRequestValidatorTest {

    private OrderRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new OrderRequestValidator();
    }

    @Test
    @DisplayName("quantity <= 0 thì ném INVALID_REQUEST")
    void shouldThrowInvalidRequest_WhenQuantityIsZeroOrNegative() {
        // Test quantity = 0
        CreateOrderRequest requestZero = new CreateOrderRequest(1L, 1L, 0);
        AppException exZero = assertThrows(AppException.class, () -> validator.validate(requestZero));
        assertEquals(ErrorCode.INVALID_REQUEST, exZero.getErrorCode());
        assertTrue(exZero.getMessage().contains("quantity"));

        // Test quantity = -5
        CreateOrderRequest requestNegative = new CreateOrderRequest(1L, 1L, -5);
        AppException exNegative = assertThrows(AppException.class, () -> validator.validate(requestNegative));
        assertEquals(ErrorCode.INVALID_REQUEST, exNegative.getErrorCode());
    }

    @Test
    @DisplayName("userId hoặc productId null thì ném INVALID_REQUEST")
    void shouldThrowInvalidRequest_WhenIdIsNull() {
        CreateOrderRequest nullUser = new CreateOrderRequest(null, 1L, 2);
        AppException exUser = assertThrows(AppException.class, () -> validator.validate(nullUser));
        assertEquals(ErrorCode.INVALID_REQUEST, exUser.getErrorCode());

        CreateOrderRequest nullProduct = new CreateOrderRequest(1L, null, 2);
        AppException exProd = assertThrows(AppException.class, () -> validator.validate(nullProduct));
        assertEquals(ErrorCode.INVALID_REQUEST, exProd.getErrorCode());
    }

    @Test
    @DisplayName("Dữ liệu hợp lệ thì validate thành công không ném lỗi")
    void shouldPass_WhenRequestIsValid() {
        CreateOrderRequest validRequest = new CreateOrderRequest(1L, 2L, 3);
        assertDoesNotThrow(() -> validator.validate(validRequest));
    }
}
