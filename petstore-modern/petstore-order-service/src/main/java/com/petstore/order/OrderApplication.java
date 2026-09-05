package com.petstore.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for Pet Store Modern Order Microservice.
 */
@SpringBootApplication(scanBasePackages = {"com.petstore.order", "com.petstore.common"})
public class OrderApplication {

  public static void main(String[] args) {
    SpringApplication.run(OrderApplication.class, args);
  }
}
