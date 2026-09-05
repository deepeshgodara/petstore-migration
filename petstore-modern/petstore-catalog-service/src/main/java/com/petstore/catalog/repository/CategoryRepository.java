package com.petstore.catalog.repository;

import com.petstore.catalog.document.CategoryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for Category documents.
 */
@Repository
public interface CategoryRepository extends MongoRepository<CategoryDocument, String> {
}
