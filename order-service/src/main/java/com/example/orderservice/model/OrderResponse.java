package com.example.orderservice.model;

import com.example.orderservice.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private Long productId;
    private Integer quantity;
    private Double totalPrice;
    private Order.OrderStatus status;
}