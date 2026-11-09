package com.ecommerce.notification.consumer;

import com.ecommerce.notification.event.InventoryFailedEvent;
import com.ecommerce.notification.event.InventoryReservedEvent;
import com.ecommerce.notification.event.OrderPlacedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationEventConsumer {

    @KafkaListener(topics = "order-placed-topic", groupId = "notification-service-group")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Sending Email Notification [ORDER_PLACED]: orderNumber={}, userId={}, totalAmount=\${}, itemsCount={}",
                event.orderNumber(), event.userId(), event.totalAmount(), event.items().size());
    }

    @KafkaListener(topics = "inventory-reserved-topic", groupId = "notification-service-group")
    public void handleInventoryReserved(InventoryReservedEvent event) {
        log.info("Sending Email Notification [ORDER_CONFIRMED]: orderNumber={}, status=SUCCESS, message='{}'",
                event.orderNumber(), event.message());
    }

    @KafkaListener(topics = "inventory-failed-topic", groupId = "notification-service-group")
    public void handleInventoryFailed(InventoryFailedEvent event) {
        log.warn("Sending Email Notification [ORDER_FAILED]: orderNumber={}, status=OUT_OF_STOCK, reason='{}'",
                event.orderNumber(), event.reason());
    }
}
