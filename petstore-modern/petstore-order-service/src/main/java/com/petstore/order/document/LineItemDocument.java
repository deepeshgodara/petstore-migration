package com.petstore.order.document;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Embedded document representing a purchase order line item.
 */
public class LineItemDocument implements Serializable {

  private static final long serialVersionUID = 1L;

  private int lineNumber;
  private String itemId;
  private String productId;
  private String categoryId;
  private int quantity;
  private BigDecimal unitPrice;
  private BigDecimal totalCost;

  public LineItemDocument() {}

  public LineItemDocument(
      int lineNumber,
      String itemId,
      String productId,
      String categoryId,
      int quantity,
      BigDecimal unitPrice,
      BigDecimal totalCost) {
    this.lineNumber = lineNumber;
    this.itemId = itemId;
    this.productId = productId;
    this.categoryId = categoryId;
    this.quantity = quantity;
    this.unitPrice = unitPrice;
    this.totalCost = totalCost;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public void setLineNumber(int lineNumber) {
    this.lineNumber = lineNumber;
  }

  public String getItemId() {
    return itemId;
  }

  public void setItemId(String itemId) {
    this.itemId = itemId;
  }

  public String getProductId() {
    return productId;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public String getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(String categoryId) {
    this.categoryId = categoryId;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(BigDecimal unitPrice) {
    this.unitPrice = unitPrice;
  }

  public BigDecimal getTotalCost() {
    return totalCost;
  }

  public void setTotalCost(BigDecimal totalCost) {
    this.totalCost = totalCost;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LineItemDocument that)) {
      return false;
    }
    return lineNumber == that.lineNumber && Objects.equals(itemId, that.itemId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lineNumber, itemId);
  }

  @Override
  public String toString() {
    return "LineItemDocument{"
        + "lineNumber=" + lineNumber
        + ", itemId='" + itemId + '\''
        + ", quantity=" + quantity
        + ", totalCost=" + totalCost
        + '}';
  }
}
