package com.ecommerce.order.consumer;

import com.ecommerce.order.config.KafkaTopicConfig;
import com.ecommerce.order.event.InventoryFailedEvent;
import com.ecommerce.order.event.InventoryReservedEvent;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderRepository orderRepository;

    @KafkaListener(topics = KafkaTopicConfig.INVENTORY_RESERVED_TOPIC, groupId = "order-service-group")
    @Transactional
    public void handleInventoryReserved(InventoryReservedEvent event) {
        log.info("Received InventoryReservedEvent for orderNumber={}, eventId={}", 
                event.orderNumber(), event.eventId());

        orderRepository.findByOrderNumber(event.orderNumber()).ifPresentOrElse(order -> {
            order.setStatus(OrderStatus.INVENTORY_RESERVED);
            orderRepository.save(order);
            log.info("Order status updated to INVENTORY_RESERVED for orderNumber={}", event.orderNumber());
        }, () -> log.warn("Order not found for orderNumber={}", event.orderNumber()));
    }

    @KafkaListener(topics = KafkaTopicConfig.INVENTORY_FAILED_TOPIC, groupId = "order-service-group")
    @Transactional
    public void handleInventoryFailed(InventoryFailedEvent event) {
        log.warn("Received InventoryFailedEvent for orderNumber={}, reason={}", 
                event.orderNumber(), event.reason());

        orderRepository.findByOrderNumber(event.orderNumber()).ifPresentOrElse(order -> {
            order.setStatus(OrderStatus.INVENTORY_FAILED);
            orderRepository.save(order);
            log.info("Order status updated to INVENTORY_FAILED for orderNumber={}", event.orderNumber());
        }, () -> log.warn("Order not found for orderNumber={}", event.orderNumber()));
    }
}
