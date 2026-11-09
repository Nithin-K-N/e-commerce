package com.ecommerce.inventory.consumer;

import com.ecommerce.inventory.dto.StockReservationRequest;
import com.ecommerce.inventory.dto.StockReservationResponse;
import com.ecommerce.inventory.event.InventoryFailedEvent;
import com.ecommerce.inventory.event.InventoryReservedEvent;
import com.ecommerce.inventory.event.OrderPlacedEvent;
import com.ecommerce.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPlacedEventConsumer {

    private final InventoryService inventoryService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public static final String INVENTORY_RESERVED_TOPIC = "inventory-reserved-topic";
    public static final String INVENTORY_FAILED_TOPIC = "inventory-failed-topic";

    @KafkaListener(topics = "order-placed-topic", groupId = "inventory-service-group")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Processing OrderPlacedEvent: orderNumber={}, itemCount={}", 
                event.orderNumber(), event.items().size());

        List<OrderPlacedEvent.OrderItemDto> reservedItems = new ArrayList<>();
        boolean allReserved = true;
        String failureReason = null;

        for (var item : event.items()) {
            StockReservationResponse response = inventoryService.reserveStock(
                    new StockReservationRequest(item.sku(), item.quantity(), event.orderNumber())
            );

            if (response.success()) {
                reservedItems.add(item);
                log.info("Reserved stock for sku={}, qty={}, orderNumber={}", 
                        item.sku(), item.quantity(), event.orderNumber());
            } else {
                allReserved = false;
                failureReason = "Insufficient stock or unavailable SKU: " + item.sku() + " (" + response.message() + ")";
                log.warn("Failed to reserve stock for sku={}, orderNumber={}. Reason: {}", 
                        item.sku(), event.orderNumber(), response.message());
                break;
            }
        }

        if (allReserved) {
            log.info("All items successfully reserved for orderNumber={}. Publishing InventoryReservedEvent", 
                    event.orderNumber());

            InventoryReservedEvent reservedEvent = new InventoryReservedEvent(
                    UUID.randomUUID().toString(),
                    event.orderNumber(),
                    true,
                    "Stock successfully reserved for all items",
                    LocalDateTime.now()
            );

            kafkaTemplate.send(INVENTORY_RESERVED_TOPIC, event.orderNumber(), reservedEvent);
        } else {
            // SAGA COMPENSATION: Roll back any partially reserved items!
            log.warn("Order {} failed stock reservation. Initiating rollback compensation for {} items...",
                    event.orderNumber(), reservedItems.size());

            for (var item : reservedItems) {
                inventoryService.releaseStock(item.sku(), item.quantity(), event.orderNumber());
                log.info("Compensated/Released stock for sku={}, qty={}, orderNumber={}",
                        item.sku(), item.quantity(), event.orderNumber());
            }

            InventoryFailedEvent failedEvent = new InventoryFailedEvent(
                    UUID.randomUUID().toString(),
                    event.orderNumber(),
                    failureReason,
                    LocalDateTime.now()
            );

            kafkaTemplate.send(INVENTORY_FAILED_TOPIC, event.orderNumber(), failedEvent);
        }
    }
}
