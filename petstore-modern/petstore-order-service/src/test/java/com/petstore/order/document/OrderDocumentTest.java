package com.petstore.order.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OrderDocument}, {@link LineItemDocument},
 * {@link AddressDocument}, and {@link PaymentDocument}.
 */
class OrderDocumentTest {

  @Test
  @DisplayName("Should assemble complete order document aggregate with line items")
  void shouldAssembleCompleteOrderAggregate() {
    AddressDocument billing = new AddressDocument(
        "Duke Java",
        "123 Sun Way",
        null,
        "Santa Clara",
        "CA",
        "95054",
        "USA",
        "408-555-1212",
        "duke@sun.com"
    );

    AddressDocument shipping = new AddressDocument(
        "Duke Java",
        "123 Sun Way",
        null,
        "Santa Clara",
        "CA",
        "95054",
        "USA",
        "408-555-1212",
        "duke@sun.com"
    );

    PaymentDocument payment = new PaymentDocument(
        "Visa",
        "XXXX-XXXX-XXXX-1111",
        "12/2028"
    );

    LineItemDocument item1 = new LineItemDocument(
        0, "EST-1", "FI-SW-01", "FISH", 2, new BigDecimal("16.50"), new BigDecimal("33.00")
    );
    LineItemDocument item2 = new LineItemDocument(
        1, "EST-2", "FI-SW-01", "FISH", 1, new BigDecimal("5.50"), new BigDecimal("5.50")
    );

    Instant now = Instant.now();
    OrderDocument order = new OrderDocument(
        "100115",
        "j2ee",
        now,
        OrderStatus.PENDING,
        new BigDecimal("38.50"),
        "en_US",
        billing,
        shipping,
        payment,
        List.of(item1, item2)
    );

    assertThat(order.getId()).isEqualTo("100115");
    assertThat(order.getUserId()).isEqualTo("j2ee");
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(order.getTotalPrice()).isEqualTo(new BigDecimal("38.50"));
    assertThat(order.getLineItems()).hasSize(2);
    assertThat(order.getBilling().getCity()).isEqualTo("Santa Clara");
    assertThat(order.getPayment().getCardType()).isEqualTo("Visa");

    // Test state transition to APPROVED
    order.setStatus(OrderStatus.APPROVED);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.APPROVED);
  }

  @Test
  @DisplayName("Should verify equals and hashCode contracts on OrderDocument")
  void shouldVerifyOrderEqualsAndHashCode() {
    OrderDocument order1 = new OrderDocument("100115", "j2ee", null, null, null, null, null, null, null, null);
    OrderDocument order2 = new OrderDocument("100115", "other", null, null, null, null, null, null, null, null);
    OrderDocument order3 = new OrderDocument("100116", "j2ee", null, null, null, null, null, null, null, null);

    assertThat(order1).isEqualTo(order2);
    assertThat(order1.hashCode()).isEqualTo(order2.hashCode());
    assertThat(order1).isNotEqualTo(order3);
  }

  @Test
  @DisplayName("Should verify LineItemDocument equality based on lineNumber and itemId")
  void shouldVerifyLineItemEquality() {
    LineItemDocument item1 = new LineItemDocument(0, "EST-1", "P1", "C1", 1, BigDecimal.TEN, BigDecimal.TEN);
    LineItemDocument item2 = new LineItemDocument(0, "EST-1", "P2", "C2", 2, BigDecimal.ONE, BigDecimal.TWO);
    LineItemDocument item3 = new LineItemDocument(1, "EST-1", "P1", "C1", 1, BigDecimal.TEN, BigDecimal.TEN);

    assertThat(item1).isEqualTo(item2);
    assertThat(item1).isNotEqualTo(item3);
  }
}
