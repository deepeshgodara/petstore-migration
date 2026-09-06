# Database & MongoDB Compass Guide

This guide details the MongoDB persistence layer, replica set topology, and instructions for connecting and querying via **MongoDB Compass**.

---

## 1. Connecting MongoDB Compass

MongoDB Compass is the official graphical user interface (GUI) for MongoDB. It allows engineers to visually explore schemas, inspect real-time query performance, run aggregation pipelines, and manage indexes.

### 1.1 Connection String (1-Click Paste)
In the MongoDB Compass connection modal, paste the following connection URI into the **New Connection** field:

```text
mongodb://localhost:27017/petstore?replicaSet=rs0&directConnection=true
```

### 1.2 Manual Connection Parameters
If filling out the form manually:
- **Host**: `localhost`
- **Port**: `27017`
- **Authentication**: None (Default local development cluster)
- **Replica Set**: `rs0`
- **Default Database**: `petstore`
- **Direct Connection**: Enabled (`true`)

### 1.3 Quick Launch Script
From your local terminal, run the helper script:
```bash
./scripts/mongo_compass_connect.sh
```
If MongoDB Compass is installed in `/Applications` on macOS, this script will launch Compass and automatically pass the pre-configured connection URI.

---

## 2. Collection Schemas & Indexes

The `petstore` database contains two primary document collections:

### 2.1 Collection: `petstore_orders`
- **Document Aggregate Root**: Purchase orders with embedded customer snapshots, addresses, payments, and line items.
- **Indexes**:
  | Index Name | Keys | Type | Purpose |
  | :--- | :--- | :--- | :--- |
  | `_id_` | `{ _id: 1 }` | Unique | Primary key lookup by Order ID |
  | `userId_orderDate_idx` | `{ userId: 1, orderDate: -1 }` | Compound | Fast retrieval of a customer's order history |
  | `status_orderDate_idx` | `{ status: 1, orderDate: -1 }` | Compound | Powers Admin approval queue (`status: PENDING`) |

### 2.2 Collection: `petstore_products`
- **Document Aggregate Root**: Product catalog with embedded child `items` (inventory SKUs) and multilingual localization maps.
- **Indexes**:
  | Index Name | Keys | Type | Purpose |
  | :--- | :--- | :--- | :--- |
  | `_id_` | `{ _id: 1 }` | Unique | Primary key lookup by Product ID (e.g. `FI-SW-01`) |
  | `categoryId_idx` | `{ categoryId: 1 }` | Single Field | Storefront category filtering (`FISH`, `DOGS`, etc.) |
  | `items_itemId_idx` | `{ 'items.itemId': 1 }` | Multikey | Instant SKU lookup for checkout and supplier inventory |

---

## 3. Analytical Query Presets for MongoDB Compass

Open MongoDB Compass, select the `petstore` database, and use these pre-built queries in the **Filter** or **Aggregations** tabs:

### 3.1 Find All Pending Orders in Approval Queue
- **Collection**: `petstore_orders`
- **Filter**:
  ```json
  { "status": "PENDING" }
  ```
- **Sort**:
  ```json
  { "orderDate": -1 }
  ```

### 3.2 Aggregate Sales Revenue by Pet Category
- **Collection**: `petstore_orders`
- **Aggregation Pipeline**:
  ```json
  [
    { "$unwind": "$lineItems" },
    {
      "$group": {
        "_id": "$lineItems.categoryId",
        "totalRevenue": { "$sum": "$lineItems.totalCost" },
        "unitsSold": { "$sum": "$lineItems.quantity" },
        "orderCount": { "$addToSet": "$_id" }
      }
    },
    {
      "$project": {
        "category": "$_id",
        "totalRevenue": 1,
        "unitsSold": 1,
        "uniqueOrders": { "$size": "$orderCount" },
        "_id": 0
      }
    },
    { "$sort": { "totalRevenue": -1 } }
  ]
  ```

### 3.3 Find Low Stock Inventory SKUs (< 50 units)
- **Collection**: `petstore_products`
- **Aggregation Pipeline**:
  ```json
  [
    { "$unwind": "$items" },
    { "$match": { "items.inventoryQuantity": { "$lt": 50 } } },
    {
      "$project": {
        "itemId": "$items.itemId",
        "productName": "$name",
        "stock": "$items.inventoryQuantity",
        "attribute": "$items.attribute"
      }
    }
  ]
  ```

### 3.4 Query Top Customers by Total Spend
- **Collection**: `petstore_orders`
- **Aggregation Pipeline**:
  ```json
  [
    { "$match": { "status": { "$in": ["APPROVED", "COMPLETED"] } } },
    {
      "$group": {
        "_id": "$userId",
        "orderCount": { "$sum": 1 },
        "totalSpent": { "$sum": { "$toDecimal": "$totalPrice" } }
      }
    },
    { "$sort": { "totalSpent": -1 } },
    { "$limit": 10 }
  ]
  ```
