package com.ecommerce.order.producer;

import com.ecommerce.order.config.KafkaTopicConfig;
import com.ecommerce.order.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderPlaced(OrderPlacedEvent event) {
        log.info("Publishing OrderPlacedEvent for orderNumber={}, eventId={}", 
                event.orderNumber(), event.eventId());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                KafkaTopicConfig.ORDER_PLACED_TOPIC, 
                event.orderNumber(), 
                event
        );

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully published OrderPlacedEvent for orderNumber={} to partition={}",
                        event.orderNumber(),
                        result.getRecordMetadata().partition());
            } else {
                log.error("Failed to publish OrderPlacedEvent for orderNumber={}: {}",
                        event.orderNumber(), ex.getMessage(), ex);
            }
        });
    }
}
