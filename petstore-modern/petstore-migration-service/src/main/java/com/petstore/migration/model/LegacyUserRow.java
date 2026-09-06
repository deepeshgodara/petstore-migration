package com.petstore.migration.model;

import java.io.Serializable;

/**
 * Record representing a complete joined user row from legacy USER, CUSTOMER,
 * PROFILE, ACCOUNT, CONTACTINFO, ADDRESS, and CREDITCARD tables.
 */
public record LegacyUserRow(
    String username,
    String password,
    String status,
    String email,
    String givenName,
    String familyName,
    String telephone,
    String favoriteCategory,
    String preferredLanguage,
    boolean bannerPreference,
    boolean myListPreference,
    String street1,
    String street2,
    String city,
    String state,
    String zipCode,
    String country,
    String cardNumber,
    String cardType,
    String expiryDate
) implements Serializable {

  public LegacyUserRow {
    username = username != null ? username.trim() : "";
    password = password != null ? password.trim() : "";
    status = status != null ? status.trim() : "active";
    email = email != null ? email.trim() : "";
    givenName = givenName != null ? givenName.trim() : "";
    familyName = familyName != null ? familyName.trim() : "";
    telephone = telephone != null ? telephone.trim() : "";
    favoriteCategory = favoriteCategory != null ? favoriteCategory.trim() : "FISH";
    preferredLanguage = preferredLanguage != null ? preferredLanguage.trim() : "en_US";
    street1 = street1 != null ? street1.trim() : "";
    street2 = street2 != null ? street2.trim() : "";
    city = city != null ? city.trim() : "";
    state = state != null ? state.trim() : "";
    zipCode = zipCode != null ? zipCode.trim() : "";
    country = country != null ? country.trim() : "";
    cardNumber = cardNumber != null ? cardNumber.trim() : "";
    cardType = cardType != null ? cardType.trim() : "";
    expiryDate = expiryDate != null ? expiryDate.trim() : "";
  }
}
