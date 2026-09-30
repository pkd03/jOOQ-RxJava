package vn.tnteco.demo.wio.rest.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.tnteco.demo.data.dto.response.OrderResponse;

/**
 * Consumer mẫu trong package wio.rest.consumer (theo đúng quy ước cấu trúc dự án).
 * Dùng để tiêu thụ các sự kiện (message queue/event bus) trong hệ thống nếu có.
 */
@Slf4j
@Component
public class OrderEventConsumer {

    public void onOrderCreated(OrderResponse order) {
        log.info("[CONSUMER] Nhận sự kiện đơn hàng mới: orderId={}, userId={}, amount={}",
                order.getId(), order.getUserId(), order.getAmount());
    }
}
