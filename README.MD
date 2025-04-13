# Order and Product Microservices with gRPC and Kafka

This project demonstrates a microservices architecture featuring an **Order Service** and a **Product Service**. The services communicate synchronously using **gRPC** for specific data retrieval (product details) and asynchronously using **Apache Kafka** for event-driven updates, showcasing a pattern for handling distributed transactions in order processing.

## Overview

* **Order Service:** Exposes a REST API for creating and retrieving orders. It communicates with the Product Service via gRPC to fetch product details and uses Kafka to publish order creation events and consume product availability events.
* **Product Service (Implied):** Manages product information and inventory. It exposes a gRPC endpoint for product details and interacts with Kafka to consume order creation events and publish product availability/status events.
* **Communication:**
    * **Synchronous:** gRPC is used by the Order Service to request product information directly from the Product Service when an order is being created.
    * **Asynchronous:** Kafka is used for event-driven communication:
        * Order Service publishes `OrderEvent` when an order is created.
        * Product Service consumes `OrderEvent`, checks stock, and publishes `ProductEvent`.
        * Order Service consumes `ProductEvent` to update the order status (e.g., to `PROCESSING` or `FAILED`).
* **Database:** The Order Service uses an H2 in-memory database.

## Features (Order Service)

* Create new orders via REST endpoint (`POST /api/orders`).
* Retrieve existing order details via REST endpoint (`GET /api/orders/{id}`).
* Fetch product details from Product Service using gRPC.
* Publish `OrderEvent` to Kafka upon order creation.
* Consume `ProductEvent` from Kafka to update order status based on product availability.
* Maintain order state (CREATED, PROCESSING, FAILED).

## Technologies Used

* Java
* Spring Boot
* Spring Web
* Spring Data JPA
* Spring Kafka
* gRPC
* Apache Kafka
* H2 Database
* Maven
* Lombok

## Project Structure

```
.
├── order-service/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/orderservice/
│   │   │   │   ├── client/       # gRPC Client
│   │   │   │   ├── config/       # Kafka Configuration
│   │   │   │   ├── controller/   # REST Controller
│   │   │   │   ├── entity/       # JPA Entity (Order)
│   │   │   │   ├── model/       # DTOs and Event Models
│   │   │   │   ├── repository/   # Spring Data Repository
│   │   │   │   ├── service/       # Business Logic
│   │   │   │   └── OrderServiceApplication.java
│   │   │   ├── proto/         # Protocol Buffer definitions (product.proto)
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   └── pom.xml
└── product-service/ # (Structure similar to order-service, focused on products)
    ├── src/
    │   ├── main/
    │   │   ├── java/com/example/productservice/
    │   │   │   ├── config/       # Kafka Configuration
    │   │   │   ├── entity/       # JPA Entity (Product)
    │   │   │   ├── model/       # Event Models
    │   │   │   ├── repository/   # Spring Data Repository
    │   │   │   ├── service/       # Business Logic & gRPC Service Impl
    │   │   │   └── ProductServiceApplication.java
    │   │   ├── proto/         # Protocol Buffer definitions (product.proto)
    │   │   └── resources/
    │   │       ├── application.properties
    │   │       └── data.sql     # Initial product data
    │   └── test/
    └── pom.xml
```

## Prerequisites

* JDK (e.g., 11 or 17)
* Maven
* Docker & Docker Compose (Recommended for running Kafka)
* An active Kafka instance running (Default: `localhost:9092`)

## Getting Started

1.  **Clone the repository:**
    ```bash
    git clone <your-repository-url>
    cd <your-repository-directory>
    ```

2.  **Start Kafka:**
    Make sure you have Apache Kafka running on `localhost:9092`. You can use Docker Compose for a quick setup. Create a `docker-compose.yml` file if you don't have one:
    ```yaml
    version: '3'
    services:
      zookeeper:
        image: confluentinc/cp-zookeeper:latest
        environment:
          ZOOKEEPER_CLIENT_PORT: 2181
          ZOOKEEPER_TICK_TIME: 2000
        ports:
          - "2181:2181"
      kafka:
        image: confluentinc/cp-kafka:latest
        depends_on:
          - zookeeper
        ports:
          - "9092:9092"
        environment:
          KAFKA_BROKER_ID: 1
          KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
          KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
          KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
          KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
          KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
          KAFKA_CREATE_TOPICS: "order-events:1:1,product-events:1:1" # Auto-create topics
    ```
    Run Kafka using Docker Compose:
    ```bash
    docker-compose up -d
    ```

## Running the Application

1.  **Build and Run the Product Service:**
    ```bash
    cd product-service
    mvn clean install
    mvn spring-boot:run
    ```
    *(Note: Ensure Product Service's `application.properties` sets its server port, gRPC port, and Kafka details correctly)*

2.  **Build and Run the Order Service:**
    ```bash
    cd ../order-service # Navigate back and into order-service
    mvn clean install
    mvn spring-boot:run
    ```

## Usage / API Endpoints (Order Service)

The Order Service runs on `http://localhost:8080` by default.

1.  **Create an Order:**
    Send a POST request to `/api/orders`.
    ```bash
    curl -X POST -H "Content-Type: application/json" \
         -d '{"productId": 1, "quantity": 3}' \
         http://localhost:8080/api/orders
    ```
    * This will:
        * Call the Product Service via gRPC to get details for `productId: 1`.
        * Create an order locally with `CREATED` status.
        * Send an `OrderEvent` to the `order-events` Kafka topic.
    * Response will show the initially created order.

2.  **Check Order Status:**
    Send a GET request to `/api/orders/{id}`, replacing `{id}` with the ID returned from the creation step (e.g., 1).
    ```bash
    curl http://localhost:8080/api/orders/1
    ```
    * Initially, the status might be `CREATED`.
    * After the Product Service processes the `OrderEvent` and sends back a `ProductEvent` (e.g., `AVAILABLE`), the Order Service will consume this event and update the status. Refreshing this request should eventually show `PROCESSING` or `FAILED`.

## Configuration (Order Service - `application.properties`)

* `server.port=8080`: Port for the Order Service REST API.
* `spring.datasource.*`: H2 database configuration.
* `spring.h2.console.enabled=true`: Enables H2 web console (usually at `/h2-console`).
* `grpc.client.product-service.address=static://localhost:9090`: Address of the Product Service gRPC server.
* `grpc.client.product-service.negotiation-type=plaintext`: gRPC communication security (none).
* `spring.kafka.bootstrap-servers=localhost:9092`: Kafka broker address.
* `spring.jpa.hibernate.ddl-auto=update`: Database schema management.
* `spring.jpa.show-sql=true`: Logs SQL statements.

