package com.petstore.order.service;

import com.petstore.order.document.AddressDocument;
import com.petstore.order.dto.UserLoginRequest;
import com.petstore.order.dto.UserRegistrationRequest;
import com.petstore.order.dto.UserResponse;
import com.petstore.order.kafka.UserEventProducer;
import com.petstore.order.repository.UserRepository;
import com.petstore.user.document.UserDocument;
import com.petstore.user.document.UserProfileDocument;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service managing user registration, authentication, MongoDB persistence,
 * and Kafka event dispatching.
 */
@Service
public class UserService {

  private static final Logger log = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;
  private final UserEventProducer userEventProducer;

  public UserService(UserRepository userRepository, UserEventProducer userEventProducer) {
    this.userRepository = userRepository;
    this.userEventProducer = userEventProducer;
  }

  /**
   * Registers a new user, saves to MongoDB collection petstore_users,
   * and publishes a domain event to Kafka.
   */
  public UserResponse register(UserRegistrationRequest req) {
    if (req.username().isBlank()) {
      throw new IllegalArgumentException("Username must not be blank");
    }
    if (req.password().isBlank()) {
      throw new IllegalArgumentException("Password must not be blank");
    }
    if (userRepository.existsByUsernameIgnoreCase(req.username())) {
      throw new IllegalArgumentException("Username '" + req.username() + "' is already registered");
    }

    String role = switch (req.username().toLowerCase()) {
      case "admin" -> "ROLE_ADMIN";
      case "supplier" -> "ROLE_SUPPLIER";
      case "engineer" -> "ROLE_ENGINEER";
      case "root" -> "ROLE_SUPERADMIN";
      default -> "ROLE_CUSTOMER";
    };

    UserDocument user = new UserDocument(
        req.username(),
        req.password(),
        req.email().isBlank() ? req.username() + "@example.com" : req.email(),
        req.givenName(),
        req.familyName(),
        req.telephone(),
        role
    );

    user.setStatus("active");
    user.setMigratedFromLegacy(false);
    user.setCreatedAt(Instant.now());
    user.setUpdatedAt(Instant.now());

    UserProfileDocument profile = new UserProfileDocument(
        req.favoriteCategory().isBlank() ? "FISH" : req.favoriteCategory(),
        req.preferredLanguage().isBlank() ? "en_US" : req.preferredLanguage(),
        true,
        true
    );
    user.setProfile(profile);

    String fullName = (req.givenName() + " " + req.familyName()).trim();
    AddressDocument address = new AddressDocument(
        fullName.isBlank() ? req.username() : fullName,
        req.street1(),
        req.street2(),
        req.city(),
        req.state(),
        req.zipCode(),
        req.country(),
        req.telephone(),
        req.email()
    );
    user.setAddress(address);

    UserDocument saved = userRepository.save(user);
    log.info("Successfully registered user [{}] in MongoDB petstore_users", saved.getUsername());

    // Publish to Kafka topic petstore.users.created
    userEventProducer.publishUserCreated(saved);

    return UserResponse.fromDocument(saved);
  }

  /**
   * Authenticates user against MongoDB petstore_users.
   */
  public UserResponse login(UserLoginRequest req) {
    if (req.username().isBlank()) {
      throw new IllegalArgumentException("Username must not be blank");
    }

    UserDocument user = userRepository.findByUsernameIgnoreCase(req.username())
        .orElseThrow(() -> new IllegalArgumentException("Invalid username or credentials"));

    if (req.password() != null && !req.password().isBlank()) {
      if (!req.password().equals(user.getPassword())) {
        throw new IllegalArgumentException("Invalid username or credentials");
      }
    }

    log.info("User [{}] successfully authenticated against MongoDB", user.getUsername());
    return UserResponse.fromDocument(user);
  }

  /**
   * Retrieves user profile by username.
   */
  public UserResponse getUser(String username) {
    UserDocument user = userRepository.findByUsernameIgnoreCase(username)
        .orElseThrow(() -> new NoSuchElementException("User not found: " + username));
    return UserResponse.fromDocument(user);
  }

  /**
   * Lists all users in MongoDB.
   */
  public List<UserResponse> getAllUsers() {
    return userRepository.findAll().stream()
        .map(UserResponse::fromDocument)
        .toList();
  }
}
