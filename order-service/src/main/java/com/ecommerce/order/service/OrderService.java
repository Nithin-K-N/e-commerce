package com.ecommerce.order.service;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.map.OrderMapper;
import com.ecommerce.order.event.OrderPlacedEvent;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.producer.OrderEventProducer;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer eventProducer;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        BigDecimal totalAmount = BigDecimal.ZERO;
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(request.userId())
                .status(OrderStatus.PENDING)
                .build();

        for (var itemReq : request.items()) {
            BigDecimal itemTotal = itemReq.price().multiply(BigDecimal.valueOf(itemReq.quantity()));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem item = OrderItem.builder()
                    .sku(itemReq.sku().trim().toUpperCase())
                    .productName(itemReq.productName())
                    .price(itemReq.price())
                    .quantity(itemReq.quantity())
                    .build();

            order.addItem(item);
        }

        order.setTotalAmount(totalAmount);
        Order saved = orderRepository.save(order);

        log.info("Order created successfully: id={}, orderNumber={}, totalAmount={}", 
                saved.getId(), saved.getOrderNumber(), saved.getTotalAmount());

        // Publish OrderPlacedEvent to Kafka topic with orderNumber as key
        List<OrderPlacedEvent.OrderItemDto> eventItems = saved.getItems().stream()
                .map(i -> new OrderPlacedEvent.OrderItemDto(i.getSku(), i.getProductName(), i.getPrice(), i.getQuantity()))
                .toList();

        OrderPlacedEvent event = new OrderPlacedEvent(
                UUID.randomUUID().toString(),
                saved.getOrderNumber(),
                saved.getUserId(),
                saved.getTotalAmount(),
                eventItems,
                LocalDateTime.now()
        );

        eventProducer.publishOrderPlaced(event);

        return orderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with orderNumber: " + orderNumber));
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }
}
