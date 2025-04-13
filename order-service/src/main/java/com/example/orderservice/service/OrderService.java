package com.example.orderservice.service;

import com.example.grpc.ProductResponse;
import com.example.orderservice.client.ProductGrpcClient;
import com.example.orderservice.entity.Order;
import com.example.orderservice.model.OrderEvent;
import com.example.orderservice.model.OrderRequest;
import com.example.orderservice.model.OrderResponse;
import com.example.orderservice.model.ProductEvent;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductGrpcClient productGrpcClient;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest) {
        // Get product details using gRPC
        ProductResponse productResponse = productGrpcClient.getProductById(orderRequest.getProductId());
        
        // Calculate total price
        double totalPrice = productResponse.getPrice() * orderRequest.getQuantity();
        
        // Create order with CREATED status
        Order order = new Order();
        order.setProductId(orderRequest.getProductId());
        order.setQuantity(orderRequest.getQuantity());
        order.setTotalPrice(totalPrice);
        order.setStatus(Order.OrderStatus.CREATED);
        
        // Save order to DB
        Order savedOrder = orderRepository.save(order);
        
        // Send order event to product service
        OrderEvent orderEvent = new OrderEvent(savedOrder.getId(), orderRequest.getProductId(), orderRequest.getQuantity());
        kafkaTemplate.send("order-events", orderEvent);
        
        // Return response
        return mapToOrderResponse(savedOrder);
    }
    
    @KafkaListener(topics = "product-events", groupId = "order-service-group", 
                  containerFactory = "productEventKafkaListenerContainerFactory")
    @Transactional
    public void processProductEvent(ProductEvent productEvent) {
        Order order = orderRepository.findById(productEvent.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + productEvent.getOrderId()));
        
        if (productEvent.getStatus() == ProductEvent.ProductStatus.AVAILABLE) {
            order.setStatus(Order.OrderStatus.PROCESSING);
        } else {
            order.setStatus(Order.OrderStatus.FAILED);
        }
        
        orderRepository.save(order);
    }
    
    public OrderResponse getOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + id));
        
        return mapToOrderResponse(order);
    }
    
    private OrderResponse mapToOrderResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getProductId(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getStatus()
        );
    }
}