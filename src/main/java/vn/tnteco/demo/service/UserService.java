package vn.tnteco.demo.service;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import vn.tnteco.demo.data.dto.response.OrderResponse;
import vn.tnteco.demo.data.dto.response.UserDashboardResponse;
import vn.tnteco.demo.data.dto.response.UserProfileResponse;
import vn.tnteco.demo.data.dto.response.UserResponse;
import vn.tnteco.demo.data.repository.OrderRepository;
import vn.tnteco.demo.data.repository.UserRepository;
import vn.tnteco.demo.exception.AppException;
import vn.tnteco.demo.exception.ErrorCode;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    /**
     * API 1: Lấy thông tin user theo ID.
     *
     * GIẢI THÍCH TẠI SAO DÙNG flatMap:
     * - Repository trả về Single<Optional<UserResponse>> để tránh null rò rỉ vào RxJava.
     * - Ở tầng Service, ta cần giải nén Optional:
     *   + Nếu có dữ liệu: chuyển thành Single.just(user).
     *   + Nếu rỗng: chuyển thành Single.error(AppException).
     * - Nếu dùng toán tử map(), kết quả trả về sẽ bị lồng: Single<Single<UserResponse>>.
     * - Toán tử flatMap() nhận vào 1 giá trị và trả về 1 Single mới, đồng thời tự động "làm phẳng" (flatten)
     *   để kiểu trả về cuối cùng là Single<UserResponse>.
     */
    public Single<UserResponse> getUserById(Long id) {
        return userRepository.findById(id)
                .flatMap(userOpt -> userOpt
                        .map(Single::just)
                        .orElseGet(() -> Single.error(new AppException(ErrorCode.USER_NOT_FOUND,
                                "Không tìm thấy người dùng với id: " + id))));
    }

    /**
     * API 2: Lấy thông tin Profile (gồm thông tin User và danh sách Orders).
     *
     * GIẢI THÍCH TẠI SAO DÙNG flatMap:
     * - Bước 1: Lấy user qua getUserById(id).
     * - Bước 2: Chỉ khi bước 1 hoàn thành và tìm thấy user hợp lệ, ta mới lấy tiếp danh sách order của user đó.
     *   Đây là quan hệ PHỤ THUỘC TUẦN TỰ (Sequential Dependency: kết quả bước sau phụ thuộc dữ liệu bước trước).
     * - flatMap nhận kết quả của bước 1 (UserResponse) và kích hoạt luồng bất đồng bộ tiếp theo (Single<List<OrderResponse>>).
     *
     * GIẢI THÍCH TẠI SAO DÙNG Pair:
     * - Sau khi bước 2 thực hiện xong, ta có List<OrderResponse>. Tuy nhiên, để tạo UserProfileResponse,
     *   ta cần CẢ UserResponse (ở bước 1) VÀ List<OrderResponse> (ở bước 2).
     * - Trong lập trình hàm, scope của biến bước 1 sẽ bị mất nếu không truyền tiếp.
     * - Pair.of(user, orders) của Apache Commons Lang đóng vai trò như một bộ đôi mang theo (carry)
     *   cả 2 đối tượng qua các bước trong pipeline mà không cần phải khai báo class DTO phụ tạm thời.
     */
    public Single<UserProfileResponse> getUserProfile(Long id) {
        return getUserById(id)
                // flatMap chuyển tiếp sang lấy orders khi user đã hợp lệ
                .flatMap(user -> orderRepository.findByUserId(user.getId())
                        // Pair mang theo cả (user, orders) sang bước tiếp theo
                        .map(orders -> Pair.of(user, orders)))
                // Cuối cùng gom Pair thành UserProfileResponse
                .map(pair -> new UserProfileResponse(pair.getLeft(), pair.getRight()));
    }

    /**
     * API 3: Lấy thông tin Dashboard (User + 5 Orders gần nhất + Tổng tiền đã chi).
     *
     * GIẢI THÍCH TẠI SAO DÙNG Single.zip:
     * - Khác với API Profile (cần user rồi mới lấy orders), 3 truy vấn ở đây hoàn toàn ĐỘC LẬP:
     *   1. Thông tin user (Single<UserResponse>)
     *   2. 5 đơn hàng gần nhất (Single<List<OrderResponse>>)
     *   3. Tổng chi tiêu (Single<BigDecimal>)
     * - Nếu dùng flatMap tuần tự, tổng thời gian = T1 + T2 + T3.
     * - Khi dùng Single.zip kết hợp subscribeOn(Schedulers.io()) riêng cho từng nguồn, cả 3 truy vấn
     *   sẽ được thực thi ĐỒNG THỜI (PARALLEL) trên các thread khác nhau trong pool IO.
     * - Tổng thời gian chỉ xấp xỉ bằng MAX(T1, T2, T3), tối ưu hiệu năng tối đa.
     * - Zip sẽ đợi cả 3 nguồn hoàn tất rồi kết hợp (combine) kết quả qua Zipper function lambda.
     *   Nếu bất kỳ nguồn nào gặp lỗi, Single.zip ngắt ngay và đẩy lỗi xuống downstream.
     */
    public Single<UserDashboardResponse> getUserDashboard(Long id) {
        long startTime = System.currentTimeMillis();
        log.info("[DASHBOARD] Bắt đầu tổng hợp dashboard cho userId={}", id);

        // Nguồn 1: Lấy user (có subscribeOn IO riêng)
        Single<UserResponse> singleUser = getUserById(id)
                .subscribeOn(Schedulers.io())
                .doOnSuccess(u -> log.info("[DASHBOARD] Luồng 1 (User) hoàn tất trên thread: {}", Thread.currentThread().getName()));

        // Nguồn 2: Lấy 5 đơn gần nhất (có subscribeOn IO riêng)
        Single<List<OrderResponse>> singleRecentOrders = orderRepository.findRecentOrdersByUserId(id, 5)
                .subscribeOn(Schedulers.io())
                .doOnSuccess(orders -> log.info("[DASHBOARD] Luồng 2 (RecentOrders) hoàn tất trên thread: {}", Thread.currentThread().getName()));

        // Nguồn 3: Tính tổng tiền chi tiêu (có subscribeOn IO riêng)
        Single<BigDecimal> singleTotalSpent = orderRepository.calculateTotalSpentByUserId(id)
                .subscribeOn(Schedulers.io())
                .doOnSuccess(total -> log.info("[DASHBOARD] Luồng 3 (TotalSpent) hoàn tất trên thread: {}", Thread.currentThread().getName()));

        // Gộp 3 nguồn chạy song song bằng Single.zip
        return Single.zip(singleUser, singleRecentOrders, singleTotalSpent, (user, orders, total) -> {
            long duration = System.currentTimeMillis() - startTime;
            log.info("[DASHBOARD] Hoàn tất gộp cả 3 nguồn song song cho userId={} trong {} ms", id, duration);
            return new UserDashboardResponse(user, orders, total, duration);
        });
    }
}
