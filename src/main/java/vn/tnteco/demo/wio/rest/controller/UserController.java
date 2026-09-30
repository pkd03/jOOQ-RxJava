package vn.tnteco.demo.wio.rest.controller;

import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import vn.tnteco.demo.data.dto.response.UserDashboardResponse;
import vn.tnteco.demo.data.dto.response.UserProfileResponse;
import vn.tnteco.demo.data.dto.response.UserResponse;
import vn.tnteco.demo.service.UserService;
import vn.tnteco.demo.wio.rest.UserOperations;

/**
 * Controller triển khai UserOperations.
 * Tuân thủ quy ước kiến trúc: Controller chỉ ủy quyền (delegate) sang Service, không chứa nghiệp vụ.
 */
@RestController
@RequiredArgsConstructor
public class UserController implements UserOperations {

    private final UserService userService;

    @Override
    public Single<UserResponse> getUserById(Long id) {
        return userService.getUserById(id);
    }

    @Override
    public Single<UserProfileResponse> getUserProfile(Long id) {
        return userService.getUserProfile(id);
    }

    @Override
    public Single<UserDashboardResponse> getUserDashboard(Long id) {
        return userService.getUserDashboard(id);
    }
}
