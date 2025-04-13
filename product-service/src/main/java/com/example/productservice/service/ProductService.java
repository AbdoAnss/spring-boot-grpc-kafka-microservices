package com.example.productservice.service;

import com.example.productservice.entity.Product;
import com.example.productservice.model.OrderEvent;
import com.example.productservice.model.ProductEvent;
import com.example.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, ProductEvent> kafkaTemplate;

    @KafkaListener(topics = "order-events", groupId = "product-service-group", 
                  containerFactory = "orderEventKafkaListenerContainerFactory")
    @Transactional
    public void processOrderEvent(OrderEvent orderEvent) {
        Long productId = orderEvent.getProductId();
        Integer requestedQuantity = orderEvent.getQuantity();
        Long orderId = orderEvent.getOrderId();
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));
        
        ProductEvent.ProductStatus status;
        
        if (product.getQuantity() >= requestedQuantity) {
            // Update product quantity
            product.setQuantity(product.getQuantity() - requestedQuantity);
            productRepository.save(product);
            status = ProductEvent.ProductStatus.AVAILABLE;
        } else {
            status = ProductEvent.ProductStatus.OUT_OF_STOCK;
        }
        
        // Send product event back to order service
        ProductEvent productEvent = new ProductEvent(orderId, productId, status);
        kafkaTemplate.send("product-events", productEvent);
    }
}