# Baseline Design: Java Pet Store 1.3.1_02 (2002 Reference Architecture)

> [!NOTE]
> **Purpose of this Documentation**: This directory contains comprehensive **High-Level Design (HLD)** and **Low-Level Design (LLD)** specifications representing the authentic baseline architecture of the **Java™ Pet Store Demo 1.3.1_02 (2002)**. It serves as the official technical reference for understanding how the legacy multi-tier J2EE system functions before executing modernization into **Spring Boot 3.x + MongoDB + Apache Kafka**.

---

## 📚 Document Index

### 1. High-Level Design (HLD)
HLD documents illustrate the overall system topology, subsystem communication, data pathways, and physical/virtual deployment topologies.

| Document | Key Diagrams & Content Covered |
| :--- | :--- |
| **[System Architecture](file:///Users/deepeshgodara/Documents/petstore1.3.1_02/baseline_design/HLD/system_architecture.md)** | Multi-tier J2EE Blueprint architecture, Web Application Framework (WAF), EJB Session/Entity tiers, Asynchronous Order Processing Center (OPC), Supplier Subsystem, and Cloudscape RDBMS. |
| **[Data Flow Diagrams (DFD)](file:///Users/deepeshgodara/Documents/petstore1.3.1_02/baseline_design/HLD/data_flow_diagrams.md)** | Level 0 Context DFD, Level 1 Subsystem DFD, and Level 2 Order & JMS Async Event Processing DFD. |
| **[Deployment Diagram](file:///Users/deepeshgodara/Documents/petstore1.3.1_02/baseline_design/HLD/deployment_diagram.md)** | Enterprise Application Archives (`petstore.ear`, `opc.ear`, `supplier.ear`, `petstoreadmin.ear`), container execution boundaries, JMS queues/topics, and JDBC data sources. |
| **[Network Topology](file:///Users/deepeshgodara/Documents/petstore1.3.1_02/baseline_design/HLD/network_topology.md)** | Network boundaries, HTTP/HTTPS web listeners (8080), JNDI/RMI-IIOP enterprise bus, JMS message broker (61616), and JDBC database listeners. |

---

### 2. Low-Level Design (LLD)
LLD documents detail the internal mechanics, object structures, database schemas, and algorithms.

| Document | Key Diagrams & Content Covered |
| :--- | :--- |
| **[Class Diagrams](file:///Users/deepeshgodara/Documents/petstore1.3.1_02/baseline_design/LLD/class_diagrams.md)** | WAF Web Controller tier, Session Facade & State tier, FastLane DAO pattern, and EJB 2.0 CMP Entity Bean relationships. |
| **[Sequence Diagrams](file:///Users/deepeshgodara/Documents/petstore1.3.1_02/baseline_design/LLD/sequence_diagrams.md)** | End-to-end lifecycles: FastLane Catalog Browsing, Shopping Cart Session manipulation, User SignOn & Authentication, and Asynchronous Order Fulfillment via JMS/MDBs. |
| **[Entity-Relationship (ER) Diagrams](file:///Users/deepeshgodara/Documents/petstore1.3.1_02/baseline_design/LLD/entity_relationship_diagrams.md)** | Physical schemas for `PetStoreDB`, `OPCDB`, and `SupplierDB`, table definitions, primary/foreign keys, and column constraints. |
| **[State Machine Diagrams](file:///Users/deepeshgodara/Documents/petstore1.3.1_02/baseline_design/LLD/state_machine_diagrams.md)** | State transitions for Purchase Orders (`Pending` -> `Approved` -> `Processing` -> `Completed`), Stateful Session Beans (SFSB Cart), and User Authentication sessions. |
| **[Flowcharts and Pseudocode](file:///Users/deepeshgodara/Documents/petstore1.3.1_02/baseline_design/LLD/flowcharts_and_pseudocode.md)** | WAF Front Controller request pipeline algorithm, FastLane vs EJB decision routing, and Block Allocation Unique ID generation algorithm. |

---

### 3. Standalone Runner Architecture
| Document | Content Covered |
| :--- | :--- |
| **[Runner Architecture & Workflow](file:///Users/deepeshgodara/Documents/petstore1.3.1_02/runner/README.md)** | Design and implementation of the lightweight embedded Java 21 runner, emulating J2EE WAF MVC, session state tracking, in-memory database models, and view rendering. |

---

## 🏛️ Executive Summary of Baseline Architecture

The 2002 Java Pet Store application was created by Sun Microsystems as the definitive reference blueprint for enterprise Java applications (**J2EE 1.3**). It demonstrates key architectural patterns:
1. **Model-View-Controller (MVC)** via the custom **Web Application Framework (WAF)**.
2. **Fast Lane Reader Pattern**: Bypassing heavy EJB entity layers for high-throughput read-only catalog queries using direct JDBC.
3. **Session Facade Pattern**: Wrapping fine-grained business logic into Stateful/Stateless Session Beans (`ShoppingClientFacadeLocalEJB`, `ShoppingControllerLocalEJB`).
4. **Asynchronous Messaging**: Decoupling order intake from payment processing and supplier inventory restocking using **JMS Queues & Topics** with **Message-Driven Beans (MDBs)**.
5. **Container-Managed Persistence (CMP 2.0)**: Abstracting relational persistence into entity bean descriptors (`UserEJB`, `CustomerEJB`, `AccountEJB`, `AddressEJB`, `CreditCardEJB`).
