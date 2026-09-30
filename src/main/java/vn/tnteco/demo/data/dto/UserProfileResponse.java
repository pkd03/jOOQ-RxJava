package vn.tnteco.demo.data.dto;

import java.util.List;

public record UserProfileResponse(
        UserRecordDto user,
        List<OrderRecordDto> orders
) {
}
