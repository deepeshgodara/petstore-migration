package com.petstore.migration.model;

import java.io.Serializable;
import java.util.Map;

/**
 * Real-time MongoDB telemetry and performance diagnostics response.
 *
 * @param status database status ("ONLINE", "DEGRADED", "OFFLINE")
 * @param version MongoDB engine version (e.g., "7.0.40")
 * @param uptimeSeconds database engine uptime in seconds
 * @param replicaSetName replica set name ("rs0")
 * @param isPrimary whether current node is replica set primary
 * @param connections connection metrics (current, available, active, totalCreated)
 * @param opcounters operational counter totals (insert, query, update, delete, command)
 * @param memory memory utilization (residentMb, virtualMb)
 * @param wiredTigerCache WiredTiger cache metrics (bytesInCache, dirtyBytes, maxBytes)
 * @param databaseStats general database stats (dataSize, storageSize, indexSize, collections, objects)
 * @param collectionStats collection document counts and storage bytes
 * @param compassConnectionUri pre-configured connection URI for MongoDB Compass
 */
public record MongoDiagnosticsResponse(
    String status,
    String version,
    long uptimeSeconds,
    String replicaSetName,
    boolean isPrimary,
    Map<String, Object> connections,
    Map<String, Object> opcounters,
    Map<String, Object> memory,
    Map<String, Object> wiredTigerCache,
    Map<String, Object> databaseStats,
    Map<String, Object> collectionStats,
    String compassConnectionUri
) implements Serializable {}
