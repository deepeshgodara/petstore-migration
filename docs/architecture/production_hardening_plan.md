# Production Hardening & Architecture Remediation Plan (Addressing Slides 20 & 25)

This document addresses the 12 technical gaps, MongoDB gotchas, concurrency risks, and security issues identified in the post-mortem analysis (Slides 20 & 25).

---

## Technical Assessment: Is Claude's Reasoning Good Enough?

**Verdict: Claude's reasoning is extraordinarily sharp, technically sound, and 100% verified against our running codebase.**

Our live verification against the active MongoDB instance and Spring Boot services confirmed every single assertion made in Slides 20 and 25:
1. **BSON String Money Typing**: Running `db.petstore_orders.findOne()` confirmed `totalPrice: '125.00000000000000000000000000000000'` is stored as a BSON `string`, preventing native index-backed range filters and numeric aggregations without `$toDecimal`.
2. **Index Ghosting (`auto-index-creation: false`)**: Running `getIndexes()` on `petstore_orders`, `petstore_users`, and `petstore_products` revealed that **only the default `_id_` index exists**. All `@CompoundIndex` and `@Indexed` annotations are completely ignored by Spring Boot at runtime.
3. **Critical Password Bypass**: In [UserService.java](file:///Users/deepeshgodara/Documents/petstore1.3.1_02/petstore-modern/petstore-order-service/src/main/java/com/petstore/order/service/UserService.java#L115), the password validation check is wrapped in `if (req.password() != null && !req.password().isBlank())`, meaning submitting an empty password completely bypasses authentication!
4. **Plaintext Password Storage**: Passwords were replicated as plaintext from the 2002 legacy database into modern MongoDB with zero cryptographic hashing.
5. **Missing Outbox & Transactions**: [OrderService.java](file:///Users/deepeshgodara/Documents/petstore1.3.1_02/petstore-modern/petstore-order-service/src/main/java/com/petstore/order/service/OrderService.java#L150) performs `orderRepository.save(order)` followed by an uncoordinated fire-and-forget Kafka publish. A broker outage causes silent event loss.
6. **Unwired Kill Switch**: The producer `DualWritePublisher` checks `${migration.dualwrite.enabled:true}`, but `petstore-order-service` never declares this in `application.yml` nor does it expose Spring Actuator refresh endpoints to toggle it.

---

## Remediation Phases

### Phase 1: Critical Security & Authentication Hardening
1. **Fix Password Bypass**: Require strict non-blank passwords during login in [UserService.java](file:///Users/deepeshgodara/Documents/petstore1.3.1_02/petstore-modern/petstore-order-service/src/main/java/com/petstore/order/service/UserService.java).
2. **BCrypt Hashing**: Integrate `BCryptPasswordEncoder` with lazy upgrade upon successful login.
3. **Spring Security**: Add JWT / Security filter chain to protect `/api/v1/admin/**`.

### Phase 2: MongoDB Typing & Indexing Architecture
1. **MongoCustomConversions**: Register `BigDecimal` <-> `org.bson.types.Decimal128` converters so prices are stored as true numeric decimals.
2. **DatabaseIndexInitializer**: Create an `ApplicationReadyEvent` listener to programmatically create indexes (`userId_orderDate_idx`, `status_orderDate_idx`, unique `email`, compound text index for multilingual search).
3. **Backfill Script**: Convert existing string prices to `NumberDecimal` in MongoDB.

### Phase 3: Concurrency Control & Multi-Document Transactions
1. **Optimistic Locking**: Add `@Version private Long version;` to [OrderDocument.java](file:///Users/deepeshgodara/Documents/petstore1.3.1_02/petstore-modern/petstore-common/src/main/java/com/petstore/order/document/OrderDocument.java).
2. **MongoDB Transactions**: Configure `MongoTransactionManager` on the `rs0` replica set and annotate mutating service methods with `@Transactional`.

### Phase 4: Reliability — Transactional Outbox & Resilient Messaging
1. **Transactional Outbox**: Persist `OutboxDocument` inside the same MongoDB transaction as `OrderDocument`.
2. **Outbox Relay**: Reliable poller / change-stream dispatcher streaming to Kafka with retries.
3. **Kill Switch & Actuator**: Declare `migration.dualwrite.enabled` in `petstore-order-service`'s `application.yml` with dynamic refresh.

### Phase 5: Cutover Safety, Reconciler Optimization & Deployment
1. **O(1) Hash Map Reconciliation**: Replace nested $O(n^2)$ scans in [ShadowReadComparator.java](file:///Users/deepeshgodara/Documents/petstore1.3.1_02/petstore-modern/petstore-migration-service/src/main/java/com/petstore/migration/reconciliation/ShadowReadComparator.java).
2. **Legacy Write-Back Adapter**: Kafka consumer syncing modern orders back into legacy SQL tables for non-destructive emergency rollback.
