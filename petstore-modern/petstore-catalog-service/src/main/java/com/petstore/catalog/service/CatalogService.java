package com.petstore.catalog.service;

import com.petstore.catalog.document.CategoryDocument;
import com.petstore.catalog.document.ItemDocument;
import com.petstore.catalog.document.ProductDocument;
import com.petstore.catalog.dto.CategoryResponse;
import com.petstore.catalog.dto.ItemResponse;
import com.petstore.catalog.dto.ProductResponse;
import com.petstore.catalog.repository.CategoryRepository;
import com.petstore.catalog.repository.ProductRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service managing catalog browsing, multi-lingual text resolution, and item lookups.
 */
@Service
public class CatalogService {

  private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

  private final CategoryRepository categoryRepository;
  private final ProductRepository productRepository;

  public CatalogService(CategoryRepository categoryRepository, ProductRepository productRepository) {
    this.categoryRepository = categoryRepository;
    this.productRepository = productRepository;
  }

  /**
   * Retrieves all categories with localized names and descriptions for the specified locale.
   *
   * @param locale target locale (e.g., "en_US", "ja_JP", "zh_CN")
   * @return list of localized category responses
   */
  public List<CategoryResponse> getAllCategories(String locale) {
    return categoryRepository.findAll().stream()
        .sorted(Comparator.comparing(CategoryDocument::getId))
        .map(cat -> CategoryResponse.of(cat, locale))
        .toList();
  }

  /**
   * Retrieves a category by identifier.
   *
   * @param categoryId category identifier
   * @param locale target locale
   * @return optional containing CategoryResponse if found
   */
  public Optional<CategoryResponse> getCategoryById(String categoryId, String locale) {
    return categoryRepository.findById(categoryId)
        .map(cat -> CategoryResponse.of(cat, locale));
  }

  /**
   * Retrieves products filtered optionally by category or search query.
   *
   * @param categoryId optional category filter
   * @param query optional search keyword
   * @param locale target locale
   * @return list of localized product responses
   */
  public List<ProductResponse> getProducts(String categoryId, String query, String locale) {
    List<ProductDocument> products;

    if (query != null && !query.isBlank()) {
      log.debug("Searching products by query [{}]", query);
      products = productRepository.searchByLocalizedName(query.trim());
    } else if (categoryId != null && !categoryId.isBlank()) {
      log.debug("Retrieving products for category [{}]", categoryId);
      products = productRepository.findByCategoryId(categoryId.trim().toUpperCase());
    } else {
      products = productRepository.findAll();
    }

    return products.stream()
        .sorted(Comparator.comparing(ProductDocument::getId))
        .map(prod -> ProductResponse.of(prod, locale))
        .toList();
  }

  /**
   * Retrieves a product by identifier.
   *
   * @param productId product identifier
   * @param locale target locale
   * @return optional containing ProductResponse if found
   */
  public Optional<ProductResponse> getProductById(String productId, String locale) {
    return productRepository.findById(productId)
        .map(prod -> ProductResponse.of(prod, locale));
  }

  /**
   * Looks up a specific item/SKU across the catalog by inspecting embedded product items.
   *
   * @param itemId item SKU identifier (e.g., "EST-1")
   * @param locale target locale
   * @return optional containing ItemResponse if found
   */
  public Optional<ItemResponse> getItemById(String itemId, String locale) {
    return productRepository.findByItemId(itemId)
        .flatMap(product -> product.getItems().stream()
            .filter(item -> itemId.equals(item.getItemId()))
            .findFirst()
            .map(item -> ItemResponse.of(item, product, locale))
        );
  }
}
