package com.petstore.migration.processor;

import com.petstore.migration.model.LegacyUserRow;
import com.petstore.order.document.AddressDocument;
import com.petstore.order.document.PaymentDocument;
import com.petstore.user.document.UserDocument;
import com.petstore.user.document.UserProfileDocument;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Transforms relational 3NF rows from the legacy customer subsystem
 * (USER, CUSTOMER, ACCOUNT, PROFILE, CONTACTINFO, ADDRESS, CREDITCARD)
 * into MongoDB UserDocument aggregates.
 */
@Component
public class UserTransformationProcessor {

  public List<UserDocument> transformUsers(List<LegacyUserRow> rows) {
    if (rows == null || rows.isEmpty()) {
      return List.of();
    }

    List<UserDocument> documents = new ArrayList<>(rows.size());
    for (LegacyUserRow row : rows) {
      documents.add(transformUser(row));
    }
    return documents;
  }

  public UserDocument transformUser(LegacyUserRow row) {
    if (row == null) {
      return null;
    }

    String username = row.username();
    String email = !row.email().isBlank() ? row.email() : username + "@petstore.sun.com";

    String role = switch (username.toLowerCase()) {
      case "admin" -> "ROLE_ADMIN";
      case "supplier" -> "ROLE_SUPPLIER";
      case "engineer" -> "ROLE_ENGINEER";
      case "root" -> "ROLE_SUPERADMIN";
      default -> "ROLE_CUSTOMER";
    };

    UserDocument user = new UserDocument(
        username,
        row.password(),
        email,
        row.givenName(),
        row.familyName(),
        row.telephone(),
        role
    );

    user.setStatus(row.status().isBlank() ? "active" : row.status());
    user.setMigratedFromLegacy(true);

    // Embedded Profile
    UserProfileDocument profile = new UserProfileDocument(
        row.favoriteCategory().isBlank() ? "FISH" : row.favoriteCategory(),
        row.preferredLanguage().isBlank() ? "en_US" : row.preferredLanguage(),
        row.bannerPreference(),
        row.myListPreference()
    );
    user.setProfile(profile);

    // Embedded Address
    String fullName = (row.givenName() + " " + row.familyName()).trim();
    AddressDocument address = new AddressDocument(
        fullName.isBlank() ? username : fullName,
        row.street1(),
        row.street2(),
        row.city(),
        row.state(),
        row.zipCode(),
        row.country(),
        row.telephone(),
        email
    );
    user.setAddress(address);

    // Embedded Credit Card
    if (!row.cardNumber().isBlank() || !row.cardType().isBlank()) {
      PaymentDocument card = new PaymentDocument(
          row.cardType(),
          row.cardNumber(),
          row.expiryDate()
      );
      user.setCreditCard(card);
    }

    return user;
  }
}
