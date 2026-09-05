package com.petstore.migration.reconciliation;

import java.io.Serializable;

/**
 * Encapsulates field-level discrepancy information identified during a shadow read comparison.
 *
 * @param field name of the attribute with discrepancy
 * @param expectedRelationalValue value from legacy relational System of Record
 * @param actualMongoValue value from target MongoDB document store
 * @param discrepancyType category of drift (e.g., STATUS_MISMATCH, PRICE_DRIFT, MISSING_RECORD)
 * @param description human-readable diagnostic message
 */
public record DiscrepancyDetail(
    String field,
    String expectedRelationalValue,
    String actualMongoValue,
    String discrepancyType,
    String description
) implements Serializable {}
