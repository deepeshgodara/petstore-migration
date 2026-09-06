package com.petstore.order.dto;

import java.io.Serializable;

/**
 * Payload for user authentication REST requests.
 */
public record UserLoginRequest(
    String username,
    String password
) implements Serializable {

  public UserLoginRequest {
    username = username != null ? username.trim() : "";
    password = password != null ? password.trim() : "";
  }
}
