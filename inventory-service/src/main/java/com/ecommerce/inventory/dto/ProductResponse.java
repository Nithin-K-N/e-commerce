package com.ecommerce.inventory.dto;

import java.math.BigDecimal;

public record ProductResponse(
    Long id,
    String sku,
    String name,
    String description,
    BigDecimal price,
    Integer totalStock,
    Integer availableStock
) {}
