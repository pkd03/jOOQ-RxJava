package vn.tnteco.demo.data.repository;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import vn.tnteco.demo.data.dto.response.UserResponse;
import vn.tnteco.demo.jooq.tables.records.UsersRecord;

import java.util.Optional;

import static vn.tnteco.demo.jooq.Tables.USERS;

/**
 * Repository thao tác bảng USERS thông qua jOOQ.
 *
 * Nguyên tắc RxJava 3 + jOOQ:
 * 1. jOOQ là thư viện JDBC blocking (chặn luồng). Do đó, TẤT CẢ các truy vấn đều phải được bọc
 *    trong Single.fromCallable(...) và chuyển sang Schedulers.io() để không làm tắc nghẽn luồng xử lý web (Event Loop/Tomcat thread).
 * 2. RxJava 3 TUYỆT ĐỐI KHÔNG CHO PHÉP giá trị null chảy trong stream (sẽ ném NullPointerException ngay lập tức).
 *    Vì vậy, với truy vấn tìm 1 phần tử có thể không tồn tại, repository LUÔN trả về Single<Optional<T>>
 *    bằng cách dùng fetchOptional() của jOOQ.
 */
@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final DSLContext dsl;

    /**
     * Tìm user theo ID.
     * Trả về Single<Optional<UserResponse>> để tránh null rò rỉ vào RxJava.
     */
    public Single<Optional<UserResponse>> findById(Long id) {
        return Single.fromCallable(() -> {
            Optional<UsersRecord> recordOpt = dsl.selectFrom(USERS)
                    .where(USERS.ID.eq(id))
                    .fetchOptional();

            return recordOpt.map(this::mapToDto);
        }).subscribeOn(Schedulers.io());
    }

    private UserResponse mapToDto(UsersRecord r) {
        return UserResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .email(r.getEmail())
                .age(r.getAge())
                .status(r.getStatus())
                .build();
    }
}
