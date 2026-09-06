import { Item, Locale } from '../types/catalog';

/**
 * Service managing Supplier operations and inventory stock adjustments.
 */
class SupplierService {
  private readonly baseUrl = '/api/v1';

  /**
   * Fetches all items across products with their current stock levels.
   */
  async getAllItems(locale: Locale = 'en_US'): Promise<Item[]> {
    const response = await fetch(`${this.baseUrl}/items?locale=${encodeURIComponent(locale)}`);
    if (!response.ok) {
      throw new Error(`Failed to fetch inventory items: ${response.status} ${response.statusText}`);
    }
    return response.json();
  }

  /**
   * Updates an item's inventory stock quantity in MongoDB.
   */
  async updateInventory(itemId: string, quantity: number, locale: Locale = 'en_US'): Promise<Item> {
    const response = await fetch(
      `${this.baseUrl}/items/${encodeURIComponent(itemId)}/inventory?locale=${encodeURIComponent(locale)}`,
      {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ quantity }),
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to update inventory for item [${itemId}]: ${response.statusText}`);
    }
    return response.json();
  }
}

export const supplierService = new SupplierService();
