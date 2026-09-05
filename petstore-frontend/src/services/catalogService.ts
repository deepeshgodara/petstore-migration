import { Category, Item, Locale, Product } from '../types/catalog';

/**
 * Service providing typed HTTP methods for the Pet Store Catalog API.
 */
class CatalogApiService {
  private readonly baseUrl = '/api/v1';

  /**
   * Fetches all localized categories.
   */
  async getCategories(locale: Locale = 'en_US'): Promise<Category[]> {
    const response = await fetch(`${this.baseUrl}/categories?locale=${encodeURIComponent(locale)}`);
    if (!response.ok) {
      throw new Error(`Failed to fetch categories: ${response.status} ${response.statusText}`);
    }
    return response.json();
  }

  /**
   * Fetches a single category by its identifier.
   */
  async getCategoryById(categoryId: string, locale: Locale = 'en_US'): Promise<Category> {
    const response = await fetch(
      `${this.baseUrl}/categories/${encodeURIComponent(categoryId)}?locale=${encodeURIComponent(locale)}`
    );
    if (!response.ok) {
      throw new Error(`Failed to fetch category [${categoryId}]: ${response.statusText}`);
    }
    return response.json();
  }

  /**
   * Fetches products optionally filtered by category and/or query string.
   */
  async getProducts(
    categoryId?: string,
    query?: string,
    locale: Locale = 'en_US'
  ): Promise<Product[]> {
    const params = new URLSearchParams();
    params.set('locale', locale);
    if (categoryId && categoryId !== 'ALL') {
      params.set('categoryId', categoryId);
    }
    if (query && query.trim().length > 0) {
      params.set('query', query.trim());
    }

    const response = await fetch(`${this.baseUrl}/products?${params.toString()}`);
    if (!response.ok) {
      throw new Error(`Failed to fetch products: ${response.status} ${response.statusText}`);
    }
    return response.json();
  }

  /**
   * Fetches a single product by ID including its child item inventory.
   */
  async getProductById(productId: string, locale: Locale = 'en_US'): Promise<Product> {
    const response = await fetch(
      `${this.baseUrl}/products/${encodeURIComponent(productId)}?locale=${encodeURIComponent(locale)}`
    );
    if (!response.ok) {
      throw new Error(`Failed to fetch product [${productId}]: ${response.statusText}`);
    }
    return response.json();
  }

  /**
   * Fetches an individual item SKU by ID.
   */
  async getItemById(itemId: string, locale: Locale = 'en_US'): Promise<Item> {
    const response = await fetch(
      `${this.baseUrl}/items/${encodeURIComponent(itemId)}?locale=${encodeURIComponent(locale)}`
    );
    if (!response.ok) {
      throw new Error(`Failed to fetch item [${itemId}]: ${response.statusText}`);
    }
    return response.json();
  }
}

export const catalogService = new CatalogApiService();
