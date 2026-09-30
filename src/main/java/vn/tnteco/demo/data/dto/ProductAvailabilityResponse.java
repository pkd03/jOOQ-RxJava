package vn.tnteco.demo.data.dto;

public record ProductAvailabilityResponse(
        Long productId,
        String productName,
        Integer stock,
        boolean available,
        String note
) {
}
