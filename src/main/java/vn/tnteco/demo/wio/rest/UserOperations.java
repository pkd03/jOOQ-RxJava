package vn.tnteco.demo.wio.rest;

import io.reactivex.rxjava3.core.Single;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import vn.tnteco.demo.data.dto.response.UserDashboardResponse;
import vn.tnteco.demo.data.dto.response.UserProfileResponse;
import vn.tnteco.demo.data.dto.response.UserResponse;

/**
 * Interface định nghĩa các endpoint liên quan đến User theo chuẩn thiết kế API.
 * Khai báo kiểu trả về Reactive Single<T> và các Spring MVC Annotation (@GetMapping, @PathVariable...).
 */
public interface UserOperations {

    /**
     * API 1: Lấy thông tin chi tiết user theo ID.
     */
    @GetMapping("/api/users/{id}")
    Single<UserResponse> getUserById(@PathVariable("id") Long id);

    /**
     * API 2: Lấy profile user kèm danh sách đơn hàng (minh họa flatMap + Pair).
     */
    @GetMapping("/api/users/{id}/profile")
    Single<UserProfileResponse> getUserProfile(@PathVariable("id") Long id);

    /**
     * API 3: Lấy dashboard tổng quan (minh họa Single.zip chạy song song 3 truy vấn độc lập).
     */
    @GetMapping("/api/users/{id}/dashboard")
    Single<UserDashboardResponse> getUserDashboard(@PathVariable("id") Long id);
}
