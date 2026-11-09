package com.ecommerce.order.event;

import java.time.LocalDateTime;

public record InventoryReservedEvent(
    String eventId,
    String orderNumber,
    boolean success,
    String message,
    LocalDateTime timestamp
) {}
