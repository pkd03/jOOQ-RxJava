package vn.tnteco.demo.data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDashboardResponse {

    private UserResponse user;
    private List<OrderResponse> recentOrders;
    private BigDecimal totalSpent;
    private long executionDurationMs;
}
