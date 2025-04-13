package com.example.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import net.devh.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration;

@SpringBootApplication
@Import({GrpcClientAutoConfiguration.class})
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}