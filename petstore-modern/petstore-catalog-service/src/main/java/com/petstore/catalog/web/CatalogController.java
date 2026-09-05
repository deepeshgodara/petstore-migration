package com.petstore.catalog.web;

import com.petstore.catalog.dto.CategoryResponse;
import com.petstore.catalog.dto.ItemResponse;
import com.petstore.catalog.dto.ProductResponse;
import com.petstore.catalog.service.CatalogService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing endpoints for browsing categories, products, and items
 * with automatic locale resolution (en_US, ja_JP, zh_CN).
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class CatalogController {

  private final CatalogService catalogService;

  public CatalogController(CatalogService catalogService) {
    this.catalogService = catalogService;
  }

  /**
   * Retrieves all categories with localized names and descriptions.
   *
   * @param locale requested locale (defaults to en_US)
   * @return list of localized categories
   */
  @GetMapping("/categories")
  public ResponseEntity<List<CategoryResponse>> getCategories(
      @RequestParam(name = "locale", defaultValue = "en_US") String locale) {
    List<CategoryResponse> categories = catalogService.getAllCategories(locale);
    return ResponseEntity.ok(categories);
  }

  /**
   * Retrieves a single category by identifier.
   *
   * @param categoryId category identifier (e.g., "BIRDS")
   * @param locale requested locale
   * @return CategoryResponse or 404 Not Found
   */
  @GetMapping("/categories/{categoryId}")
  public ResponseEntity<CategoryResponse> getCategoryById(
      @PathVariable String categoryId,
      @RequestParam(name = "locale", defaultValue = "en_US") String locale) {
    return catalogService.getCategoryById(categoryId, locale)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Retrieves products optionally filtered by category or search keyword.
   *
   * @param categoryId optional category filter (e.g., "FISH")
   * @param query optional search keyword
   * @param locale requested locale
   * @return list of matching localized products
   */
  @GetMapping("/products")
  public ResponseEntity<List<ProductResponse>> getProducts(
      @RequestParam(name = "categoryId", required = false) String categoryId,
      @RequestParam(name = "query", required = false) String query,
      @RequestParam(name = "locale", defaultValue = "en_US") String locale) {
    List<ProductResponse> products = catalogService.getProducts(categoryId, query, locale);
    return ResponseEntity.ok(products);
  }

  /**
   * Retrieves a single product by identifier with all embedded items.
   *
   * @param productId product identifier (e.g., "FI-SW-01")
   * @param locale requested locale
   * @return ProductResponse or 404 Not Found
   */
  @GetMapping("/products/{productId}")
  public ResponseEntity<ProductResponse> getProductById(
      @PathVariable String productId,
      @RequestParam(name = "locale", defaultValue = "en_US") String locale) {
    return catalogService.getProductById(productId, locale)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Retrieves a specific item (SKU) by identifier.
   *
   * @param itemId item identifier (e.g., "EST-1")
   * @param locale requested locale
   * @return ItemResponse or 404 Not Found
   */
  @GetMapping("/items/{itemId}")
  public ResponseEntity<ItemResponse> getItemById(
      @PathVariable String itemId,
      @RequestParam(name = "locale", defaultValue = "en_US") String locale) {
    return catalogService.getItemById(itemId, locale)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
