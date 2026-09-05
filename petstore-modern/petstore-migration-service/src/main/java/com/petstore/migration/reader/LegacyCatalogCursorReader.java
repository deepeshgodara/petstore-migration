package com.petstore.migration.reader;

import com.petstore.migration.model.LegacyCategoryRow;
import com.petstore.migration.model.LegacyItemRow;
import com.petstore.migration.model.LegacyProductRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

/**
 * JDBC Cursor Reader streaming catalog rows from the legacy relational database
 * (CATEGORY, PRODUCT, ITEM, and their respective multi-lingual DETAILS tables).
 */
@Component
public class LegacyCatalogCursorReader {

  private static final String SELECT_CATEGORIES_SQL =
      "SELECT c.CATID, cd.LOCALE, cd.NAME, cd.IMAGE, cd.DESCN "
          + "FROM PUBLIC.CATEGORY c "
          + "JOIN PUBLIC.CATEGORY_DETAILS cd ON c.CATID = cd.CATID "
          + "ORDER BY c.CATID, cd.LOCALE";

  private static final String SELECT_PRODUCTS_SQL =
      "SELECT p.PRODUCTID, p.CATID, pd.LOCALE, pd.NAME, pd.IMAGE, pd.DESCN "
          + "FROM PUBLIC.PRODUCT p "
          + "JOIN PUBLIC.PRODUCT_DETAILS pd ON p.PRODUCTID = pd.PRODUCTID "
          + "ORDER BY p.PRODUCTID, pd.LOCALE";

  private static final String SELECT_ITEMS_SQL =
      "SELECT i.ITEMID, i.PRODUCTID, id.LISTPRICE, id.UNITCOST, id.LOCALE, id.IMAGE, "
          + "id.DESCN, id.ATTR1, COALESCE(inv.QUANTITY, 0) AS QUANTITY "
          + "FROM PUBLIC.ITEM i "
          + "JOIN PUBLIC.ITEM_DETAILS id ON i.ITEMID = id.ITEMID "
          + "LEFT JOIN PUBLIC.INVENTORY inv ON i.ITEMID = inv.ITEMID "
          + "ORDER BY i.ITEMID, id.LOCALE";

  private final JdbcTemplate jdbcTemplate;

  public LegacyCatalogCursorReader(@Qualifier("legacyJdbcTemplate") JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Reads all category rows across all locales.
   *
   * @return list of LegacyCategoryRow
   */
  public List<LegacyCategoryRow> readAllCategories() {
    return jdbcTemplate.query(SELECT_CATEGORIES_SQL, new CategoryRowMapper());
  }

  /**
   * Streams category rows to the provided consumer using cursor fetch semantics.
   *
   * @param consumer the row consumer
   */
  public void streamCategories(Consumer<LegacyCategoryRow> consumer) {
    CategoryRowMapper mapper = new CategoryRowMapper();
    jdbcTemplate.query(SELECT_CATEGORIES_SQL, rs -> {
      consumer.accept(mapper.mapRow(rs, rs.getRow()));
    });
  }

  /**
   * Reads all product rows across all locales.
   *
   * @return list of LegacyProductRow
   */
  public List<LegacyProductRow> readAllProducts() {
    return jdbcTemplate.query(SELECT_PRODUCTS_SQL, new ProductRowMapper());
  }

  /**
   * Streams product rows to the provided consumer.
   *
   * @param consumer the row consumer
   */
  public void streamProducts(Consumer<LegacyProductRow> consumer) {
    ProductRowMapper mapper = new ProductRowMapper();
    jdbcTemplate.query(SELECT_PRODUCTS_SQL, rs -> {
      consumer.accept(mapper.mapRow(rs, rs.getRow()));
    });
  }

  /**
   * Reads all item and inventory rows across all locales.
   *
   * @return list of LegacyItemRow
   */
  public List<LegacyItemRow> readAllItems() {
    return jdbcTemplate.query(SELECT_ITEMS_SQL, new ItemRowMapper());
  }

  /**
   * Streams item rows to the provided consumer.
   *
   * @param consumer the row consumer
   */
  public void streamItems(Consumer<LegacyItemRow> consumer) {
    ItemRowMapper mapper = new ItemRowMapper();
    jdbcTemplate.query(SELECT_ITEMS_SQL, rs -> {
      consumer.accept(mapper.mapRow(rs, rs.getRow()));
    });
  }

  private static class CategoryRowMapper implements RowMapper<LegacyCategoryRow> {
    @Override
    public LegacyCategoryRow mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new LegacyCategoryRow(
          rs.getString("CATID"),
          rs.getString("LOCALE"),
          rs.getString("NAME"),
          rs.getString("IMAGE"),
          rs.getString("DESCN")
      );
    }
  }

  private static class ProductRowMapper implements RowMapper<LegacyProductRow> {
    @Override
    public LegacyProductRow mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new LegacyProductRow(
          rs.getString("PRODUCTID"),
          rs.getString("CATID"),
          rs.getString("LOCALE"),
          rs.getString("NAME"),
          rs.getString("IMAGE"),
          rs.getString("DESCN")
      );
    }
  }

  private static class ItemRowMapper implements RowMapper<LegacyItemRow> {
    @Override
    public LegacyItemRow mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new LegacyItemRow(
          rs.getString("ITEMID"),
          rs.getString("PRODUCTID"),
          rs.getBigDecimal("LISTPRICE"),
          rs.getBigDecimal("UNITCOST"),
          rs.getString("LOCALE"),
          rs.getString("IMAGE"),
          rs.getString("DESCN"),
          rs.getString("ATTR1"),
          rs.getInt("QUANTITY")
      );
    }
  }
}
