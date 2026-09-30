package vn.tnteco.demo.exception;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Cấu trúc JSON thống nhất trả về cho client khi có lỗi xảy ra.
 * Tuyệt đối không để lộ stack trace hoặc câu lệnh SQL ra phía client.
 */
public record ErrorResponse(
        String code,
        String message,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        String path
) {
    public static ErrorResponse of(String code, String message, String path) {
        return new ErrorResponse(code, message, LocalDateTime.now(), path);
    }
}
