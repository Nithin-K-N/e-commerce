package com.ecommerce.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
    Long id,
    String sku,
    String productName,
    BigDecimal price,
    Integer quantity
) {}
