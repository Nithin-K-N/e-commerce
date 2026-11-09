package com.ecommerce.inventory.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderPlacedEvent(
    String eventId,
    String orderNumber,
    Long userId,
    BigDecimal totalAmount,
    List<OrderItemDto> items,
    LocalDateTime timestamp
) {
    public record OrderItemDto(
        String sku,
        String productName,
        BigDecimal price,
        Integer quantity
    ) {}
}
