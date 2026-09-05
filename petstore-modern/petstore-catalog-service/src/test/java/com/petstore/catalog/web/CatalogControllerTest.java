package com.petstore.catalog.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.petstore.catalog.dto.CategoryResponse;
import com.petstore.catalog.dto.ItemResponse;
import com.petstore.catalog.dto.ProductResponse;
import com.petstore.catalog.service.CatalogService;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for {@link CatalogController}.
 */
class CatalogControllerTest {

  private CatalogService catalogService;
  private CatalogController controller;

  @BeforeEach
  void setUp() {
    catalogService = mock(CatalogService.class);
    controller = new CatalogController(catalogService);
  }

  @Test
  @DisplayName("Should return categories list on GET /categories")
  void shouldReturnCategories() {
    CategoryResponse cat = new CategoryResponse("FISH", "Fish", "Desc", "fish.gif", null, null);
    when(catalogService.getAllCategories("en_US")).thenReturn(List.of(cat));

    ResponseEntity<List<CategoryResponse>> response = controller.getCategories("en_US");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody().get(0).id()).isEqualTo("FISH");
  }

  @Test
  @DisplayName("Should return 404 when category not found")
  void shouldReturn404WhenCategoryNotFound() {
    when(catalogService.getCategoryById("UNKNOWN", "en_US")).thenReturn(Optional.empty());

    ResponseEntity<CategoryResponse> response = controller.getCategoryById("UNKNOWN", "en_US");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("Should return products list on GET /products")
  void shouldReturnProducts() {
    ProductResponse prod = new ProductResponse(
        "FI-SW-01", "FISH", "Angelfish", "Desc", "fish.gif", Collections.emptyList(), null, null);
    when(catalogService.getProducts("FISH", null, "en_US")).thenReturn(List.of(prod));

    ResponseEntity<List<ProductResponse>> response = controller.getProducts("FISH", null, "en_US");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody().get(0).name()).isEqualTo("Angelfish");
  }

  @Test
  @DisplayName("Should return item by SKU on GET /items/{itemId}")
  void shouldReturnItemById() {
    ItemResponse item = new ItemResponse(
        "EST-1", "FI-SW-01", "Angelfish", BigDecimal.valueOf(16.50), BigDecimal.valueOf(10.00),
        "Large", "fish.gif", 100, null);
    when(catalogService.getItemById("EST-1", "en_US")).thenReturn(Optional.of(item));

    ResponseEntity<ItemResponse> response = controller.getItemById("EST-1", "en_US");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().itemId()).isEqualTo("EST-1");
  }
}
