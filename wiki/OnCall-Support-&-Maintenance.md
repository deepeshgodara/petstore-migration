# On-Call Support & Maintenance Runbook

This playbook provides actionable, step-by-step operational instructions for on-call engineers and SREs responsible for maintaining the Pet Store modern ecosystem.

---

## 1. Service Failure & Restart Procedures

### 1.1 `petstore-catalog-service` (Port 8081) Down
- **Symptoms**: Storefront category/product browsing fails with HTTP 502/504 or network timeout; supplier inventory updates fail.
- **Diagnostic Command**:
  ```bash
  curl -i http://localhost:8081/actuator/health
  lsof -i :8081
  ```
- **Restart Procedure**:
  ```bash
  # Option A: Container / Cloud Deployment (Docker Compose / Kubernetes)
  docker compose -f docker/docker-compose.yml restart catalog-service

  # Option B: Local CLI Execution (from repository root)
  kill -9 $(lsof -t -i:8081) 2>/dev/null || true
  cd petstore-modern && mvn -pl petstore-catalog-service spring-boot:run
  ```
- **Verification**:
  ```bash
  curl -s http://localhost:8081/api/v1/categories | grep -o "FISH"
  ```

---

### 1.2 `petstore-order-service` (Port 8082) Down
- **Symptoms**: Customer checkout modal fails with error; Admin orders queue returns error; `/api/v1/orders` unreachable.
- **Diagnostic Command**:
  ```bash
  curl -i http://localhost:8082/actuator/health
  lsof -i :8082
  ```
- **Restart Procedure**:
  ```bash
  # Option A: Container / Cloud Deployment (Docker Compose / Kubernetes)
  docker compose -f docker/docker-compose.yml restart order-service

  # Option B: Local CLI Execution (from repository root)
  kill -9 $(lsof -t -i:8082) 2>/dev/null || true
  cd petstore-modern && mvn -pl petstore-order-service spring-boot:run
  ```
- **Verification**:
  ```bash
  curl -s http://localhost:8082/api/v1/orders/admin/summary
  ```

---

### 1.3 `petstore-migration-service` (Port 8085) Down
- **Symptoms**: `/ops` dashboard fails to refresh parity metrics; dual-write events are not consumed to the legacy database.
- **Diagnostic Command**:
  ```bash
  curl -i http://localhost:8085/actuator/health
  lsof -i :8085
  ```
- **Restart Procedure**:
  ```bash
  # Option A: Container / Cloud Deployment (Docker Compose / Kubernetes)
  docker compose -f docker/docker-compose.yml restart migration-service

  # Option B: Local CLI Execution (from repository root)
  kill -9 $(lsof -t -i:8085) 2>/dev/null || true
  cd petstore-modern && mvn -pl petstore-migration-service spring-boot:run
  ```
- **Verification**:
  ```bash
  curl -s "http://localhost:8085/api/v1/migration/parity?runAudit=false"
  ```

---

### 1.4 `petstore-frontend` (Port 3000) Down
- **Symptoms**: Browser cannot connect to `http://localhost:3000`.
- **Restart Procedure**:
  ```bash
  # Option A: Container / Cloud Deployment (Docker Compose / Kubernetes)
  docker compose -f docker/docker-compose.yml restart frontend

  # Option B: Local CLI Execution (from repository root)
  kill -9 $(lsof -t -i:3000) 2>/dev/null || true
  cd petstore-frontend && npm run dev
  ```

---

## 2. Infrastructure Outages & Recovery

### 2.1 MongoDB Replica Set (`rs0`) Outage
- **Container**: `petstore-mongo` (Port 27017)
- **Check Status**:
  ```bash
  docker ps -a | grep petstore-mongo
  docker exec petstore-mongo mongosh --quiet --eval "rs.status().ok"
  ```
- **Recovery If Container Stopped / Paused**:
  ```bash
  docker unpause petstore-mongo 2>/dev/null || docker start petstore-mongo
  # Wait 3 seconds for replica set election
  sleep 3
  docker exec petstore-mongo mongosh --quiet --eval "db.ping()"
  ```
- **Zero Blast Radius Verification**:
  Note that under a MongoDB outage, the legacy TomEE store remains hot, and customer writes queue to Kafka without blocking checkout.

---

### 2.2 Apache Kafka Broker Outage
- **Container**: `petstore-kafka` (Port 9092)
- **Check Broker Health**:
  ```bash
  docker exec petstore-kafka /usr/bin/kafka-topics --bootstrap-server localhost:9092 --list
  ```
- **Recovery Procedure**:
  ```bash
  docker restart petstore-kafka
  # Verify topics exist
  docker exec petstore-kafka /usr/bin/kafka-topics --bootstrap-server localhost:9092 \
    --create --if-not-exists --topic petstore.orders.dualwrite --partitions 3 --replication-factor 1
  ```

---

### 2.3 Legacy J2EE Monolith Container (`petstore-baseline`) Down
- **Container**: `petstore-baseline` (Port 8000 & Port 9001)
- **Check Health**:
  ```bash
  curl -s -o /dev/null -w "%{http_code}" http://localhost:8000/petstore/
  ```
- **Restart Procedure**:
  ```bash
  docker restart petstore-baseline
  # Wait 10 seconds for TomEE and HSQLDB to bind
  sleep 10
  ./scripts/query_legacy_db.sh
  ```

---

## 3. Dead-Letter Queue (DLQ) Triage & Message Replay

When transient database or network failures occur during dual-write propagation, events route to `petstore.orders.dlq`.

### 3.1 Inspecting DLQ Messages
```bash
docker exec petstore-kafka /usr/bin/kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic petstore.orders.dlq \
  --from-beginning \
  --timeout-ms 5000
```

### 3.2 Triggering Shadow Reconciliation Post-Recovery
After resolving the downstream issue, trigger an automated audit from the terminal:
```bash
curl -s -X GET "http://localhost:8085/api/v1/migration/parity?runAudit=true"
```
The reconciler will scan both datastores, detect any unwritten entities, and perform idempotent upserts to restore 100.0% parity.

---

## 4. Incident Severity and Escalation Matrix

| Severity | Definition | Target Resolution | Action Plan |
| :--- | :--- | :--- | :--- |
| **P0 (Critical)** | Both Modern & Legacy order placement endpoints failing. Customer checkout halted. | < 15 mins | 1. Check Kafka and Order Service.<br/>2. If modern is unrecoverable, route 100% storefront traffic to legacy TomEE (:8000). |
| **P1 (High)** | Modern store healthy, but dual-write consumer failing. Parity lag growing. | < 45 mins | 1. Check `petstore-migration-service` logs.<br/>2. Verify HSQLDB container is accepting JDBC connections.<br/>3. Replay DLQ messages. |
| **P2 (Medium)** | Admin Portal or Supplier Portal UI degraded; checkout unaffected. | < 2 hours | 1. Check Catalog Service (:8081).<br/>2. Rebuild frontend bundle if necessary. |
| **P3 (Low)** | Minor cosmetic or non-critical telemetry discrepancy. | Next Business Day | File Jira ticket, monitor OpenTelemetry logs. |
