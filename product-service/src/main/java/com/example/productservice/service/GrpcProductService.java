package com.example.productservice.service;

import com.example.grpc.ProductRequest;
import com.example.grpc.ProductResponse;
import com.example.grpc.ProductServiceGrpc;
import com.example.productservice.entity.Product;
import com.example.productservice.repository.ProductRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@GrpcService
@RequiredArgsConstructor
public class GrpcProductService extends ProductServiceGrpc.ProductServiceImplBase {

    private final ProductRepository productRepository;

    @Override
    public void getProductById(ProductRequest request, StreamObserver<ProductResponse> responseObserver) {
        Long productId = request.getProductId();
        
        Optional<Product> productOptional = productRepository.findById(productId);
        
        if (productOptional.isPresent()) {
            Product product = productOptional.get();
            
            ProductResponse response = ProductResponse.newBuilder()
                    .setId(product.getId())
                    .setName(product.getName())
                    .setPrice(product.getPrice())
                    .setQuantity(product.getQuantity())
                    .build();
                    
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Product not found with ID: " + productId)
                    .asRuntimeException());
        }
    }
}