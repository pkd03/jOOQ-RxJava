package vn.tnteco.demo.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Danh sách mã lỗi chuẩn của hệ thống.
 * Mỗi mã lỗi gồm:
 * - code: Định danh chuỗi ổn định (dành cho client phân loại lỗi theo logic phần mềm)
 * - httpStatus: Mã trạng thái HTTP tương ứng
 * - message: Thông điệp mô tả thân thiện, rõ ràng
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    USER_NOT_FOUND("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User không tồn tại"),
    USER_INACTIVE("USER_INACTIVE", HttpStatus.UNPROCESSABLE_ENTITY, "Tài khoản user đang bị khóa hoặc không hoạt động"),
    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", HttpStatus.NOT_FOUND, "Sản phẩm không tồn tại"),
    INSUFFICIENT_STOCK("INSUFFICIENT_STOCK", HttpStatus.CONFLICT, "Số lượng sản phẩm trong kho không đủ"),
    INVALID_REQUEST("INVALID_REQUEST", HttpStatus.BAD_REQUEST, "Dữ liệu yêu cầu không hợp lệ"),
    DATABASE_ERROR("DATABASE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi cơ sở dữ liệu"),
    INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống nội bộ");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
