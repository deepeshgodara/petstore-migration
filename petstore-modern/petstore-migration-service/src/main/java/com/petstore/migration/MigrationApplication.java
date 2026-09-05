package com.petstore.migration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for Pet Store Modern Migration & Shadow Reconciliation Microservice.
 */
@SpringBootApplication
public class MigrationApplication {

  public static void main(String[] args) {
    SpringApplication.run(MigrationApplication.class, args);
  }
}
