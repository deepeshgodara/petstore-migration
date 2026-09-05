package com.petstore.order.document;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB document representing a complete Pet Store purchase order aggregate.
 * Eliminates 5-table relational joins across PURCHASEORDER, LINEITEM, CONTACTINFO,
 * ADDRESS, and CREDITCARD. Maps to the {@code petstore_orders} collection.
 */
@Document(collection = "petstore_orders")
@CompoundIndexes({
    @CompoundIndex(name = "userId_orderDate_idx", def = "{'userId': 1, 'orderDate': -1}"),
    @CompoundIndex(name = "status_orderDate_idx", def = "{'status': 1, 'orderDate': -1}")
})
public class OrderDocument implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  private String id;

  @Indexed
  private String userId;

  private Instant orderDate;

  @Indexed
  private OrderStatus status = OrderStatus.PENDING;

  private BigDecimal totalPrice = BigDecimal.ZERO;
  private String locale = "en_US";

  private AddressDocument billing;
  private AddressDocument shipping;
  private PaymentDocument payment;
  private List<LineItemDocument> lineItems = new ArrayList<>();

  private Instant createdAt = Instant.now();
  private Instant updatedAt = Instant.now();
  private boolean migratedFromLegacy = false;

  public OrderDocument() {}

  public OrderDocument(
      String id,
      String userId,
      Instant orderDate,
      OrderStatus status,
      BigDecimal totalPrice,
      String locale,
      AddressDocument billing,
      AddressDocument shipping,
      PaymentDocument payment,
      List<LineItemDocument> lineItems) {
    this.id = id;
    this.userId = userId;
    this.orderDate = orderDate != null ? orderDate : Instant.now();
    this.status = status != null ? status : OrderStatus.PENDING;
    this.totalPrice = totalPrice != null ? totalPrice : BigDecimal.ZERO;
    this.locale = locale != null ? locale : "en_US";
    this.billing = billing;
    this.shipping = shipping;
    this.payment = payment;
    if (lineItems != null) {
      this.lineItems = new ArrayList<>(lineItems);
    }
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public Instant getOrderDate() {
    return orderDate;
  }

  public void setOrderDate(Instant orderDate) {
    this.orderDate = orderDate;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public void setStatus(OrderStatus status) {
    this.status = status;
    this.updatedAt = Instant.now();
  }

  public BigDecimal getTotalPrice() {
    return totalPrice;
  }

  public void setTotalPrice(BigDecimal totalPrice) {
    this.totalPrice = totalPrice;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public AddressDocument getBilling() {
    return billing;
  }

  public void setBilling(AddressDocument billing) {
    this.billing = billing;
  }

  public AddressDocument getShipping() {
    return shipping;
  }

  public void setShipping(AddressDocument shipping) {
    this.shipping = shipping;
  }

  public PaymentDocument getPayment() {
    return payment;
  }

  public void setPayment(PaymentDocument payment) {
    this.payment = payment;
  }

  public List<LineItemDocument> getLineItems() {
    return Collections.unmodifiableList(lineItems);
  }

  public void setLineItems(List<LineItemDocument> lineItems) {
    this.lineItems = lineItems != null ? new ArrayList<>(lineItems) : new ArrayList<>();
  }

  public void addLineItem(LineItemDocument lineItem) {
    if (lineItem != null) {
      this.lineItems.add(lineItem);
    }
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public boolean isMigratedFromLegacy() {
    return migratedFromLegacy;
  }

  public void setMigratedFromLegacy(boolean migratedFromLegacy) {
    this.migratedFromLegacy = migratedFromLegacy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof OrderDocument that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "OrderDocument{"
        + "id='" + id + '\''
        + ", userId='" + userId + '\''
        + ", status=" + status
        + ", totalPrice=" + totalPrice
        + ", lineItemsCount=" + lineItems.size()
        + '}';
  }
}
