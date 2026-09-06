#!/usr/bin/env bash
# ==============================================================================
# Script: verify_supplier_inventory.sh
# Purpose: Verifies the modernized Supplier & Inventory Management workflow:
#          1. Checks health of Catalog Service and Vite reverse proxy
#          2. Fetches full inventory catalog (28 SKUs) via GET /api/v1/items
#          3. Simulates Supplier updating stock via PUT /api/v1/items/{itemId}/inventory
#          4. Validates real-time persistence inside MongoDB replica set 'rs0'
#          5. Confirms immediate REST reflection on customer-facing item endpoints
# ==============================================================================

set -euo pipefail

# ANSI Color formatting
GREEN='\033[0;32m'
RED='\033[0;31m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${BOLD}${CYAN}===================================================================${NC}"
echo -e "${BOLD}${CYAN}  PET STORE MODERNIZATION: SUPPLIER INVENTORY VERIFICATION         ${NC}"
echo -e "${BOLD}${CYAN}===================================================================${NC}"

VITE_URL="http://localhost:3000"
CATALOG_SERVICE_URL="http://localhost:8081"

# Step 1: Health & Service Availability
echo -e "\n${BOLD}[Step 1/5] Verifying catalog microservice and frontend proxy...${NC}"

if ! curl -s -f "${CATALOG_SERVICE_URL}/api/v1/categories" > /dev/null; then
  echo -e "${RED}[FAIL] Catalog Service at ${CATALOG_SERVICE_URL} is unreachable!${NC}"
  exit 1
fi
echo -e "  ${GREEN}✓${NC} Catalog Service is healthy on port 8081"

if ! curl -s -f "${VITE_URL}/" > /dev/null; then
  echo -e "${RED}[FAIL] Vite dev server at ${VITE_URL} is unreachable!${NC}"
  exit 1
fi
echo -e "  ${GREEN}✓${NC} Vite Frontend server is healthy on port 3000"

# Step 2: Fetch Full Inventory via Supplier API
echo -e "\n${BOLD}[Step 2/5] Fetching complete inventory catalog via GET /api/v1/items...${NC}"

ITEMS_RESPONSE=$(curl -s "${VITE_URL}/api/v1/items")
TOTAL_ITEMS=$(echo "${ITEMS_RESPONSE}" | grep -o '"itemId"' | wc -l | tr -d ' ')

if [ "${TOTAL_ITEMS}" -lt 28 ]; then
  echo -e "${RED}[FAIL] Expected at least 28 inventory items, but received: ${TOTAL_ITEMS}${NC}"
  exit 1
fi
echo -e "  ${GREEN}✓${NC} Successfully loaded ${BOLD}${CYAN}${TOTAL_ITEMS} catalog SKUs${NC} across all 5 pet categories"

# Step 3: Record Baseline Stock for Target Item (EST-1 Angelfish)
echo -e "\n${BOLD}[Step 3/5] Querying baseline stock for Item EST-1 (Large Angelfish)...${NC}"
TARGET_ITEM=$(curl -s "${VITE_URL}/api/v1/items/EST-1")
INITIAL_QTY=$(echo "${TARGET_ITEM}" | grep -o '"inventoryQuantity":[0-9]*' | cut -d':' -f2)
ITEM_NAME=$(echo "${TARGET_ITEM}" | grep -o '"productName":"[^"]*' | cut -d'"' -f4)

echo -e "  ${GREEN}✓${NC} Target Item SKU: ${BOLD}EST-1${NC} (${ITEM_NAME})"
echo -e "      Current Warehouse Stock: ${YELLOW}${INITIAL_QTY} units${NC}"

# Calculate new stock quantity (+50 adjustment)
NEW_QTY=$((INITIAL_QTY + 50))

# Step 4: Execute Stock Update via Supplier API
echo -e "\n${BOLD}[Step 4/5] Simulating Supplier updating stock to ${NEW_QTY} units (PUT /api/v1/items/EST-1/inventory)...${NC}"

UPDATE_PAYLOAD="{\"quantity\": ${NEW_QTY}}"
UPDATE_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X PUT "${VITE_URL}/api/v1/items/EST-1/inventory" \
  -H "Content-Type: application/json" \
  -d "${UPDATE_PAYLOAD}")

HTTP_STATUS=$(echo "${UPDATE_RESPONSE}" | grep "HTTP_STATUS:" | cut -d':' -f2)
UPDATE_BODY=$(echo "${UPDATE_RESPONSE}" | sed '/HTTP_STATUS:/d')

if [ "${HTTP_STATUS}" != "200" ]; then
  echo -e "${RED}[FAIL] Inventory update failed with HTTP ${HTTP_STATUS}: ${UPDATE_BODY}${NC}"
  exit 1
fi

PERSISTED_QTY=$(echo "${UPDATE_BODY}" | grep -o '"inventoryQuantity":[0-9]*' | cut -d':' -f2)

if [ "${PERSISTED_QTY}" -ne "${NEW_QTY}" ]; then
  echo -e "${RED}[FAIL] Expected updated quantity ${NEW_QTY}, but received ${PERSISTED_QTY}${NC}"
  exit 1
fi
echo -e "  ${GREEN}✓${NC} Supplier API returned HTTP 200: Stock updated to ${BOLD}${GREEN}${PERSISTED_QTY} units${NC}"

# Step 5: Verify MongoDB Document Store & Customer REST Reflection
echo -e "\n${BOLD}[Step 5/5] Verifying MongoDB collection 'petstore_products' and customer catalog reflection...${NC}"

MONGO_DOC=$(docker exec petstore-mongo mongosh --quiet --eval "JSON.stringify(db.getSiblingDB('petstore').petstore_products.findOne({'items.itemId': 'EST-1'}, {'items.$': 1}))")

if ! echo "${MONGO_DOC}" | grep -q "\"inventoryQuantity\":${NEW_QTY}"; then
  echo -e "${RED}[FAIL] MongoDB document did not reflect updated inventory quantity (${NEW_QTY}): ${MONGO_DOC}${NC}"
  exit 1
fi
echo -e "  ${GREEN}✓${NC} Confirmed document updated in MongoDB replica set 'rs0':"
echo -e "      MongoDB Filter:  {'items.itemId': 'EST-1'}"
echo -e "      Persisted Stock: ${BOLD}${GREEN}${NEW_QTY} units${NC}"

# Re-query customer item endpoint
CUSTOMER_VIEW=$(curl -s "${VITE_URL}/api/v1/items/EST-1")
VERIFIED_QTY=$(echo "${CUSTOMER_VIEW}" | grep -o '"inventoryQuantity":[0-9]*' | cut -d':' -f2)

if [ "${VERIFIED_QTY}" -ne "${NEW_QTY}" ]; then
  echo -e "${RED}[FAIL] Customer item view returned stale stock (${VERIFIED_QTY}) instead of ${NEW_QTY}!${NC}"
  exit 1
fi
echo -e "  ${GREEN}✓${NC} Customer Storefront Catalog verified: immediately returns fresh stock level (${VERIFIED_QTY} units)"

echo -e "\n${BOLD}${GREEN}===================================================================${NC}"
echo -e "${BOLD}${GREEN}  [SUCCESS] SUPPLIER INVENTORY MANAGEMENT FULLY VERIFIED!          ${NC}"
echo -e "${BOLD}${GREEN}===================================================================${NC}\n"
exit 0
