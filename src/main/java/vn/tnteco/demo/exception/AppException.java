package vn.tnteco.demo.exception;

/**
 * Exception tùy chỉnh chứa ErrorCode chuẩn của hệ sinh thái backend.
 * Cho phép kèm theo chi tiết bổ sung (detail) nếu cần thiết để log hoặc mô tả rõ hơn.
 */
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, String customMessage) {
        super(customMessage != null && !customMessage.isBlank() ? customMessage : errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
