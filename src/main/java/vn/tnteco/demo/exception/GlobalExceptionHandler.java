package vn.tnteco.demo.exception;

import io.reactivex.rxjava3.exceptions.CompositeException;
import io.reactivex.rxjava3.exceptions.UndeliverableException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jooq.exception.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.concurrent.ExecutionException;

/**
 * Bộ xử lý ngoại lệ tập trung (@RestControllerAdvice) cho toàn bộ ứng dụng.
 * Đảm bảo:
 * 1. Định dạng JSON trả về cho client luôn thống nhất theo ErrorResponse.
 * 2. Không để lộ SQL hoặc StackTrace ra ngoài; ghi log đầy đủ ở server để debug.
 * 3. Hỗ trợ giải nén (unwrap) các ngoại lệ đặc trưng của RxJava 3 như CompositeException
 *    (sinh ra khi Single.zip có nhiều nguồn cùng bắn lỗi) hoặc UndeliverableException.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Bắt AppException nghiệp vụ chủ động ném ra trong luồng xử lý.
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("AppException xảy ra tại [{}]: code={}, message={}",
                request.getRequestURI(), errorCode.getCode(), ex.getMessage());

        ErrorResponse response = ErrorResponse.of(errorCode.getCode(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    /**
     * Bắt lỗi validation dữ liệu đầu vào (Spring Validation).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String detailMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse(ErrorCode.INVALID_REQUEST.getMessage());

        log.warn("Lỗi validate đầu vào tại [{}]: {}", request.getRequestURI(), detailMessage);

        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_REQUEST.getCode(), detailMessage, request.getRequestURI());
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getHttpStatus()).body(response);
    }

    /**
     * Bắt lỗi jOOQ DataAccessException (lỗi SQL, vi phạm khóa ngoại, mất kết nối cơ sở dữ liệu...).
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException ex, HttpServletRequest request) {
        // Chỉ log chi tiết SQL lỗi ở server, không gửi thông tin schema/SQL ra ngoài client
        log.error("Lỗi cơ sở dữ liệu (jOOQ) tại [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.DATABASE_ERROR.getCode(),
                ErrorCode.DATABASE_ERROR.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(ErrorCode.DATABASE_ERROR.getHttpStatus()).body(response);
    }

    /**
     * Bắt CompositeException của RxJava 3 (khi Single.zip hoặc nhiều nguồn reactive cùng bắn lỗi đồng thời).
     */
    @ExceptionHandler(CompositeException.class)
    public ResponseEntity<ErrorResponse> handleCompositeException(CompositeException ex, HttpServletRequest request) {
        log.warn("RxJava CompositeException chứa {} ngoại lệ tại [{}]", ex.getExceptions().size(), request.getRequestURI());

        // Tìm ngoại lệ AppException đầu tiên trong danh sách lỗi tổng hợp
        for (Throwable cause : ex.getExceptions()) {
            Throwable unwrapped = unwrap(cause);
            if (unwrapped instanceof AppException appEx) {
                return handleAppException(appEx, request);
            }
        }

        // Nếu không có AppException, xử lý như lỗi hệ thống
        return handleGenericException(ex, request);
    }

    /**
     * Bắt UndeliverableException từ RxJava.
     */
    @ExceptionHandler(UndeliverableException.class)
    public ResponseEntity<ErrorResponse> handleUndeliverableException(UndeliverableException ex, HttpServletRequest request) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        if (cause instanceof AppException appEx) {
            return handleAppException(appEx, request);
        }
        return handleGenericException(cause, request);
    }

    /**
     * Bắt tất cả các exception chưa được phân loại khác.
     */
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Throwable ex, HttpServletRequest request) {
        Throwable cause = unwrap(ex);
        if (cause instanceof AppException appEx) {
            return handleAppException(appEx, request);
        }
        if (cause instanceof DataAccessException dataEx) {
            return handleDataAccessException(dataEx, request);
        }

        log.error("Lỗi không lường trước tại [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.INTERNAL_ERROR.getCode(),
                ErrorCode.INTERNAL_ERROR.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus()).body(response);
    }

    /**
     * Hàm phụ trợ giải nén ExecutionException, CompositeException lồng nhau để tìm Root Cause.
     */
    private Throwable unwrap(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        if (throwable instanceof ExecutionException && throwable.getCause() != null) {
            return unwrap(throwable.getCause());
        }
        if (throwable instanceof UndeliverableException && throwable.getCause() != null) {
            return unwrap(throwable.getCause());
        }
        if (throwable instanceof CompositeException comp && !comp.getExceptions().isEmpty()) {
            return unwrap(comp.getExceptions().get(0));
        }
        return throwable;
    }
}
