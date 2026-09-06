package com.petstore.migration.reader;

import com.petstore.migration.model.LegacyUserRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

/**
 * JDBC Cursor Reader streaming user, customer, profile, and account data
 * from the legacy relational database across 7 joined tables.
 */
@Component
public class LegacyUserCursorReader {

  private static final String SELECT_USERS_SQL =
      "SELECT u.USERNAME, u.PASSWORD, "
          + "COALESCE(a.STATUS, 'active') AS STATUS, "
          + "ci.EMAIL, ci.GIVENNAME, ci.FAMILYNAME, ci.TELEPHONE, "
          + "p.FAVORITECATEGORY, p.PREFERREDLANGUAGE, "
          + "p.BANNERPREFERENCE, p.MYLISTPREFERENCE, "
          + "addr.STREETNAME1, addr.STREETNAME2, addr.CITY, addr.STATE, addr.ZIPCODE, addr.COUNTRY, "
          + "cc.CARDNUMBER, cc.CARDTYPE, cc.EXPIRYDATE "
          + "FROM PUBLIC.USER u "
          + "LEFT JOIN PUBLIC.CUSTOMER c ON u.USERNAME = c.USERID "
          + "LEFT JOIN PUBLIC.PROFILE p ON c.PROFILE_OPENEJB_PK = p.OPENEJB_PK "
          + "LEFT JOIN PUBLIC.ACCOUNT a ON c.ACCOUNT_OPENEJB_PK = a.OPENEJB_PK "
          + "LEFT JOIN PUBLIC.CONTACTINFO ci ON a.CONTACTINFO_OPENEJB_PK = ci.OPENEJB_PK "
          + "LEFT JOIN PUBLIC.ADDRESS addr ON ci.ADDRESS_OPENEJB_PK = addr.OPENEJB_PK "
          + "LEFT JOIN PUBLIC.CREDITCARD cc ON a.CREDITCARD_OPENEJB_PK = cc.OPENEJB_PK "
          + "ORDER BY u.USERNAME";

  private final JdbcTemplate jdbcTemplate;

  public LegacyUserCursorReader(@Qualifier("legacyJdbcTemplate") JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Reads all user rows joined across the legacy customer subsystem.
   *
   * @return list of LegacyUserRow
   */
  public List<LegacyUserRow> readAllUsers() {
    return jdbcTemplate.query(SELECT_USERS_SQL, new UserRowMapper());
  }

  private static class UserRowMapper implements RowMapper<LegacyUserRow> {

    @Override
    public LegacyUserRow mapRow(ResultSet rs, int rowNum) throws SQLException {
      Object bannerObj = rs.getObject("BANNERPREFERENCE");
      boolean bannerPreference = bannerObj == null
          || Boolean.TRUE.equals(bannerObj)
          || "1".equals(String.valueOf(bannerObj))
          || "true".equalsIgnoreCase(String.valueOf(bannerObj));

      Object myListObj = rs.getObject("MYLISTPREFERENCE");
      boolean myListPreference = myListObj == null
          || Boolean.TRUE.equals(myListObj)
          || "1".equals(String.valueOf(myListObj))
          || "true".equalsIgnoreCase(String.valueOf(myListObj));

      return new LegacyUserRow(
          rs.getString("USERNAME"),
          rs.getString("PASSWORD"),
          rs.getString("STATUS"),
          rs.getString("EMAIL"),
          rs.getString("GIVENNAME"),
          rs.getString("FAMILYNAME"),
          rs.getString("TELEPHONE"),
          rs.getString("FAVORITECATEGORY"),
          rs.getString("PREFERREDLANGUAGE"),
          bannerPreference,
          myListPreference,
          rs.getString("STREETNAME1"),
          rs.getString("STREETNAME2"),
          rs.getString("CITY"),
          rs.getString("STATE"),
          rs.getString("ZIPCODE"),
          rs.getString("COUNTRY"),
          rs.getString("CARDNUMBER"),
          rs.getString("CARDTYPE"),
          rs.getString("EXPIRYDATE")
      );
    }
  }
}
