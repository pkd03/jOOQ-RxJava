package vn.tnteco.demo.wio.rest.controller;

import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import vn.tnteco.demo.data.dto.response.ProductAvailabilityResponse;
import vn.tnteco.demo.service.ProductService;
import vn.tnteco.demo.wio.rest.ProductOperations;

/**
 * Controller triển khai ProductOperations.
 */
@RestController
@RequiredArgsConstructor
public class ProductController implements ProductOperations {

    private final ProductService productService;

    @Override
    public Single<ProductAvailabilityResponse> getProductAvailability(Long id) {
        return productService.checkAvailability(id);
    }
}
