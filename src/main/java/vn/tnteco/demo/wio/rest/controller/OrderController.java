package vn.tnteco.demo.wio.rest.controller;

import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import vn.tnteco.demo.data.dto.request.CreateOrderRequest;
import vn.tnteco.demo.data.dto.response.OrderResponse;
import vn.tnteco.demo.service.OrderService;
import vn.tnteco.demo.wio.rest.OrderOperations;

/**
 * Controller triển khai OrderOperations.
 */
@RestController
@RequiredArgsConstructor
public class OrderController implements OrderOperations {

    private final OrderService orderService;

    @Override
    public Single<OrderResponse> createOrder(CreateOrderRequest request) {
        return orderService.createOrder(request);
    }
}
