package com.petstore.migration.reader;

import com.petstore.migration.model.LegacyLineItemRow;
import com.petstore.migration.model.LegacyOrderRow;
import com.petstore.order.document.AddressDocument;
import com.petstore.order.document.LineItemDocument;
import com.petstore.order.document.OrderDocument;
import com.petstore.order.document.OrderStatus;
import com.petstore.order.document.PaymentDocument;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

/**
 * JDBC Cursor Reader extracting order history and manager approvals from the legacy
 * relational database (PURCHASEORDER, MANAGER, LINEITEM, CONTACTINFO, ADDRESS, CREDITCARD)
 * and transforming them into unified modern OrderDocument aggregates.
 */
@Component
public class LegacyOrderCursorReader {

  private static final String SELECT_ORDERS_SQL =
      "SELECT po.POID, po.POUSERID, po.PODATE, po.POVALUE, po.POLOCALE, "
          + "COALESCE(m.STATUS, 'PENDING') AS STATUS, "
          + "ci.GIVENNAME, ci.FAMILYNAME, ci.EMAIL, ci.TELEPHONE, "
          + "addr.STREETNAME1, addr.STREETNAME2, addr.CITY, addr.STATE, addr.ZIPCODE, addr.COUNTRY, "
          + "cc.CARDTYPE, cc.CARDNUMBER, cc.EXPIRYDATE "
          + "FROM PUBLIC.PURCHASEORDER po "
          + "LEFT JOIN PUBLIC.MANAGER m ON po.POID = m.ORDERID "
          + "LEFT JOIN PUBLIC.CONTACTINFO ci ON po.CONTACTINFO_OPENEJB_PK = ci.OPENEJB_PK "
          + "LEFT JOIN PUBLIC.ADDRESS addr ON ci.ADDRESS_OPENEJB_PK = addr.OPENEJB_PK "
          + "LEFT JOIN PUBLIC.CREDITCARD cc ON po.CREDITCARD_OPENEJB_PK = cc.OPENEJB_PK "
          + "ORDER BY po.POID";

  private static final String SELECT_LINEITEMS_SQL =
      "SELECT PURCHASEORDER_LINEITEMS_POID, LINENUMBER, ITEMID, PRODUCTID, CATEGORYID, "
          + "QUANTITY, UNITPRICE "
          + "FROM PUBLIC.LINEITEM "
          + "WHERE PURCHASEORDER_LINEITEMS_POID IS NOT NULL "
          + "ORDER BY PURCHASEORDER_LINEITEMS_POID, LINENUMBER";

  private final JdbcTemplate jdbcTemplate;

