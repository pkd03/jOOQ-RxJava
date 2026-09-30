package vn.tnteco.demo.data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAvailabilityResponse {

    private Long productId;
    private String productName;
    private Integer stock;
    private boolean available;
    private String note;
}
