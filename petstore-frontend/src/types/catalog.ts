/**
 * Type definitions for the Pet Store modern catalog and multi-lingual system.
 */

export type Locale = 'en_US' | 'ja_JP' | 'zh_CN';

export interface LocaleConfig {
  code: Locale;
  label: string;
  nativeLabel: string;
  flag: string;
}

export const SUPPORTED_LOCALES: LocaleConfig[] = [
  { code: 'en_US', label: 'English (US)', nativeLabel: 'English', flag: '🇺🇸' },
  { code: 'ja_JP', label: 'Japanese', nativeLabel: '日本語', flag: '🇯🇵' },
  { code: 'zh_CN', label: 'Chinese (Simplified)', nativeLabel: '简体中文', flag: '🇨🇳' },
];

export interface Category {
  id: string;
  name: string;
  description: string;
  image: string;
  names: Record<string, string>;
  descriptions: Record<string, string>;
}

export interface Item {
  itemId: string;
  productId: string;
  productName: string;
  listPrice: number;
  unitCost: number;
  attribute: string;
  image: string;
  inventoryQuantity: number;
  attributes: Record<string, string>;
}

export interface Product {
  id: string;
  categoryId: string;
  name: string;
  description: string;
  image: string;
  items: Item[];
  names: Record<string, string>;
  descriptions: Record<string, string>;
}
