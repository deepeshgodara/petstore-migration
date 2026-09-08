import { Locale } from '../types/catalog';

/**
 * Currency Formatter Utility for Java Pet Store.
 * Formats prices according to the active locale matching legacy PetStore 1.3.1 conventions:
 * - en_US: USD ($16.50)
 * - ja_JP: JPY (￥1,951)
 * - zh_CN: CNY (￥142.00)
 */
export function formatCurrency(
  amount: number | string | undefined | null,
  locale: Locale = 'en_US'
): string {
  const num = Number(amount) || 0;
  if (locale === 'ja_JP') {
    return `￥${Math.round(num).toLocaleString('ja-JP')}`;
  }
  if (locale === 'zh_CN') {
    return `￥${num.toFixed(2)}`;
  }
  return `$${num.toFixed(2)}`;
}

export function getCurrencySymbol(locale: Locale = 'en_US'): string {
  if (locale === 'ja_JP' || locale === 'zh_CN') {
    return '￥';
  }
  return '$';
}
