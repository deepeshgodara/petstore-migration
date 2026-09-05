package com.petstore.migration.reconciliation;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of a shadow read parity audit comparing legacy relational entity state
 * against target MongoDB document state.
 */
public class ComparisonResult implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String entityType;
  private final String entityId;
  private final boolean match;
  private final List<DiscrepancyDetail> discrepancies;
  private final long durationNanos;
  private final Instant timestamp;

  public ComparisonResult(
      String entityType,
      String entityId,
      boolean match,
      List<DiscrepancyDetail> discrepancies,
      long durationNanos) {
    this.entityType = entityType;
    this.entityId = entityId;
    this.match = match;
    this.discrepancies = discrepancies != null
        ? Collections.unmodifiableList(new ArrayList<>(discrepancies))
        : Collections.emptyList();
    this.durationNanos = durationNanos;
    this.timestamp = Instant.now();
  }

  /**
   * Factory method for creating a 100% matched comparison result.
   */
  public static ComparisonResult match(String entityType, String entityId, long durationNanos) {
    return new ComparisonResult(entityType, entityId, true, Collections.emptyList(), durationNanos);
  }

  /**
   * Factory method for creating a divergent comparison result with identified discrepancies.
   */
  public static ComparisonResult drift(
      String entityType,
      String entityId,
      List<DiscrepancyDetail> discrepancies,
      long durationNanos) {
    return new ComparisonResult(entityType, entityId, false, discrepancies, durationNanos);
  }

  public String getEntityType() {
    return entityType;
  }

  public String getEntityId() {
    return entityId;
  }

  public boolean isMatch() {
    return match;
  }

  public List<DiscrepancyDetail> getDiscrepancies() {
    return discrepancies;
  }

  public long getDurationNanos() {
    return durationNanos;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return "ComparisonResult{"
        + "entityType='" + entityType + '\''
        + ", entityId='" + entityId + '\''
        + ", match=" + match
        + ", discrepanciesCount=" + discrepancies.size()
        + ", durationNanos=" + durationNanos
        + ", timestamp=" + timestamp
        + '}';
  }
}
