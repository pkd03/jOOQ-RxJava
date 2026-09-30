package vn.tnteco.demo.wio.rest;

import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import vn.tnteco.demo.data.dto.CreateOrderRequest;
import vn.tnteco.demo.data.dto.OrderRecordDto;

/**
 * Interface định nghĩa các endpoint liên quan đến Order.
 */
public interface OrderOperations {

    /**
     * API 4: Tạo đơn hàng mới với chuỗi flatMap kiểm tra tuần tự và transaction jOOQ.
     */
    @PostMapping("/api/orders")
    Single<OrderRecordDto> createOrder(@Valid @RequestBody CreateOrderRequest request);
}
