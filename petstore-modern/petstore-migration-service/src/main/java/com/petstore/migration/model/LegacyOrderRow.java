package com.petstore.migration.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Record representing a joined purchase order row from legacy tables
 * (PURCHASEORDER, MANAGER, CONTACTINFO, ADDRESS, and CREDITCARD).
 */
public record LegacyOrderRow(
    String poId,
    String userId,
    long poDateMillis,
    BigDecimal poValue,
    String locale,
    String status,
    String givenName,
    String familyName,
    String email,
    String telephone,
    String streetName1,
    String streetName2,
    String city,
    String state,
    String zipCode,
    String country,
    String cardType,
    String cardNumber,
    String expiryDate
) implements Serializable {

  public LegacyOrderRow {
    poId = poId != null ? poId.trim() : "";
    userId = userId != null ? userId.trim() : "";
    poValue = poValue != null ? poValue : BigDecimal.ZERO;
    locale = locale != null ? locale.trim() : "en_US";
    status = status != null ? status.trim().toUpperCase() : "PENDING";
  }
}
