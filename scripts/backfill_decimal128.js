// MongoDB Backfill Script: Convert string/float money fields to native BSON Decimal128 (IEEE 754-2008)
// Run with: mongosh petstore scripts/backfill_decimal128.js

print("Starting Decimal128 migration backfill for petstore...");

// 1. Backfill petstore_orders
let orderCount = 0;
db.petstore_orders.find().forEach(order => {
  let modified = false;
  let updates = {};

  if (order.totalPrice !== undefined && !(order.totalPrice instanceof NumberDecimal)) {
    updates.totalPrice = NumberDecimal(order.totalPrice.toString());
    modified = true;
  }

  if (order.lineItems && Array.isArray(order.lineItems)) {
    let updatedItems = order.lineItems.map(item => {
      let itemCopy = { ...item };
      if (itemCopy.unitPrice !== undefined && !(itemCopy.unitPrice instanceof NumberDecimal)) {
        itemCopy.unitPrice = NumberDecimal(itemCopy.unitPrice.toString());
        modified = true;
      }
      if (itemCopy.totalCost !== undefined && !(itemCopy.totalCost instanceof NumberDecimal)) {
        itemCopy.totalCost = NumberDecimal(itemCopy.totalCost.toString());
        modified = true;
      }
      return itemCopy;
    });
    if (modified) {
      updates.lineItems = updatedItems;
    }
  }

  if (modified) {
    db.petstore_orders.updateOne({ _id: order._id }, { $set: updates });
    orderCount++;
  }
});
print(`Updated ${orderCount} orders to native Decimal128.`);

// 2. Backfill petstore_products
let productCount = 0;
db.petstore_products.find().forEach(prod => {
  let modified = false;
  if (prod.items && Array.isArray(prod.items)) {
    let updatedItems = prod.items.map(item => {
      let itemCopy = { ...item };
      if (itemCopy.listPrice !== undefined && !(itemCopy.listPrice instanceof NumberDecimal)) {
        itemCopy.listPrice = NumberDecimal(itemCopy.listPrice.toString());
        modified = true;
      }
      if (itemCopy.unitCost !== undefined && !(itemCopy.unitCost instanceof NumberDecimal)) {
        itemCopy.unitCost = NumberDecimal(itemCopy.unitCost.toString());
        modified = true;
      }
      return itemCopy;
    });
    if (modified) {
      db.petstore_products.updateOne({ _id: prod._id }, { $set: { items: updatedItems } });
      productCount++;
    }
  }
});
print(`Updated ${productCount} products with Decimal128 pricing.`);

print("Decimal128 backfill completed successfully.");
