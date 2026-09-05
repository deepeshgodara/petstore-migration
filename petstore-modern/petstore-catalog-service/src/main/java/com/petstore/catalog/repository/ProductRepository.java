package com.petstore.catalog.repository;

import com.petstore.catalog.document.ProductDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for Product documents with custom aggregation pipelines.
 */
@Repository
public interface ProductRepository extends MongoRepository<ProductDocument, String> {

  /**
   * Finds all products within a specific category.
   *
   * @param categoryId the category ID (e.g., "FISH", "DOGS")
   * @return list of matching products
   */
  List<ProductDocument> findByCategoryId(String categoryId);

  /**
   * Finds the parent product document containing a specific item/SKU ID.
   *
   * @param itemId the SKU ID (e.g., "EST-1")
   * @return optional containing the parent product if found
   */
  @Query("{'items.itemId': ?0}")
  Optional<ProductDocument> findByItemId(String itemId);

  /**
   * Performs text search across product names using regex matching.
   *
   * @param regexPattern regular expression pattern for matching product name
   * @return list of matching products
   */
  @Query("{'$or': [{'names.en_US': {$regex: ?0, $options: 'i'}}, {'names.ja_JP': {$regex: ?0}}, {'names.zh_CN': {$regex: ?0}}]}")
  List<ProductDocument> searchByLocalizedName(String regexPattern);

  /**
   * Aggregation pipeline to count total products per category.
   *
   * @return list of category count projections
   */
  @Aggregation(pipeline = {
      "{ '$group': { '_id': '$categoryId', 'productCount': { '$sum': 1 } } }",
      "{ '$project': { 'categoryId': '$_id', 'productCount': 1, '_id': 0 } }"
  })
  List<CategoryProductCount> countProductsByCategory();

  /**
   * Projection interface for category product count aggregation.
   */
  interface CategoryProductCount {
    String getCategoryId();
    long getProductCount();
  }
}
