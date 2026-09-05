package com.petstore.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.petstore.order.document.AddressDocument;
import com.petstore.order.document.LineItemDocument;
import com.petstore.order.document.OrderDocument;
import com.petstore.order.document.OrderStatus;
import com.petstore.order.document.PaymentDocument;
import com.petstore.order.dto.CreateOrderRequest;
import com.petstore.order.dto.OrderSummaryResponse;
import com.petstore.order.dto.UpdateOrderStatusRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Automated integration tests using Testcontainers (MongoDB 7.0 + Kafka 7.6.1).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

  @Container
  @ServiceConnection
  static MongoDBContainer mongo = new MongoDBContainer(
      DockerImageName.parse("mongo:7.0"));

  @Container
  @ServiceConnection
  static KafkaContainer kafka = new KafkaContainer(
      DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  @DisplayName("Should execute order lifecycle with live Mongo and Kafka containers")
  void shouldExecuteFullOrderLifecycle() {
    // 1. Place customer order
    AddressDocument addr = new AddressDocument(
        "Integration Customer", "123 Container Way", null,
        "Austin", "TX", "78701", "USA", "512-555-0100", "shopper@test.com");
    PaymentDocument payment = new PaymentDocument(
        "Visa", "XXXX-XXXX-XXXX-9999", "11/29");
    LineItemDocument item = new LineItemDocument(
        1, "EST-1", "FI-SW-01", "FISH", 2,
        BigDecimal.valueOf(16.50), BigDecimal.valueOf(33.00));

    CreateOrderRequest createRequest = new CreateOrderRequest(
        "integration-user", "en_US", addr, addr, payment, List.of(item));

    ResponseEntity<OrderDocument> createResponse = restTemplate.postForEntity(
        "/api/v1/orders", createRequest, OrderDocument.class);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    OrderDocument createdOrder = createResponse.getBody();
    assertThat(createdOrder).isNotNull();
    assertThat(createdOrder.getId()).isNotBlank();
    assertThat(createdOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(createdOrder.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(33.00));

    String orderId = createdOrder.getId();

    // 2. Query order by ID
    ResponseEntity<OrderDocument> getResponse = restTemplate.getForEntity(
        "/api/v1/orders/" + orderId, OrderDocument.class);
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getResponse.getBody()).isNotNull();
    assertThat(getResponse.getBody().getUserId()).isEqualTo("integration-user");

    // 3. Update order status to APPROVED
    UpdateOrderStatusRequest updateRequest = new UpdateOrderStatusRequest(OrderStatus.APPROVED);
    ResponseEntity<OrderDocument> updateResponse = restTemplate.exchange(
        "/api/v1/orders/" + orderId + "/status",
        HttpMethod.PUT,
        new HttpEntity<>(updateRequest),
        OrderDocument.class);

    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updateResponse.getBody()).isNotNull();
    assertThat(updateResponse.getBody().getStatus()).isEqualTo(OrderStatus.APPROVED);

    // 4. Query admin summary and verify aggregation metrics in live Mongo container
    ResponseEntity<OrderSummaryResponse> summaryResponse = restTemplate.getForEntity(
        "/api/v1/orders/admin/summary", OrderSummaryResponse.class);
    assertThat(summaryResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    OrderSummaryResponse summary = summaryResponse.getBody();
    assertThat(summary).isNotNull();
    assertThat(summary.totalOrders()).isGreaterThanOrEqualTo(1);
    assertThat(summary.statusBreakdown()).containsKey("APPROVED");
  }
}
