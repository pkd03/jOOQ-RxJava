package vn.tnteco.demo.service;

import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.tnteco.demo.data.dto.CreateOrderRequest;
import vn.tnteco.demo.data.dto.OrderRecordDto;
import vn.tnteco.demo.data.repository.OrderRepository;
import vn.tnteco.demo.data.repository.ProductRepository;
import vn.tnteco.demo.data.repository.UserRepository;
import vn.tnteco.demo.exception.AppException;
import vn.tnteco.demo.exception.ErrorCode;
import vn.tnteco.demo.validate.OrderRequestValidator;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderRequestValidator orderRequestValidator;

    public OrderService(UserRepository userRepository,
                        ProductRepository productRepository,
                        OrderRepository orderRepository,
                        OrderRequestValidator orderRequestValidator) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderRequestValidator = orderRequestValidator;
    }

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
    public Single<OrderRecordDto> createOrder(CreateOrderRequest request) {
        log.info("[ORDER] Bắt đầu xử lý tạo đơn hàng: userId={}, productId={}, quantity={}",
                request.userId(), request.productId(), request.quantity());

        return Single.fromCallable(() -> {
            // Bước 1: Validate input
            orderRequestValidator.validate(request);
            return request;
        })
        // Bước 2: Kiểm tra User
        .flatMap(req -> userRepository.findById(req.userId())
                .flatMap(userOpt -> {
                    if (userOpt.isEmpty()) {
                        return Single.error(new AppException(ErrorCode.USER_NOT_FOUND,
                                "Không tìm thấy user với id: " + req.userId()));
                    }
                    var user = userOpt.get();
                    if (!user.isActive()) {
                        return Single.error(new AppException(ErrorCode.USER_INACTIVE,
                                "Tài khoản user id=" + req.userId() + " đang bị khóa hoặc không hoạt động"));
                    }
                    return Single.just(user);
                })
        )
        // Bước 3: Kiểm tra Product
        .flatMap(user -> productRepository.findById(request.productId())
                .flatMap(prodOpt -> {
                    if (prodOpt.isEmpty()) {
                        return Single.error(new AppException(ErrorCode.PRODUCT_NOT_FOUND,
                                "Không tìm thấy sản phẩm với id: " + request.productId()));
                    }
                    var product = prodOpt.get();
                    if (product.stock() < request.quantity()) {
                        return Single.error(new AppException(ErrorCode.INSUFFICIENT_STOCK,
                                "Kho chỉ còn " + product.stock() + " sản phẩm, không đủ số lượng yêu cầu: " + request.quantity()));
                    }
                    return Single.just(product);
                })
        )
        // Bước 4: Chạy transaction jOOQ: Trừ kho và tạo Order
        .flatMap(product -> orderRepository.createOrderInTransaction(
                request.userId(),
                request.productId(),
                request.quantity(),
                product.price()
        ))
        .doOnSuccess(order -> log.info("[ORDER] Tạo đơn hàng thành công: orderId={}, amount={}",
                order.id(), order.amount()));
    }
}
