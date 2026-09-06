package com.petstore.order.web;

import com.petstore.order.dto.UserLoginRequest;
import com.petstore.order.dto.UserRegistrationRequest;
import com.petstore.order.dto.UserResponse;
import com.petstore.order.service.UserService;
import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller managing user registrations, logins, and profile lookups.
 * Endpoints: {@code /api/v1/users}
 */
@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  /**
   * Registers a new customer account.
   */
  @PostMapping("/register")
  public ResponseEntity<UserResponse> register(@RequestBody UserRegistrationRequest request) {
    UserResponse response = userService.register(request);
    return ResponseEntity.created(URI.create("/api/v1/users/" + response.username())).body(response);
  }

  /**
   * Authenticates user against MongoDB petstore_users.
   */
  @PostMapping("/login")
  public ResponseEntity<UserResponse> login(@RequestBody UserLoginRequest request) {
    UserResponse response = userService.login(request);
    return ResponseEntity.ok(response);
  }

  /**
   * Looks up a user by username.
   */
  @GetMapping("/{username}")
  public ResponseEntity<UserResponse> getUser(@PathVariable String username) {
    return ResponseEntity.ok(userService.getUser(username));
  }

  /**
   * Lists all users (for administrative auditing and diagnostics).
   */
  @GetMapping
  public ResponseEntity<List<UserResponse>> getAllUsers() {
    return ResponseEntity.ok(userService.getAllUsers());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
  }

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<String> handleNotFound(NoSuchElementException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
  }
}
