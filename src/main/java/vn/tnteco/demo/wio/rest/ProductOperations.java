package vn.tnteco.demo.wio.rest;

import io.reactivex.rxjava3.core.Single;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import vn.tnteco.demo.data.dto.ProductAvailabilityResponse;

/**
 * Interface định nghĩa các endpoint liên quan đến Product.
 */
public interface ProductOperations {

    /**
     * API 5: Kiểm tra tính sẵn sàng / tồn kho (minh họa retry lỗi tạm thời & onErrorResumeNext fallback).
     */
    @GetMapping("/api/products/{id}/availability")
    Single<ProductAvailabilityResponse> getProductAvailability(@PathVariable("id") Long id);
}
