# Debugging & Troubleshooting Guide

This guide provides structured triage workflows, root cause diagnostic steps, and log analysis patterns for resolving issues across the Pet Store architecture.

---

## 1. Resolving Data Parity Drift

### 1.1 What Causes Drift?
- **Transient In-Flight Lag**: An order was placed in MongoDB 100ms ago, but Kafka consumption to the legacy HSQLDB has not yet committed.
- **Service Outage**: `petstore-migration-service` was stopped while orders were being created in `petstore-order-service`.
- **Database Lock**: HSQLDB locked on a write query during high-concurrency tests.

### 1.2 Diagnostic Procedure
1. Query the parity dashboard:
   ```bash
   curl -s "http://localhost:8085/api/v1/migration/parity?runAudit=false" | jq .
   ```
2. Check `unreconciledDrifts` array in the response:
   - Identifies the exact `entityType` (`ORDER`, `PRODUCT`, `ITEM`), `entityId`, and field discrepancy.
3. If drift is confirmed:
   Trigger an immediate reconciliation audit:
   ```bash
   curl -s "http://localhost:8085/api/v1/migration/parity?runAudit=true" | jq .
   ```
4. Verify parity percentage returns to `100.0%`.

---

## 2. Verification & Diagnostic Scripts

The repository includes ready-to-run diagnostic scripts in `scripts/`:

| Script | Purpose | When to Run |
| :--- | :--- | :--- |
| [`scripts/run_all_verifications.sh`](file:///Users/deepeshgodara/Documents/petstore1.3.1_02/scripts/run_all_verifications.sh) | Master test runner executing all 4 playback suites | Pre-deployment, CI/CD, post-incident validation |
| [`scripts/verify_e2e_checkout.sh`](file:///Users/deepeshgodara/Documents/petstore1.3.1_02/scripts/verify_e2e_checkout.sh) | Validates POST /api/v1/orders, MongoDB persistence, and parity | Triage checkout issues |
| [`scripts/verify_admin_approval.sh`](file:///Users/deepeshgodara/Documents/petstore1.3.1_02/scripts/verify_admin_approval.sh) | Tests PUT /orders/{id}/status, Kafka events, and KPI updates | Triage Admin approval queue |
| [`scripts/verify_supplier_inventory.sh`](file:///Users/deepeshgodara/Documents/petstore1.3.1_02/scripts/verify_supplier_inventory.sh) | Tests GET /items, PUT stock updates, and storefront reflection | Triage Supplier inventory |
| [`scripts/chaos_mongo_failure_test.sh`](file:///Users/deepeshgodara/Documents/petstore1.3.1_02/scripts/chaos_mongo_failure_test.sh) | Simulates MongoDB pause, verifies legacy uptime & DLQ recovery | Chaos engineering, resilience audits |
| [`scripts/query_legacy_db.sh`](file:///Users/deepeshgodara/Documents/petstore1.3.1_02/scripts/query_legacy_db.sh) | Direct JDBC query against legacy HSQLDB in container | Verify legacy table contents |
| [`scripts/mongo_compass_connect.sh`](file:///Users/deepeshgodara/Documents/petstore1.3.1_02/scripts/mongo_compass_connect.sh) | Displays Compass connection parameters and launches GUI | Database administration |

---

## 3. Port Conflicts and Process Triage

If a service fails to start with `Address already in use (Bind failed)`:

### 3.1 Check Process by Port
```bash
# Example for port 8082:
lsof -i :8082
```

### 3.2 Standard Port Map
- `3000`: Vite Frontend Server (`node`)
- `8000`: Legacy Apache TomEE container (`petstore-baseline`)
- `8081`: Modern Catalog Service (`petstore-catalog-service`)
- `8082`: Modern Order Service (`petstore-order-service`)
- `8085`: Modern Migration & Ops Service (`petstore-migration-service`)
- `9001`: Legacy HSQLDB container port (`petstore-baseline`)
- `9092`: Apache Kafka Broker (`petstore-kafka`)
- `27017`: MongoDB Replica Set (`petstore-mongo`)

### 3.3 Fast Clean-Kill One-Liner
```bash
kill -9 $(lsof -t -i:8081 -i:8082 -i:8085) 2>/dev/null || true
```

---

## 4. Kafka Broker & Message Flow Triage

### 4.1 Check Kafka Topics
```bash
docker exec petstore-kafka /usr/bin/kafka-topics --bootstrap-server localhost:9092 --list
```

### 4.2 Stream Live Events
```bash
# Watch order creation events:
docker exec petstore-kafka /usr/bin/kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic petstore.orders.created \
  --from-beginning

# Watch approval state transitions:
docker exec petstore-kafka /usr/bin/kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic petstore.orders.approved \
  --from-beginning
```

---

## 5. Direct Database Inspection

### 5.1 MongoDB mongosh Inspection
```bash
# Check orders in MongoDB:
docker exec petstore-mongo mongosh --quiet --eval "db.getSiblingDB('petstore').petstore_orders.countDocuments()"

# Query specific order by ID:
docker exec petstore-mongo mongosh --quiet --eval "JSON.stringify(db.getSiblingDB('petstore').petstore_orders.findOne({_id: '100115'}))"
```

### 5.2 Legacy Relational Database Inspection
```bash
# Run Java JDBC query against HSQLDB:
./scripts/query_legacy_db.sh
```
