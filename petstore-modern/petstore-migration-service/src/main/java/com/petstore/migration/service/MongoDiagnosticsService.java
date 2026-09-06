package com.petstore.migration.service;

import com.petstore.migration.model.MongoDiagnosticsResponse;
import java.util.HashMap;
import java.util.Map;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

/**
 * Service that interrogates the MongoDB replica set engine commands
 * ('serverStatus', 'dbStats', collection metrics) to provide real-time
 * performance telemetry for engineering teams and MongoDB Compass integration.
 */
@Service
public class MongoDiagnosticsService {

  private static final Logger log = LoggerFactory.getLogger(MongoDiagnosticsService.class);
  private static final String COMPASS_URI =
      "mongodb://localhost:27017/petstore?replicaSet=rs0&directConnection=true";

  private final MongoTemplate mongoTemplate;

  public MongoDiagnosticsService(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  /**
   * Queries real-time engine statistics and formats them into a comprehensive diagnostics model.
   *
   * @return MongoDiagnosticsResponse
   */
  public MongoDiagnosticsResponse getDiagnostics() {
    try {
      Document serverStatus = mongoTemplate.getDb().runCommand(new Document("serverStatus", 1));
      Document dbStats = mongoTemplate.getDb().runCommand(new Document("dbStats", 1));

      String version = serverStatus.getString("version");
      if (version == null) {
        version = "7.0.x";
      }

      long uptime = serverStatus.get("uptime", Number.class) != null
          ? serverStatus.get("uptime", Number.class).longValue()
          : 0L;

      // Connections
      Map<String, Object> connMap = new HashMap<>();
      Document conns = serverStatus.get("connections", Document.class);
      if (conns != null) {
        connMap.put("current", conns.get("current"));
        connMap.put("available", conns.get("available"));
        connMap.put("active", conns.get("active"));
        connMap.put("totalCreated", conns.get("totalCreated"));
      }

      // Opcounters
      Map<String, Object> opMap = new HashMap<>();
      Document opcounters = serverStatus.get("opcounters", Document.class);
      if (opcounters != null) {
        opMap.put("insert", opcounters.get("insert"));
        opMap.put("query", opcounters.get("query"));
        opMap.put("update", opcounters.get("update"));
        opMap.put("delete", opcounters.get("delete"));
        opMap.put("command", opcounters.get("command"));
      }

      // Memory
      Map<String, Object> memMap = new HashMap<>();
      Document mem = serverStatus.get("mem", Document.class);
      if (mem != null) {
        memMap.put("residentMb", mem.get("resident"));
        memMap.put("virtualMb", mem.get("virtual"));
      }

      // WiredTiger Cache
      Map<String, Object> cacheMap = new HashMap<>();
      Document wiredTiger = serverStatus.get("wiredTiger", Document.class);
      if (wiredTiger != null) {
        Document cache = wiredTiger.get("cache", Document.class);
        if (cache != null) {
          cacheMap.put("bytesInCache", cache.get("bytes currently in the cache"));
          cacheMap.put("dirtyBytes", cache.get("tracked dirty bytes in the cache"));
          cacheMap.put("maxBytes", cache.get("maximum bytes configured"));
        }
      }

      // DB Stats
      Map<String, Object> dbStatsMap = new HashMap<>();
      if (dbStats != null) {
        dbStatsMap.put("dataSize", dbStats.get("dataSize"));
        dbStatsMap.put("storageSize", dbStats.get("storageSize"));
        dbStatsMap.put("indexSize", dbStats.get("indexSize"));
        dbStatsMap.put("collections", dbStats.get("collections"));
        dbStatsMap.put("objects", dbStats.get("objects"));
      }

      // Collection Specific Stats
      Map<String, Object> colMap = new HashMap<>();
      try {
        long ordersCount = mongoTemplate.getCollection("petstore_orders").countDocuments();
        long productsCount = mongoTemplate.getCollection("petstore_products").countDocuments();
        colMap.put("petstore_orders_count", ordersCount);
        colMap.put("petstore_products_count", productsCount);
      } catch (Exception e) {
        log.warn("Unable to count collections: {}", e.getMessage());
      }

      // Replica set status
      String replName = "rs0";
      Document repl = serverStatus.get("repl", Document.class);
      boolean isPrimary = true;
      if (repl != null) {
        if (repl.getString("setName") != null) {
          replName = repl.getString("setName");
        }
        if (repl.getBoolean("ismaster") != null) {
          isPrimary = repl.getBoolean("ismaster");
        } else if (repl.getBoolean("isWritablePrimary") != null) {
          isPrimary = repl.getBoolean("isWritablePrimary");
        }
      }

      return new MongoDiagnosticsResponse(
          "ONLINE",
          version,
          uptime,
          replName,
          isPrimary,
          connMap,
          opMap,
          memMap,
          cacheMap,
          dbStatsMap,
          colMap,
          COMPASS_URI
      );
    } catch (Exception e) {
      log.error("Failed to query MongoDB diagnostics: {}", e.getMessage());
      return new MongoDiagnosticsResponse(
          "DEGRADED",
          "7.0.x",
          0L,
          "rs0",
          false,
          Map.of("error", e.getMessage()),
          Map.of(),
          Map.of(),
          Map.of(),
          Map.of(),
          Map.of(),
          COMPASS_URI
      );
    }
  }
}
