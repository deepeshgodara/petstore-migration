package com.petstore.user.document;

import com.petstore.order.document.AddressDocument;
import com.petstore.order.document.PaymentDocument;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB document representing a Pet Store user and customer aggregate.
 * Encapsulates relational data from legacy USER, CUSTOMER, ACCOUNT, PROFILE,
 * CONTACTINFO, ADDRESS, and CREDITCARD tables into a unified document aggregate.
 * Maps to the {@code petstore_users} collection in MongoDB.
 */
@Document(collection = "petstore_users")
public class UserDocument implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  private String username;

  private String password;

  @Indexed
  private String email;

  private String givenName;
  private String familyName;
  private String telephone;

  private String status = "active";

  @Indexed
  private String role = "ROLE_CUSTOMER";

  private UserProfileDocument profile = new UserProfileDocument();
  private AddressDocument address = new AddressDocument();
  private PaymentDocument creditCard = new PaymentDocument();

  private Instant createdAt = Instant.now();
  private Instant updatedAt = Instant.now();
  private boolean migratedFromLegacy = false;

  public UserDocument() {}

  public UserDocument(
      String username,
      String password,
      String email,
      String givenName,
      String familyName,
      String telephone,
      String role) {
    this.username = username;
    this.password = password;
    this.email = email;
    this.givenName = givenName;
    this.familyName = familyName;
    this.telephone = telephone;
    this.role = role != null ? role : "ROLE_CUSTOMER";
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  public String getFamilyName() {
    return familyName;
  }

  public void setFamilyName(String familyName) {
    this.familyName = familyName;
  }

  public String getTelephone() {
    return telephone;
  }

  public void setTelephone(String telephone) {
    this.telephone = telephone;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public UserProfileDocument getProfile() {
    return profile;
  }

  public void setProfile(UserProfileDocument profile) {
    this.profile = profile;
  }

  public AddressDocument getAddress() {
    return address;
  }

  public void setAddress(AddressDocument address) {
    this.address = address;
  }

  public PaymentDocument getCreditCard() {
    return creditCard;
  }

  public void setCreditCard(PaymentDocument creditCard) {
    this.creditCard = creditCard;
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
    if (this == o) return true;
    if (!(o instanceof UserDocument that)) return false;
    return Objects.equals(username, that.username);
  }

  @Override
  public int hashCode() {
    return Objects.hash(username);
  }

  @Override
  public String toString() {
    return "UserDocument{" +
        "username='" + username + '\'' +
        ", email='" + email + '\'' +
        ", role='" + role + '\'' +
        ", status='" + status + '\'' +
        ", migratedFromLegacy=" + migratedFromLegacy +
        '}';
  }
}
