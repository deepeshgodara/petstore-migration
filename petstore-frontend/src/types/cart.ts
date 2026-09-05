import { Item, Product } from './catalog';

export interface CartLineItem {
  id: string; // composite `${product.id}_${item.itemId}`
  product: Product;
  item: Item;
  quantity: number;
}
