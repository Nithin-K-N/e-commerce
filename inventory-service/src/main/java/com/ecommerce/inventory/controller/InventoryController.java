package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.*;
import com.ecommerce.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory", description = "Endpoints for catalog management, real-time stock inquiry, and flash-sale reservation")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/products")
    @Operation(summary = "Create product", description = "Registers a new product in the database and seeds initial stock into Redis")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Product created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or SKU already exists")
    })
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse response = inventoryService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/products/{sku}")
    @Operation(summary = "Get product by SKU", description = "Fetches product metadata from DB with real-time available stock from Redis")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product found"),
        @ApiResponse(responseCode = "400", description = "Product SKU not found")
    })
    public ResponseEntity<ProductResponse> getProduct(@PathVariable String sku) {
        return ResponseEntity.ok(inventoryService.getProductBySku(sku));
    }

    @PostMapping("/reserve")
    @Operation(summary = "Reserve stock (Flash-Sale)", description = "Executes an atomic single-threaded Redis Lua script to decrement stock with zero race conditions")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock reserved successfully"),
        @ApiResponse(responseCode = "409", description = "Stock insufficient or SKU not found in Redis cache")
    })
    public ResponseEntity<StockReservationResponse> reserveStock(@Valid @RequestBody StockReservationRequest request) {
        StockReservationResponse response = inventoryService.reserveStock(request);
        if (!response.success()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/release")
    @Operation(summary = "Release reserved stock (Saga Compensation)", description = "Rolls back stock decrement when downstream Order creation or Payment fails")
    @ApiResponse(responseCode = "200", description = "Stock rolled back successfully")
    public ResponseEntity<StockReservationResponse> releaseStock(
            @RequestParam String sku,
            @RequestParam int quantity,
            @RequestParam(required = false, defaultValue = "COMPENSATION") String orderRef) {
        return ResponseEntity.ok(inventoryService.releaseStock(sku, quantity, orderRef));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }
}
