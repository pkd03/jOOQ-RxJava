package vn.tnteco.demo.wio.rest.controller;

import io.reactivex.rxjava3.core.Single;
import org.springframework.web.bind.annotation.RestController;
import vn.tnteco.demo.data.dto.CreateOrderRequest;
import vn.tnteco.demo.data.dto.OrderRecordDto;
import vn.tnteco.demo.service.OrderService;
import vn.tnteco.demo.wio.rest.OrderOperations;

/**
 * Controller triển khai OrderOperations.
 */
@RestController
public class OrderController implements OrderOperations {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public Single<OrderRecordDto> createOrder(CreateOrderRequest request) {
        return orderService.createOrder(request);
    }
}
