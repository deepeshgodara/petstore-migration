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
  private String productName;
  private String itemAttribute;
  private String image;

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

  public LineItemDocument(
      int lineNumber,
      String itemId,
      String productId,
      String categoryId,
      int quantity,
      BigDecimal unitPrice,
      BigDecimal totalCost,
      String productName,
      String itemAttribute,
      String image) {
    this(lineNumber, itemId, productId, categoryId, quantity, unitPrice, totalCost);
    this.productName = productName;
    this.itemAttribute = itemAttribute;
    this.image = image;
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

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public String getItemAttribute() {
    return itemAttribute;
  }

  public void setItemAttribute(String itemAttribute) {
    this.itemAttribute = itemAttribute;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
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
