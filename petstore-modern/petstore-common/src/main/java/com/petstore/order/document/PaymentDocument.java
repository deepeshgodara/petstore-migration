package com.petstore.order.document;

import java.io.Serializable;
import java.util.Objects;

/**
 * Embedded document representing payment and masked credit card details.
 */
public class PaymentDocument implements Serializable {

  private static final long serialVersionUID = 1L;

  private String cardType;
  private String cardNumberMasked;
  private String expiryDate;

  public PaymentDocument() {}

  public PaymentDocument(String cardType, String cardNumberMasked, String expiryDate) {
    this.cardType = cardType;
    this.cardNumberMasked = cardNumberMasked;
    this.expiryDate = expiryDate;
  }

  public String getCardType() {
    return cardType;
  }

  public void setCardType(String cardType) {
    this.cardType = cardType;
  }

  public String getCardNumberMasked() {
    return cardNumberMasked;
  }

  public void setCardNumberMasked(String cardNumberMasked) {
    this.cardNumberMasked = cardNumberMasked;
  }

  public String getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(String expiryDate) {
    this.expiryDate = expiryDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PaymentDocument that)) {
      return false;
    }
    return Objects.equals(cardType, that.cardType)
        && Objects.equals(cardNumberMasked, that.cardNumberMasked);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cardType, cardNumberMasked);
  }

  @Override
  public String toString() {
    return "PaymentDocument{"
        + "cardType='" + cardType + '\''
        + ", cardNumberMasked='" + cardNumberMasked + '\''
        + '}';
  }
}
