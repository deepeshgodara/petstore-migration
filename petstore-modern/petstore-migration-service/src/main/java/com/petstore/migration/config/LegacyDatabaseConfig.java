package com.petstore.migration.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Configuration for connecting to the legacy Pet Store HSQLDB database.
 */
@Configuration
public class LegacyDatabaseConfig {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LegacyDatabaseConfig.class);

  @Value("${legacy.datasource.url}")
  private String url;

  @Value("${legacy.datasource.username:sa}")
  private String username;

  @Value("${legacy.datasource.password:}")
  private String password;

  @Value("${legacy.datasource.driver-class-name:org.hsqldb.jdbc.JDBCDriver}")
  private String driverClassName;

  /**
   * Resolves the legacy HSQLDB JDBC URL, prioritizing the live container database
   * directory if present to guarantee inventory and order state fidelity.
   */
  private String resolveJdbcUrl(String rawUrl) {
    if (rawUrl == null || !rawUrl.startsWith("jdbc:hsqldb:file:")) {
      return rawUrl;
    }

    String afterPrefix = rawUrl.substring("jdbc:hsqldb:file:".length());
    String pathPart = afterPrefix;
    String paramsPart = ";readonly=true;shutdown=false;hsqldb.lock_file=false";
    int semicolonIdx = afterPrefix.indexOf(';');
    if (semicolonIdx >= 0) {
      pathPart = afterPrefix.substring(0, semicolonIdx);
      paramsPart = afterPrefix.substring(semicolonIdx);
      if (!paramsPart.contains("hsqldb.lock_file=false")) {
        paramsPart += ";hsqldb.lock_file=false";
      }
    }

    String[] candidateDirs = {
        System.getenv("LEGACY_DB_DIR"),
        "../../legacy_container/tomee/data",
        "../legacy_container/tomee/data",
        "legacy_container/tomee/data",
        "/petstore/legacy_container/tomee/data",
        pathPart,
        "../../docker/data",
        "../docker/data",
        "docker/data"
    };

    for (String dir : candidateDirs) {
      if (dir != null && !dir.isBlank()) {
        java.io.File dirFile = new java.io.File(dir);
        java.io.File scriptFile = dir.endsWith("petstoredb")
            ? new java.io.File(dir + ".script")
            : new java.io.File(dirFile, "petstoredb.script");

        if (scriptFile.exists()) {
          String resolvedDb = dir.endsWith("petstoredb")
              ? new java.io.File(dir).getAbsolutePath()
              : new java.io.File(dirFile, "petstoredb").getAbsolutePath();
          log.info("Resolved authentic legacy HSQLDB database path: {}", resolvedDb);
          return "jdbc:hsqldb:file:" + resolvedDb + paramsPart;
        }
      }
    }

    log.warn("Could not locate petstoredb.script in candidate directories, using raw URL: {}", rawUrl);
    return rawUrl;
  }

  /**
   * Builds the DataSource targeting the legacy relational database in read-only mode.
   *
   * @return DataSource instance
   */
  @Bean(name = "legacyDataSource")
  public DataSource legacyDataSource() {
    String resolvedUrl = resolveJdbcUrl(url);
    log.info("Initializing legacy DataSource with URL: {}", resolvedUrl);

    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(resolvedUrl);
    config.setUsername(username);
    config.setPassword(password);
    config.setDriverClassName(driverClassName);
    config.setReadOnly(true);
    config.setPoolName("LegacyHSQLDBPool");
    return new HikariDataSource(config);
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
