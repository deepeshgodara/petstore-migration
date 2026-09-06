# Welcome to the Java Pet Store Modernization Wiki

Welcome to the official technical wiki for the **Java Pet Store Enterprise Modernization Project** (`petstore-migration`).

This wiki is the comprehensive, centralized knowledge base for architects, engineers, DevOps specialists, and on-call site reliability engineers (SREs). It documents the end-to-end modernization of the iconic 2002 Sun Microsystems J2EE BluePrints application (v1.3.1_02) into a high-performance, cloud-native architecture powered by **Java 21 LTS**, **Spring Boot 3.3.x**, **MongoDB 7.0**, **Apache Kafka (KRaft)**, and **React 18**.

---

## 🧭 Navigation & Chapters

| Chapter | Description | Primary Audience |
| :--- | :--- | :--- |
| [**Architecture Overview**](Architecture-Overview) | Deep-dive into bounded contexts, the Dual-Write & Shadow Reconciliation pattern, and document modeling. | Architects, Lead Engineers |
| [**Modern High-Level Design (HLD)**](Modern-High-Level-Design) | Modern system architecture, Docker deployment topology, network boundaries, and Level 0/1 DFDs. | Architects, DevOps, SREs |
| [**Modern Low-Level Design (LLD)**](Modern-Low-Level-Design) | Microservice class diagrams, checkout/admin/supplier/chaos sequence diagrams, MongoDB BSON schemas, and flowcharts. | Developers, Architects |
| [**Legacy Architecture & Components**](Legacy-PetStore-Architecture-&-Components) | In-depth breakdown of the 2002 J2EE BluePrints application: the 4 .ear archives, WAF framework, EJB 2.0 components, 3NF schema, and order flow. | Architects, Maintainers, Engineers |
| [**Legacy High-Level Design (HLD)**](Legacy-High-Level-Design) | 2002 monolithic system architecture, enterprise packaging (.ear/.war/.jar), network boundaries, and baseline DFDs. | Architects, Engineers |
| [**Legacy Low-Level Design (LLD)**](Legacy-Low-Level-Design) | 2002 WAF MVC class hierarchy, CMP 2.0 entity beans, FastLane DAO, relational ER schemas, and state machines. | Architects, Engineers |
| [**Service Catalog**](Service-Catalog) | Detailed service specifications, runtime ports, configurations, REST endpoints, and Kafka topics. | Developers, Integrators |
| [**On-Call Support & Maintenance**](OnCall-Support-&-Maintenance) | Production runbooks: how to maintain services, restart daemons, handle outages, and recover from failures. | On-Call Engineers, SREs |
| [**Debugging & Troubleshooting Guide**](Debugging-&-Troubleshooting-Guide) | Triage procedures for data parity drift, Kafka lag, lock contention, and automated diagnostic scripts. | Support Engineers, Developers |
| [**Database & MongoDB Compass Guide**](Database-&-MongoDB-Compass-Guide) | MongoDB cluster topology, connection strings, Compass configuration, index management, and query presets. | DBA, Backend Engineers |

---

## ⚡ System At A Glance

```
+----------------------------------------------------------------------------------------------------+
|                                  PET STORE MODERN HYBRID TOPOLOGY                                  |
+----------------------------------------------------------------------------------------------------+
|  STOREFRONT & ADMIN CLIENTS:                                                                       |
|  - Customer Storefront (React 18 SPA + Vite + TypeScript)                 -> Port 3000 (/)         |
|  - Customer Account & Order History (Protected Route)                     -> Port 3000 (/account)  |
|  - Modern Admin Portal (Replaces Swing AdminApp.jar)                      -> Port 3000 (/admin)    |
|  - Modern Supplier Inventory Portal (Replaces supplier.ear JSP)           -> Port 3000 (/supplier) |
|  - Ops Telemetry & Parity Monitor                                         -> Port 3000 (/ops)      |
+----------------------------------------------------------------------------------------------------+
|  MICROSERVICES BACKEND (Spring Boot 3.3.x / Java 21 LTS / Project Loom Virtual Threads):           |
|  - petstore-catalog-service    -> Port 8081 (Multilingual catalog aggregates, inventory REST API)  |
|  - petstore-order-service      -> Port 8082 (Customer checkout, order lifecycle, dual-write)      |
|  - petstore-migration-service  -> Port 8085 (Historical ETL, shadow reconciler, DB diagnostics)   |
+----------------------------------------------------------------------------------------------------+
|  DATA & EVENT STREAMING INFRASTRUCTURE:                                                            |
|  - MongoDB Replica Set 'rs0'   -> Port 27017 (High-throughput document store: petstore)            |
|  - Apache Kafka Broker (KRaft) -> Port 9092 (Distributed event bus & dead-letter queue)            |
|  - Legacy 2002 J2EE Monolith   -> Port 8000 (/petstore - Apache TomEE + HSQLDB System of Record)  |
+----------------------------------------------------------------------------------------------------+
```

---

## 🎯 Modernization Mandates & Guarantees

1. **Zero Downtime**: Active transactions on the legacy system are never interrupted.
2. **Zero Data Loss**: The Dual-Write engine with Kafka-backed Dead-Letter Queue (DLQ) guarantees write acknowledgment and asynchronous retry isolation.
3. **100% Reversibility**: The legacy relational database remains hot as the System of Record (SoR) throughout all migration phases.
4. **Automated Parity Auditing**: Real-time shadow read comparator continuously validates live data across databases down to individual attributes.
5. **Non-Destructive Compliance**: Zero alterations to legacy 2002 source files, `build.xml`, or container deployment assets.

---

## 🚀 Quick Start for Engineers

```bash
# 1. Start core infrastructure (Legacy App, MongoDB rs0, Apache Kafka)
./docker/run_docker.sh

# 2. Run the master verification playback suite (all 4 test suites)
./scripts/run_all_verifications.sh

# 3. Connect to MongoDB using MongoDB Compass
./scripts/mongo_compass_connect.sh
```

For operational questions or on-call alerts, immediately consult the [On-Call Support & Maintenance](OnCall-Support-&-Maintenance) playbook.
