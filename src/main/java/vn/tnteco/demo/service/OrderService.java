package vn.tnteco.demo.service;

import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.tnteco.demo.data.dto.request.CreateOrderRequest;
import vn.tnteco.demo.data.dto.response.OrderResponse;
import vn.tnteco.demo.data.repository.OrderRepository;
import vn.tnteco.demo.data.repository.ProductRepository;
import vn.tnteco.demo.data.repository.UserRepository;
import vn.tnteco.demo.exception.AppException;
import vn.tnteco.demo.exception.ErrorCode;
import vn.tnteco.demo.validate.OrderRequestValidator;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderRequestValidator orderRequestValidator;

    /**
     * API 4: Tạo đơn hàng với chuỗi flatMap tuần tự và Transaction jOOQ.
     *
     * CÁC BƯỚC XỬ LÝ (CHUỖI flatMap):
     * 1. Validate request (quantity > 0, userId, productId) -> ném AppException(INVALID_REQUEST) nếu sai.
     * 2. flatMap 1: Kiểm tra User tồn tại và đang ACTIVE.
     *    - Không tìm thấy -> ném AppException(USER_NOT_FOUND).
     *    - User không ACTIVE -> ném AppException(USER_INACTIVE).
     * 3. flatMap 2: Kiểm tra Product tồn tại và đủ số lượng trong kho.
     *    - Không tìm thấy -> ném AppException(PRODUCT_NOT_FOUND).
     *    - Tồn kho < quantity -> ném AppException(INSUFFICIENT_STOCK).
     * 4. flatMap 3: Gọi orderRepository.createOrderInTransaction(...) để thực hiện:
     *    - Trừ tồn kho và lưu order trong CÙNG 1 TRANSACTION của jOOQ.
     *    - Đảm bảo tính toàn vẹn (ACID) tuyệt đối.
     *
     * =========================================================================
     * TẠI SAO TUYỆT ĐỐI KHÔNG DÙNG RETRY TRÊN THAO TÁC GHI (INSERT / UPDATE ORDER):
     * =========================================================================
     * 1. Tính Non-Idempotent (Không bất biến): Thao tác đọc (GET) có thể gọi nhiều lần mà
     *    không làm thay đổi trạng thái của hệ thống. Ngược lại, thao tác tạo đơn hàng và trừ kho
     *    làm thay đổi trạng thái dữ liệu (State-Mutating).
     * 2. Rủi ro Duplicate & Double-Deduct: Giả sử mạng bị chập chờn hoặc timeout lúc database
     *    đã commit thành công đơn hàng nhưng gói tin phản hồi chưa kịp về tới server/client.
     *    Nếu cấu hình auto-retry:
     *    - Server sẽ gửi tiếp một lệnh insert đơn hàng thứ hai!
     *    - Kho hàng bị trừ 2 lần!
     *    - Khách hàng bị tạo 2 đơn hàng trùng nhau!
     * 3. Do đó, với thao tác ghi: NẾU LỖI THÌ PHẢI BÁO LỖI NGAY cho client/người dùng hoặc
     *    sử dụng cơ chế Idempotency-Key kiểm tra trùng lặp có chủ đích, KHÔNG BAO GIỜ retry ngầm!
     */
    public Single<OrderResponse> createOrder(CreateOrderRequest request) {
        log.info("[ORDER] Bắt đầu xử lý tạo đơn hàng: userId={}, productId={}, quantity={}",
                request.getUserId(), request.getProductId(), request.getQuantity());

        return Single.fromCallable(() -> {
            // Bước 1: Validate input
            orderRequestValidator.validate(request);
            return request;
        })
        // Bước 2: Kiểm tra User
        .flatMap(req -> userRepository.findById(req.getUserId())
                .flatMap(userOpt -> {
                    if (userOpt.isEmpty()) {
                        return Single.error(new AppException(ErrorCode.USER_NOT_FOUND,
                                "Không tìm thấy user với id: " + req.getUserId()));
                    }
                    var user = userOpt.get();
                    if (!user.isActive()) {
                        return Single.error(new AppException(ErrorCode.USER_INACTIVE,
                                "Tài khoản user id=" + req.getUserId() + " đang bị khóa hoặc không hoạt động"));
                    }
                    return Single.just(user);
                })
        )
        // Bước 3: Kiểm tra Product
        .flatMap(user -> productRepository.findById(request.getProductId())
                .flatMap(prodOpt -> {
                    if (prodOpt.isEmpty()) {
                        return Single.error(new AppException(ErrorCode.PRODUCT_NOT_FOUND,
                                "Không tìm thấy sản phẩm với id: " + request.getProductId()));
                    }
                    var product = prodOpt.get();
                    if (product.getStock() < request.getQuantity()) {
                        return Single.error(new AppException(ErrorCode.INSUFFICIENT_STOCK,
                                "Kho chỉ còn " + product.getStock() + " sản phẩm, không đủ số lượng yêu cầu: " + request.getQuantity()));
                    }
                    return Single.just(product);
                })
        )
        // Bước 4: Chạy transaction jOOQ: Trừ kho và tạo Order
        .flatMap(product -> orderRepository.createOrderInTransaction(
                request.getUserId(),
                request.getProductId(),
                request.getQuantity(),
                product.getPrice()
        ))
        .doOnSuccess(order -> log.info("[ORDER] Tạo đơn hàng thành công: orderId={}, amount={}",
                order.getId(), order.getAmount()));
    }
}
