package vn.tnteco.demo.wio.rest;

import io.reactivex.rxjava3.core.Single;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.tnteco.demo.data.dto.response.PageResponse;
import vn.tnteco.demo.data.dto.response.ProductAvailabilityResponse;
import vn.tnteco.demo.data.dto.response.ProductResponse;

/**
 * Interface định nghĩa các endpoint liên quan đến Product.
 */
@RequestMapping("/api/products")
public interface ProductOperations {

    /**
     * API 5: Kiểm tra tính sẵn sàng / tồn kho (minh họa retry lỗi tạm thời & onErrorResumeNext fallback).
     */
    @GetMapping("{id}/availability")
    Single<ProductAvailabilityResponse> getProductAvailability(@PathVariable("id") Long id);

    @GetMapping("{id}")
    public Single<ProductResponse> getProductById(@PathVariable("id") Long id);

    @GetMapping
    public Single<PageResponse<ProductResponse>> getAllProduct(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size);
}
