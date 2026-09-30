package vn.tnteco.demo.data.dto;

import java.math.BigDecimal;
import java.util.List;

public record UserDashboardResponse(
        UserRecordDto user,
        List<OrderRecordDto> recentOrders,
        BigDecimal totalSpent,
        long executionDurationMs
) {
}