  public LegacyOrderCursorReader(@Qualifier("legacyJdbcTemplate") JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Reads raw joined order rows from the legacy database.
   *
   * @return list of LegacyOrderRow
   */
  public List<LegacyOrderRow> readAllOrderRows() {
    return jdbcTemplate.query(SELECT_ORDERS_SQL, new OrderRowMapper());
  }

  /**
   * Reads all line items associated with purchase orders.
   *
   * @return list of LegacyLineItemRow
   */
  public List<LegacyLineItemRow> readAllLineItemRows() {
    return jdbcTemplate.query(SELECT_LINEITEMS_SQL, new LineItemRowMapper());
  }

  /**
   * Reads and transforms legacy purchase orders into fully denormalized modern OrderDocuments.
   *
   * @return list of OrderDocument aggregates
   */
  public List<OrderDocument> readCompleteOrdersAsDocuments() {
    List<LegacyOrderRow> orderRows = readAllOrderRows();
    if (orderRows.isEmpty()) {
      return List.of();
    }

    // Read and group line items by order ID
    List<LegacyLineItemRow> lineItemRows = readAllLineItemRows();
    Map<String, List<LineItemDocument>> lineItemsByOrderId = new LinkedHashMap<>();
    for (LegacyLineItemRow itemRow : lineItemRows) {
      BigDecimal totalCost = itemRow.unitPrice().multiply(BigDecimal.valueOf(itemRow.quantity()));
      LineItemDocument doc = new LineItemDocument(
          itemRow.lineNumber(),
          itemRow.itemId(),
          itemRow.productId(),
          itemRow.categoryId(),
          itemRow.quantity(),
          itemRow.unitPrice(),
          totalCost
      );
      lineItemsByOrderId.computeIfAbsent(itemRow.orderId(), k -> new ArrayList<>()).add(doc);
    }

    List<OrderDocument> orders = new ArrayList<>();
    for (LegacyOrderRow row : orderRows) {
      OrderStatus orderStatus = parseStatus(row.status());
      Instant orderDate = row.poDateMillis() > 0
          ? Instant.ofEpochMilli(row.poDateMillis())
          : Instant.now();

      String customerName = buildFullName(row.givenName(), row.familyName());
      AddressDocument billing = new AddressDocument(
          customerName,
          row.streetName1(),
          row.streetName2(),
          row.city(),
          row.state(),
          row.zipCode(),
          row.country(),
          row.telephone(),
          row.email()
      );

      AddressDocument shipping = new AddressDocument(
          customerName,
          row.streetName1(),
          row.streetName2(),
          row.city(),
          row.state(),
          row.zipCode(),
          row.country(),
          row.telephone(),
          row.email()
      );

      String maskedCard = maskCardNumber(row.cardNumber());
      PaymentDocument payment = new PaymentDocument(row.cardType(), maskedCard, row.expiryDate());

      List<LineItemDocument> lineItems = lineItemsByOrderId.getOrDefault(row.poId(), Collections.emptyList());

      OrderDocument orderDoc = new OrderDocument(
          row.poId(),
          row.userId(),
          orderDate,
          orderStatus,
          row.poValue(),
          row.locale(),
          billing,
          shipping,
          payment,
          lineItems
      );
      orderDoc.setMigratedFromLegacy(true);
      orders.add(orderDoc);
    }

    return orders;
  }

  private OrderStatus parseStatus(String statusStr) {
    if (statusStr == null) {
      return OrderStatus.PENDING;
    }
    try {
      return OrderStatus.valueOf(statusStr.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return OrderStatus.PENDING;
    }
  }

  private String buildFullName(String given, String family) {
    if (given == null && family == null) {
      return "Legacy Customer";
    }
    String g = given != null ? given.trim() : "";
    String f = family != null ? family.trim() : "";
    return (g + " " + f).trim();
  }

  private String maskCardNumber(String raw) {
    if (raw == null || raw.isBlank()) {
      return "XXXX-XXXX-XXXX-0000";
    }
    String clean = raw.trim();
    if (clean.length() <= 4) {
      return "XXXX-XXXX-XXXX-" + clean;
    }
    return "XXXX-XXXX-XXXX-" + clean.substring(clean.length() - 4);
  }

  private static class OrderRowMapper implements RowMapper<LegacyOrderRow> {
    @Override
    public LegacyOrderRow mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new LegacyOrderRow(
          rs.getString("POID"),
          rs.getString("POUSERID"),
          rs.getLong("PODATE"),
          rs.getBigDecimal("POVALUE"),
          rs.getString("POLOCALE"),
          rs.getString("STATUS"),
          rs.getString("GIVENNAME"),
          rs.getString("FAMILYNAME"),
          rs.getString("EMAIL"),
          rs.getString("TELEPHONE"),
          rs.getString("STREETNAME1"),
          rs.getString("STREETNAME2"),
          rs.getString("CITY"),
          rs.getString("STATE"),
          rs.getString("ZIPCODE"),
          rs.getString("COUNTRY"),
          rs.getString("CARDTYPE"),
          rs.getString("CARDNUMBER"),
          rs.getString("EXPIRYDATE")
      );
    }
  }

  private static class LineItemRowMapper implements RowMapper<LegacyLineItemRow> {
    @Override
    public LegacyLineItemRow mapRow(ResultSet rs, int rowNum) throws SQLException {
      int lineNum = 0;
      try {
        String lineNumStr = rs.getString("LINENUMBER");
        if (lineNumStr != null) {
          lineNum = Integer.parseInt(lineNumStr.trim());
        }
      } catch (NumberFormatException ignored) {
        lineNum = rowNum;
      }

      return new LegacyLineItemRow(
          rs.getString("PURCHASEORDER_LINEITEMS_POID"),
          lineNum,
          rs.getString("ITEMID"),
          rs.getString("PRODUCTID"),
          rs.getString("CATEGORYID"),
          rs.getInt("QUANTITY"),
          rs.getBigDecimal("UNITPRICE")
      );
    }
  }
}
