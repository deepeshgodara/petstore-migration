package com.petstore.common.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event dispatched to Kafka when a user registers or updates profile in Pet Store.
 */
public class UserDomainEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final String TOPIC_USERS_CREATED = "petstore.users.created";
  public static final String TOPIC_USERS_DUALWRITE = "petstore.users.dualwrite";

  private final String eventId;
  private final String username;
  private final String email;
  private final String role;
  private final String eventType; // "USER_CREATED", "USER_UPDATED"
  private final Instant timestamp;

  @JsonCreator
  public UserDomainEvent(
      @JsonProperty("eventId") String eventId,
      @JsonProperty("username") String username,
      @JsonProperty("email") String email,
      @JsonProperty("role") String role,
      @JsonProperty("eventType") String eventType,
      @JsonProperty("timestamp") Instant timestamp) {
    this.eventId = eventId != null ? eventId : UUID.randomUUID().toString();
    this.username = username;
    this.email = email;
    this.role = role;
    this.eventType = eventType != null ? eventType : "USER_CREATED";
    this.timestamp = timestamp != null ? timestamp : Instant.now();
  }

  public static UserDomainEvent created(String username, String email, String role) {
    return new UserDomainEvent(
        UUID.randomUUID().toString(),
        username,
        email,
        role,
        "USER_CREATED",
        Instant.now()
    );
  }

  public String getEventId() {
    return eventId;
  }

  public String getUsername() {
    return username;
  }

  public String getEmail() {
    return email;
  }

  public String getRole() {
    return role;
  }

  public String getEventType() {
    return eventType;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return "UserDomainEvent{" +
        "eventId='" + eventId + '\'' +
        ", username='" + username + '\'' +
        ", email='" + email + '\'' +
        ", role='" + role + '\'' +
        ", eventType='" + eventType + '\'' +
        ", timestamp=" + timestamp +
        '}';
  }
}
