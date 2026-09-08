package com.petstore.common.config;

import java.math.BigDecimal;
import java.util.Arrays;
import org.bson.types.Decimal128;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

/**
 * MongoDB custom conversions configuration.
 * Maps {@link BigDecimal} to native BSON {@link Decimal128} (IEEE 754-2008)
 * to avoid String serialization gotchas and enable index-backed sorting and range aggregations.
 */
@Configuration
public class MongoConfig {

  @Bean
  public MongoCustomConversions mongoCustomConversions() {
    return new MongoCustomConversions(Arrays.asList(
        new BigDecimalToDecimal128Converter(),
        new Decimal128ToBigDecimalConverter(),
        new StringToBigDecimalConverter()
    ));
  }

  @WritingConverter
  public static class BigDecimalToDecimal128Converter implements Converter<BigDecimal, Decimal128> {
    @Override
    public Decimal128 convert(BigDecimal source) {
      return source != null ? new Decimal128(source) : null;
    }
  }

  @ReadingConverter
  public static class Decimal128ToBigDecimalConverter implements Converter<Decimal128, BigDecimal> {
    @Override
    public BigDecimal convert(Decimal128 source) {
      return source != null ? source.bigDecimalValue() : null;
    }
  }

  @ReadingConverter
  public static class StringToBigDecimalConverter implements Converter<String, BigDecimal> {
    @Override
    public BigDecimal convert(String source) {
      if (source == null || source.isBlank()) {
        return null;
      }
      try {
        return new BigDecimal(source.trim());
      } catch (NumberFormatException e) {
        return null;
      }
    }
  }
}
