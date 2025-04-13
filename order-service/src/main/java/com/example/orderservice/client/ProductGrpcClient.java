package com.example.orderservice.client;

import com.example.grpc.ProductRequest;
import com.example.grpc.ProductResponse;
import com.example.grpc.ProductServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ProductGrpcClient {

    private static final Logger logger = LoggerFactory.getLogger(ProductGrpcClient.class);

    @GrpcClient("product-service")
    private ProductServiceGrpc.ProductServiceBlockingStub productServiceBlockingStub;

    @PostConstruct
    public void init() {
        logger.info("ProductGrpcClient initialized. Is stub null? {}", (productServiceBlockingStub == null));
    }

    public ProductResponse getProductById(Long productId) {
        if (productServiceBlockingStub == null) {
            throw new RuntimeException("gRPC client not initialized. Check if product-service is running and properly configured.");
        }

        try {
            ProductRequest request = ProductRequest.newBuilder()
                    .setProductId(productId)
                    .build();

            return productServiceBlockingStub.getProductById(request);
        } catch (StatusRuntimeException e) {
            Status status = e.getStatus();
            if (status.getCode() == Status.Code.NOT_FOUND) {
                throw new RuntimeException("Product not found with ID: " + productId);
            }
            throw new RuntimeException("Error calling product service: " + e.getMessage());
        }
    }
}