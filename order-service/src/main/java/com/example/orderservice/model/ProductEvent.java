package com.example.orderservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductEvent {
    private Long orderId;
    private Long productId;
    private ProductStatus status;
    
    public enum ProductStatus {
        AVAILABLE,
        OUT_OF_STOCK
    }
}