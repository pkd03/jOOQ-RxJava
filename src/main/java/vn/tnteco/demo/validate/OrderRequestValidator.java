package vn.tnteco.demo.validate;

import org.springframework.stereotype.Component;
import vn.tnteco.demo.data.dto.request.CreateOrderRequest;
import vn.tnteco.demo.exception.AppException;
import vn.tnteco.demo.exception.ErrorCode;

/**
 * Validator nghiệp vụ cho yêu cầu tạo đơn hàng.
 * Đặt tại package 'validate' theo đúng quy ước cấu trúc dự án.
 */
@Component
public class OrderRequestValidator {

    /**
     * Kiểm tra tính hợp lệ của CreateOrderRequest.
     * Ném AppException(INVALID_REQUEST) nếu dữ liệu không thỏa mãn.
     */
    public void validate(CreateOrderRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Yêu cầu tạo đơn hàng không được để trống (null)");
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "userId không hợp lệ hoặc để trống");
        }
        if (request.getProductId() == null || request.getProductId() <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "productId không hợp lệ hoặc để trống");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Số lượng đặt hàng (quantity) phải lớn hơn 0");
        }
    }
}
