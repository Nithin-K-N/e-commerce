package com.ecommerce.inventory.dto;

public record StockReservationResponse(
    String sku,
    Integer requestedQuantity,
    Long remainingStock,
    boolean success,
    String message
) {}
