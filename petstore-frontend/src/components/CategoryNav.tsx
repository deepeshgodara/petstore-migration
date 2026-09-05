import React from 'react';
import { Category, Locale } from '../types/catalog';
import { Sparkles } from 'lucide-react';

interface CategoryNavProps {
  categories: Category[];
  selectedCategory: string;
  onSelectCategory: (categoryId: string) => void;
  locale: Locale;
}

export const CategoryNav: React.FC<CategoryNavProps> = ({
  categories,
  selectedCategory,
  onSelectCategory,
  locale,
}) => {
  const allLabel =
    locale === 'ja_JP' ? 'すべてのペット' : locale === 'zh_CN' ? '全部宠物' : 'All Pets';

  return (
    <nav className="category-nav-bar" aria-label="Pet Categories">
      <button
        className={`category-pill ${selectedCategory === 'ALL' ? 'active' : ''}`}
        onClick={() => onSelectCategory('ALL')}
      >
        <Sparkles size={16} />
        <span>{allLabel}</span>
      </button>

      {categories.map((cat) => {
        const isActive = selectedCategory === cat.id;
        const iconSrc = cat.image ? `/images/${cat.image}` : '/images/birds_icon.gif';

        return (
          <button
            key={cat.id}
            className={`category-pill ${isActive ? 'active' : ''}`}
            onClick={() => onSelectCategory(cat.id)}
          >
            <img
              src={iconSrc}
              alt={cat.name}
              className="category-pill-icon"
              onError={(e) => {
                // Graceful fallback to avoid broken image icon
                (e.target as HTMLImageElement).style.display = 'none';
              }}
            />
            <span>{cat.name}</span>
          </button>
        );
      })}
    </nav>
  );
};
