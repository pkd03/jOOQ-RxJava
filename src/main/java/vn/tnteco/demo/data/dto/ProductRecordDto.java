package vn.tnteco.demo.data.dto;

import java.math.BigDecimal;

public record ProductRecordDto(
        Long id,
        String name,
        BigDecimal price,
        Integer stock
) {
}
