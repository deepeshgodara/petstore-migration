import React from 'react';
import { Sparkles } from 'lucide-react';
import { Locale } from '../types/catalog';

interface HeroBannerProps {
  locale: Locale;
  totalProducts: number;
}

export const HeroBanner: React.FC<HeroBannerProps> = ({ locale, totalProducts }) => {
  const titles: Record<Locale, { headline: string; subtitle: string; tag: string }> = {
    en_US: {
      tag: 'Enterprise Strangler Fig Architecture',
      headline: 'Next-Gen Java Pet Store',
      subtitle:
        'Modernized to Spring Boot 3.3, Java 21 LTS, MongoDB 7.0, and Apache Kafka with real-time shadow reconciliation.',
    },
    ja_JP: {
      tag: 'エンタープライズ・ストラングラー・パターン',
      headline: '次世代 Java ペットストア',
      subtitle:
        'Spring Boot 3.3、Java 21 LTS、MongoDB 7.0、Apache Kafka によるリアルタイムな並行運用アーキテクチャ。',
    },
    zh_CN: {
      tag: '企业级绞杀者模式架构',
      headline: '新一代 Java 宠物商城',
      subtitle:
        '基于 Spring Boot 3.3、Java 21 LTS、MongoDB 7.0 与 Apache Kafka 构建的高性能微服务架构。',
    },
  };

  const content = titles[locale] || titles.en_US;

  return (
    <section className="hero-section">
      <div className="hero-glow" />
      <div className="hero-content">
        <div className="hero-badge">
          <Sparkles size={14} color="#a5b4fc" />
          <span>{content.tag}</span>
        </div>
        <h1 className="hero-title">{content.headline}</h1>
        <p className="hero-desc">{content.subtitle}</p>

        <div className="hero-stats">
          <div className="hero-stat-item">
            <span className="hero-stat-value">5</span>
            <span className="hero-stat-label">Categories</span>
          </div>
          <div className="hero-stat-item">
            <span className="hero-stat-value">{totalProducts || 16}</span>
            <span className="hero-stat-label">Product Breeds</span>
          </div>
          <div className="hero-stat-item">
            <span className="hero-stat-value">3</span>
            <span className="hero-stat-label">Locales (EN, JA, ZH)</span>
          </div>
          <div className="hero-stat-item">
            <span className="hero-stat-value" style={{ color: '#10b981' }}>Active</span>
            <span className="hero-stat-label">Dual-Write Sync</span>
          </div>
        </div>
      </div>
    </section>
  );
};
