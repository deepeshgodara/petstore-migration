package com.petstore.order.repository;

import com.petstore.user.document.UserDocument;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for user and customer documents.
 * Queries collection {@code petstore_users}.
 */
@Repository
public interface UserRepository extends MongoRepository<UserDocument, String> {

  Optional<UserDocument> findByUsernameIgnoreCase(String username);

  Optional<UserDocument> findByEmailIgnoreCase(String email);

  boolean existsByUsernameIgnoreCase(String username);

  boolean existsByEmailIgnoreCase(String email);
}
