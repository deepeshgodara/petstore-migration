package com.petstore.migration.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Configuration for connecting to the legacy Pet Store HSQLDB database.
 */
@Configuration
public class LegacyDatabaseConfig {

  @Value("${legacy.datasource.url}")
  private String url;

  @Value("${legacy.datasource.username:sa}")
  private String username;

  @Value("${legacy.datasource.password:}")
  private String password;

  @Value("${legacy.datasource.driver-class-name:org.hsqldb.jdbc.JDBCDriver}")
  private String driverClassName;

  /**
   * Builds the DataSource targeting the legacy relational database in read-only mode.
   *
   * @return DataSource instance
   */
  @Bean(name = "legacyDataSource")
  public DataSource legacyDataSource() {
    return DataSourceBuilder.create()
        .url(url)
        .username(username)
        .password(password)
        .driverClassName(driverClassName)
        .build();
  }

  /**
   * Configures a JdbcTemplate for executing streaming queries against the legacy database.
   *
   * @param legacyDataSource the legacy database datasource
   * @return JdbcTemplate configured for cursor-based reads
   */
  @Bean(name = "legacyJdbcTemplate")
  public JdbcTemplate legacyJdbcTemplate(DataSource legacyDataSource) {
    JdbcTemplate template = new JdbcTemplate(legacyDataSource);
    template.setFetchSize(500);
    return template;
  }
}
