package vn.tnteco.demo.wio.rest.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.tnteco.demo.data.dto.OrderRecordDto;

/**
 * Consumer mẫu trong package wio.rest.consumer (theo đúng quy ước cấu trúc dự án).
 * Dùng để tiêu thụ các sự kiện (message queue/event bus) trong hệ thống nếu có.
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    public void onOrderCreated(OrderRecordDto order) {
        log.info("[CONSUMER] Nhận sự kiện đơn hàng mới: orderId={}, userId={}, amount={}",
                order.id(), order.userId(), order.amount());
    }
}
