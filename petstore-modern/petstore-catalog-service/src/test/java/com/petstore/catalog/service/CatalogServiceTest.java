package com.petstore.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.petstore.catalog.document.CategoryDocument;
import com.petstore.catalog.document.ItemDocument;
import com.petstore.catalog.document.ProductDocument;
import com.petstore.catalog.dto.CategoryResponse;
import com.petstore.catalog.dto.ItemResponse;
import com.petstore.catalog.dto.ProductResponse;
import com.petstore.catalog.repository.CategoryRepository;
import com.petstore.catalog.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CatalogService}.
 */
class CatalogServiceTest {

  private CategoryRepository categoryRepository;
  private ProductRepository productRepository;
  private CatalogService catalogService;

  @BeforeEach
  void setUp() {
    categoryRepository = mock(CategoryRepository.class);
    productRepository = mock(ProductRepository.class);
    catalogService = new CatalogService(categoryRepository, productRepository);
  }

  @Test
  @DisplayName("Should return all categories with localized names resolved")
  void shouldGetAllCategoriesLocalized() {
    CategoryDocument birds = new CategoryDocument(
        "BIRDS",
        Map.of("en_US", "Birds", "ja_JP", "鳥", "zh_CN", "鸟"),
        Map.of("en_US", "Birds description"),
        "birds.gif"
    );
    when(categoryRepository.findAll()).thenReturn(List.of(birds));

    List<CategoryResponse> resultEn = catalogService.getAllCategories("en_US");
    assertThat(resultEn).hasSize(1);
    assertThat(resultEn.get(0).name()).isEqualTo("Birds");

    List<CategoryResponse> resultJa = catalogService.getAllCategories("ja_JP");
    assertThat(resultJa.get(0).name()).isEqualTo("鳥");
  }

  @Test
  @DisplayName("Should retrieve category by ID")
  void shouldGetCategoryById() {
    CategoryDocument fish = new CategoryDocument("FISH", Map.of("en_US", "Fish"), null, "fish.gif");
    when(categoryRepository.findById("FISH")).thenReturn(Optional.of(fish));

    Optional<CategoryResponse> result = catalogService.getCategoryById("FISH", "en_US");
    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo("FISH");
    assertThat(result.get().name()).isEqualTo("Fish");
  }

  @Test
  @DisplayName("Should filter products by category ID")
  void shouldFilterProductsByCategory() {
    ProductDocument p1 = new ProductDocument("FI-01", "FISH", Map.of("en_US", "Goldfish"), null, "fish.gif", null);
    when(productRepository.findByCategoryId("FISH")).thenReturn(List.of(p1));

    List<ProductResponse> products = catalogService.getProducts("FISH", null, "en_US");
    assertThat(products).hasSize(1);
    assertThat(products.get(0).id()).isEqualTo("FI-01");
  }

  @Test
  @DisplayName("Should search products by text query")
  void shouldSearchProductsByQuery() {
    ProductDocument p1 = new ProductDocument("AV-01", "BIRDS", Map.of("en_US", "Amazon Parrot"), null, "bird.gif", null);
    when(productRepository.searchByLocalizedName("Parrot")).thenReturn(List.of(p1));

    List<ProductResponse> products = catalogService.getProducts(null, "Parrot", "en_US");
    assertThat(products).hasSize(1);
    assertThat(products.get(0).id()).isEqualTo("AV-01");
  }

  @Test
  @DisplayName("Should retrieve item by SKU from embedded parent product")
  void shouldGetItemByIdFromEmbeddedProduct() {
    ItemDocument item = new ItemDocument(
        "EST-18", BigDecimal.valueOf(199.99), BigDecimal.valueOf(150.00),
        Map.of("en_US", "Adult Male", "ja_JP", "おとな 雄"), "bird.gif", 50);
    ProductDocument product = new ProductDocument(
        "AV-01", "BIRDS", Map.of("en_US", "Amazon Parrot"), null, "bird.gif", List.of(item));

    when(productRepository.findByItemId("EST-18")).thenReturn(Optional.of(product));

    Optional<ItemResponse> resultEn = catalogService.getItemById("EST-18", "en_US");
    assertThat(resultEn).isPresent();
    assertThat(resultEn.get().itemId()).isEqualTo("EST-18");
    assertThat(resultEn.get().attribute()).isEqualTo("Adult Male");
    assertThat(resultEn.get().productName()).isEqualTo("Amazon Parrot");

    Optional<ItemResponse> resultJa = catalogService.getItemById("EST-18", "ja_JP");
    assertThat(resultJa.get().attribute()).isEqualTo("おとな 雄");
  }
}
