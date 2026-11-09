package com.ecommerce.inventory.event;

import java.time.LocalDateTime;

public record InventoryFailedEvent(
    String eventId,
    String orderNumber,
    String reason,
    LocalDateTime timestamp
) {}
