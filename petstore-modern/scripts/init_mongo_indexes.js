// MongoDB Compound Index Initialization Script for Pet Store Modern
const dbName = 'petstore';
const targetDb = db.getSiblingDB(dbName);

print('Creating indexes for ' + dbName + '...');

// 1. petstore_orders indexes
print('Authoring compound indexes on petstore_orders...');
targetDb.petstore_orders.createIndex(
  { userId: 1, orderDate: -1 },
  { name: 'userId_orderDate_idx', background: true }
);

targetDb.petstore_orders.createIndex(
  { status: 1, orderDate: -1 },
  { name: 'status_orderDate_idx', background: true }
);

targetDb.petstore_orders.createIndex(
  { orderDate: -1 },
  { name: 'orderDate_idx', background: true }
);

// 2. petstore_products indexes
print('Authoring compound indexes on petstore_products...');
targetDb.petstore_products.createIndex(
  { categoryId: 1 },
  { name: 'categoryId_idx', background: true }
);

targetDb.petstore_products.createIndex(
  { 'items.itemId': 1 },
  { name: 'items_itemId_idx', background: true }
);

// 3. petstore_categories indexes
print('Authoring indexes on petstore_categories...');
targetDb.petstore_categories.createIndex(
  { _id: 1 },
  { name: 'primaryKey_idx' }
);

print('Indexes successfully created:');
print('petstore_orders indexes:');
printjson(targetDb.petstore_orders.getIndexes());

print('petstore_products indexes:');
printjson(targetDb.petstore_products.getIndexes());
