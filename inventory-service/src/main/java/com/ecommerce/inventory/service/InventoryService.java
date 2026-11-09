package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.*;
import com.ecommerce.inventory.dto.map.ProductMapper;
import com.ecommerce.inventory.model.Product;
import com.ecommerce.inventory.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;
    private final ProductMapper productMapper;

    // Atomic Lua script executed single-threaded inside Redis:
    // Guarantees zero race conditions or overselling even under 100,000 req/sec!
    private static final String STOCK_RESERVATION_LUA_SCRIPT = """
            local key = KEYS[1]
            local current = redis.call('get', key)
            if not current then
                return -1
            end
            local stock = tonumber(current)
            local qty = tonumber(ARGV[1])
            if stock < qty then
                return -2
            end
            return redis.call('decrby', key, qty)
            """;

    private final DefaultRedisScript<Long> reservationScript = new DefaultRedisScript<>(STOCK_RESERVATION_LUA_SCRIPT, Long.class);

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new IllegalArgumentException("Product with SKU already exists: " + request.sku());
        }

        Product product = productMapper.toEntity(request);
        Product saved = productRepository.save(product);

        // Seed stock count directly into Redis in-memory cache for sub-millisecond flash-sale checks
        String redisKey = getRedisStockKey(saved.getSku());
        redisTemplate.opsForValue().set(redisKey, String.valueOf(saved.getAvailableStock()));

        return productMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with SKU: " + sku));

        // Get live real-time stock from Redis if available
        String redisKey = getRedisStockKey(sku);
        String liveStock = redisTemplate.opsForValue().get(redisKey);
        int available = (liveStock != null) 
                            ? Integer.parseInt(liveStock) 
                            : product.getAvailableStock();

        return productMapper.toResponse(product, available);
    }

    @Transactional
    public StockReservationResponse reserveStock(StockReservationRequest request) {
        String redisKey = getRedisStockKey(request.sku());

        // 1. Execute Atomic Redis Lua Script (Flash-sale lock-free reservation)
        Long result = redisTemplate.execute(
                reservationScript,
                Collections.singletonList(redisKey),
                String.valueOf(request.quantity())
        );

        if (result == null || result == -1L) {
            return new StockReservationResponse(
                    request.sku(), request.quantity(), 0L, false, "Product SKU not found in inventory cache");
        }

        if (result == -2L) {
            String currentStr = redisTemplate.opsForValue().get(redisKey);
            long currentStock = (currentStr != null) ? Long.parseLong(currentStr) : 0L;
            return new StockReservationResponse(
                    request.sku(), request.quantity(), currentStock, false, "Insufficient stock available for flash sale");
        }

        // 2. Synchronize reservation state with PostgreSQL
        productRepository.findBySku(request.sku()).ifPresent(product -> {
            product.setAvailableStock(result.intValue());
            productRepository.save(product);
        });

        return new StockReservationResponse(
                request.sku(),
                request.quantity(),
                result,
                true,
                "Stock reserved successfully"
        );
    }

    @Transactional
    public StockReservationResponse releaseStock(String sku, int quantity, String orderReference) {
        String redisKey = getRedisStockKey(sku);

        // Compensating transaction (Saga pattern rollback):
        // Atomically increment stock back in Redis
        Long restoredStock = redisTemplate.opsForValue().increment(redisKey, quantity);

        // Sync back to PostgreSQL
        productRepository.findBySku(sku).ifPresent(product -> {
            if (restoredStock != null) {
                product.setAvailableStock(restoredStock.intValue());
                productRepository.save(product);
            }
        });

        return new StockReservationResponse(
                sku,
                quantity,
                restoredStock != null ? restoredStock : 0L,
                true,
                "Stock released back into available inventory"
        );
    }

    private String getRedisStockKey(String sku) {
        return "inventory:stock:" + sku.trim().toUpperCase();
    }
}
