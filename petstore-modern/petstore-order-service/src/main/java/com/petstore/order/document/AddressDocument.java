package com.petstore.order.document;

import java.io.Serializable;
import java.util.Objects;

/**
 * Embedded document representing billing or shipping address details.
 */
public class AddressDocument implements Serializable {

  private static final long serialVersionUID = 1L;

  private String name;
  private String address1;
  private String address2;
  private String city;
  private String state;
  private String postalCode;
  private String country;
  private String telephone;
  private String email;

  public AddressDocument() {}

  public AddressDocument(
      String name,
      String address1,
      String address2,
      String city,
      String state,
      String postalCode,
      String country,
      String telephone,
      String email) {
    this.name = name;
    this.address1 = address1;
    this.address2 = address2;
    this.city = city;
    this.state = state;
    this.postalCode = postalCode;
    this.country = country;
    this.telephone = telephone;
    this.email = email;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAddress1() {
    return address1;
  }

  public void setAddress1(String address1) {
    this.address1 = address1;
  }

  public String getAddress2() {
    return address2;
  }

  public void setAddress2(String address2) {
    this.address2 = address2;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public String getTelephone() {
    return telephone;
  }

  public void setTelephone(String telephone) {
    this.telephone = telephone;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AddressDocument that)) {
      return false;
    }
    return Objects.equals(name, that.name)
        && Objects.equals(address1, that.address1)
        && Objects.equals(postalCode, that.postalCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, address1, postalCode);
  }

  @Override
  public String toString() {
    return "AddressDocument{"
        + "name='" + name + '\''
        + ", city='" + city + '\''
        + ", country='" + country + '\''
        + '}';
  }
}
