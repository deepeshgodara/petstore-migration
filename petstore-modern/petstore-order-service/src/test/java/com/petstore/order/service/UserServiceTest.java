package com.petstore.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petstore.order.dto.UserLoginRequest;
import com.petstore.order.dto.UserRegistrationRequest;
import com.petstore.order.dto.UserResponse;
import com.petstore.order.kafka.UserEventProducer;
import com.petstore.order.repository.UserRepository;
import com.petstore.user.document.UserDocument;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link UserService}.
 */
class UserServiceTest {

  private UserRepository userRepository;
  private UserEventProducer userEventProducer;
  private UserService userService;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    userEventProducer = mock(UserEventProducer.class);
    userService = new UserService(userRepository, userEventProducer);
  }

  @Test
  @DisplayName("Should register new user, persist in MongoDB, and emit Kafka event")
  void shouldRegisterNewUser() {
    UserRegistrationRequest req = new UserRegistrationRequest(
        "alice_smith",
        "secret123",
        "alice@example.com",
        "Alice",
        "Smith",
        "555-9876",
        "123 Main St",
        "",
        "Austin",
        "TX",
        "78701",
        "USA",
        "en_US",
        "FISH"
    );

    when(userRepository.existsByUsernameIgnoreCase("alice_smith")).thenReturn(false);
    when(userRepository.save(any(UserDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UserResponse response = userService.register(req);

    assertThat(response.username()).isEqualTo("alice_smith");
    assertThat(response.email()).isEqualTo("alice@example.com");
    assertThat(response.name()).isEqualTo("Alice Smith");
    assertThat(response.role()).isEqualTo("ROLE_CUSTOMER");
    assertThat(response.profile().getFavoriteCategory()).isEqualTo("FISH");

    verify(userRepository).save(any(UserDocument.class));
    verify(userEventProducer).publishUserCreated(any(UserDocument.class));
  }

  @Test
  @DisplayName("Should reject registration when username already exists")
  void shouldRejectDuplicateUsername() {
    UserRegistrationRequest req = new UserRegistrationRequest(
        "existing_user",
        "pass",
        "u@example.com",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "USA",
        "en_US",
        "DOGS"
    );

    when(userRepository.existsByUsernameIgnoreCase("existing_user")).thenReturn(true);

    assertThatThrownBy(() -> userService.register(req))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already registered");
  }

  @Test
  @DisplayName("Should authenticate user with valid credentials")
  void shouldAuthenticateUser() {
    UserDocument user = new UserDocument("shopper", "j2ee", "shopper@sun.com", "Jane", "Shopper", "555-1111", "ROLE_CUSTOMER");
    when(userRepository.findByUsernameIgnoreCase("shopper")).thenReturn(Optional.of(user));

    UserResponse response = userService.login(new UserLoginRequest("shopper", "j2ee"));

    assertThat(response.username()).isEqualTo("shopper");
    assertThat(response.email()).isEqualTo("shopper@sun.com");
  }

  @Test
  @DisplayName("Should reject authentication with invalid password")
  void shouldRejectInvalidPassword() {
    UserDocument user = new UserDocument("shopper", "j2ee", "shopper@sun.com", "Jane", "Shopper", "555-1111", "ROLE_CUSTOMER");
    when(userRepository.findByUsernameIgnoreCase("shopper")).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> userService.login(new UserLoginRequest("shopper", "wrongpass")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid username or credentials");
  }
}
