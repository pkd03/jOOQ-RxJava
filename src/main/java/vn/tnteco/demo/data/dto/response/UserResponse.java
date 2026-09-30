package vn.tnteco.demo.data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private Integer age;
    private String status;

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
}
