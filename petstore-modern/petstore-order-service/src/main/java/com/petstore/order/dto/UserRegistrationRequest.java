package com.petstore.order.dto;

import java.io.Serializable;

/**
 * Payload for user registration REST requests.
 */
public record UserRegistrationRequest(
    String username,
    String password,
    String email,
    String givenName,
    String familyName,
    String telephone,
    String street1,
    String street2,
    String city,
    String state,
    String zipCode,
    String country,
    String preferredLanguage,
    String favoriteCategory
) implements Serializable {

  public UserRegistrationRequest {
    username = username != null ? username.trim() : "";
    password = password != null ? password.trim() : "";
    email = email != null ? email.trim() : "";
    givenName = givenName != null ? givenName.trim() : "";
    familyName = familyName != null ? familyName.trim() : "";
    telephone = telephone != null ? telephone.trim() : "";
    street1 = street1 != null ? street1.trim() : "";
    street2 = street2 != null ? street2.trim() : "";
    city = city != null ? city.trim() : "";
    state = state != null ? state.trim() : "";
    zipCode = zipCode != null ? zipCode.trim() : "";
    country = country != null ? country.trim() : "USA";
    preferredLanguage = preferredLanguage != null ? preferredLanguage.trim() : "en_US";
    favoriteCategory = favoriteCategory != null ? favoriteCategory.trim() : "FISH";
  }
}
