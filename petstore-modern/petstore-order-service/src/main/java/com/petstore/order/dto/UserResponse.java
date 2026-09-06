package com.petstore.order.dto;

import com.petstore.order.document.AddressDocument;
import com.petstore.user.document.UserDocument;
import com.petstore.user.document.UserProfileDocument;
import java.io.Serializable;

/**
 * Public DTO representing an authenticated user session in Pet Store.
 */
public record UserResponse(
    String username,
    String name,
    String email,
    String givenName,
    String familyName,
    String telephone,
    String role,
    String status,
    UserProfileDocument profile,
    AddressDocument address,
    String token
) implements Serializable {

  public static UserResponse fromDocument(UserDocument doc) {
    if (doc == null) {
      return null;
    }
    String fullName = ((doc.getGivenName() != null ? doc.getGivenName() : "") + " "
        + (doc.getFamilyName() != null ? doc.getFamilyName() : "")).trim();
    if (fullName.isEmpty()) {
      fullName = doc.getUsername();
    }

    return new UserResponse(
        doc.getUsername(),
        fullName,
        doc.getEmail(),
        doc.getGivenName(),
        doc.getFamilyName(),
        doc.getTelephone(),
        doc.getRole(),
        doc.getStatus(),
        doc.getProfile(),
        doc.getAddress(),
        "jwt_token_" + doc.getUsername() + "_" + System.currentTimeMillis()
    );
  }
}
