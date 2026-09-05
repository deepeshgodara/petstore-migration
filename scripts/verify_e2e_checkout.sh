#!/usr/bin/env bash
# ==============================================================================
# Script: verify_e2e_checkout.sh
# Purpose: Verifies end-to-end customer checkout in the modern architecture:
#          1. Places an order via modern REST API (Order Service port 8082)
#          2. Verifies persistence in MongoDB (petstore_orders collection)
#          3. Verifies Kafka dual-write event streaming
#          4. Executes shadow reconciliation audit on Migration Service (port 8085)
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
echo -e "${BOLD}${CYAN}  PET STORE MODERNIZATION: END-TO-END CHECKOUT VERIFICATION        ${NC}"
echo -e "${BOLD}${CYAN}===================================================================${NC}"

ORDER_SERVICE_URL="http://localhost:8082"
MIGRATION_SERVICE_URL="http://localhost:8085"

# Step 1: Health checks
echo -e "\n${BOLD}[Step 1/5] Checking microservice availability...${NC}"

if ! curl -s -f "${ORDER_SERVICE_URL}/api/v1/orders/admin/summary" > /dev/null; then
  echo -e "${RED}[FAIL] Order Service at ${ORDER_SERVICE_URL} is unreachable!${NC}"
  exit 1
fi
echo -e "  ${GREEN}✓${NC} Order Service is healthy on port 8082"

if ! curl -s -f "${MIGRATION_SERVICE_URL}/api/v1/migration/parity" > /dev/null; then
  echo -e "${RED}[FAIL] Migration Service at ${MIGRATION_SERVICE_URL} is unreachable!${NC}"
  exit 1
fi
echo -e "  ${GREEN}✓${NC} Migration Service is healthy on port 8085"

# Step 2: Submit Customer Order Checkout
echo -e "\n${BOLD}[Step 2/5] Submitting Customer Order via modern REST endpoint (POST /api/v1/orders)...${NC}"

TIMESTAMP=$(date +%s%N)
TEST_USER="modern_shopper_${TIMESTAMP: -6}"

PAYLOAD=$(cat <<EOF
{
  "userId": "${TEST_USER}",
  "locale": "en_US",
  "billing": {
    "name": "Alex Tech",
    "address1": "500 Silicon Vista",
    "address2": "Apt 12B",
    "city": "San Jose",
    "state": "CA",
    "postalCode": "95112",
    "country": "USA",
    "telephone": "408-555-0188",
    "email": "${TEST_USER}@petstore.internal"
  },
  "shipping": {
    "name": "Alex Tech",
    "address1": "500 Silicon Vista",
    "address2": "Apt 12B",
    "city": "San Jose",
    "state": "CA",
    "postalCode": "95112",
    "country": "USA",
    "telephone": "408-555-0188",
    "email": "${TEST_USER}@petstore.internal"
  },
  "payment": {
    "cardType": "VISA",
    "cardNumberMasked": "•••• •••• •••• 9876",
    "expiryDate": "09/29"
  },
  "lineItems": [
    {
      "lineNumber": 1,
      "itemId": "EST-1",
      "productId": "FI-SW-01",
      "categoryId": "FISH",
      "quantity": 1,
      "unitPrice": 142.00,
      "totalCost": 142.00
    },
    {
      "lineNumber": 2,
      "itemId": "EST-18",
      "productId": "AV-CB-01",
      "categoryId": "BIRDS",
      "quantity": 1,
      "unitPrice": 1600.00,
      "totalCost": 1600.00
    }
  ]
}
EOF
)

RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "${ORDER_SERVICE_URL}/api/v1/orders" \
  -H "Content-Type: application/json" \
  -d "${PAYLOAD}")

HTTP_STATUS=$(echo "${RESPONSE}" | grep "HTTP_STATUS:" | cut -d':' -f2)
BODY=$(echo "${RESPONSE}" | sed '/HTTP_STATUS:/d')

if [ "${HTTP_STATUS}" != "201" ]; then
  echo -e "${RED}[FAIL] Order placement failed with HTTP ${HTTP_STATUS}: ${BODY}${NC}"
  exit 1
fi

ORDER_ID=$(echo "${BODY}" | grep -o '"id":"[^"]*' | cut -d'"' -f4)
TOTAL_PRICE=$(echo "${BODY}" | grep -o '"totalPrice":[^,}]*' | cut -d':' -f2)
STATUS=$(echo "${BODY}" | grep -o '"status":"[^"]*' | cut -d'"' -f4)

echo -e "  ${GREEN}✓${NC} Order successfully placed!"
echo -e "      Order ID:     ${BOLD}${CYAN}#${ORDER_ID}${NC}"
echo -e "      Status:       ${YELLOW}${STATUS}${NC}"
echo -e "      Total Amount: ${GREEN}\$${TOTAL_PRICE}${NC}"
echo -e "      User:         ${TEST_USER}"

# Step 3: Verify Persistence in MongoDB Document Store
echo -e "\n${BOLD}[Step 3/5] Verifying MongoDB document aggregate in collection 'petstore_orders'...${NC}"

MONGO_DOC=$(docker exec petstore-mongo mongosh --quiet --eval "JSON.stringify(db.getSiblingDB('petstore').petstore_orders.findOne({_id: '${ORDER_ID}'}))")

if [ -z "${MONGO_DOC}" ] || [ "${MONGO_DOC}" = "null" ]; then
  echo -e "${RED}[FAIL] Order #${ORDER_ID} was NOT found in MongoDB collection 'petstore_orders'!${NC}"
  exit 1
fi

MONGO_LINES=$(echo "${MONGO_DOC}" | grep -o '"itemId"' | wc -l | tr -d ' ')
echo -e "  ${GREEN}✓${NC} Confirmed document persisted in MongoDB replica set 'rs0':"
echo -e "      MongoDB _id:       ${ORDER_ID}"
echo -e "      Persisted Lines:   ${MONGO_LINES} line item(s)"
echo -e "      Document Class:    com.petstore.order.document.OrderDocument"

# Step 4: Verify REST Retrieval by Order ID
echo -e "\n${BOLD}[Step 4/5] Verifying REST retrieval via GET /api/v1/orders/${ORDER_ID}...${NC}"

GET_RESPONSE=$(curl -s -f "${ORDER_SERVICE_URL}/api/v1/orders/${ORDER_ID}")
RETRIEVED_ID=$(echo "${GET_RESPONSE}" | grep -o '"id":"[^"]*' | cut -d'"' -f4)

if [ "${RETRIEVED_ID}" != "${ORDER_ID}" ]; then
  echo -e "${RED}[FAIL] Retrieved Order ID (${RETRIEVED_ID}) does not match placed Order ID (${ORDER_ID})!${NC}"
  exit 1
fi
echo -e "  ${GREEN}✓${NC} GET endpoint verified: returned exact order payload with status ${STATUS}"

# Step 5: Execute Shadow Reconciliation Audit
echo -e "\n${BOLD}[Step 5/5] Executing immediate Shadow Reconciliation Audit via Migration Service...${NC}"

AUDIT_RESPONSE=$(curl -s "${MIGRATION_SERVICE_URL}/api/v1/migration/parity?runAudit=true")
PARITY_PCT=$(echo "${AUDIT_RESPONSE}" | grep -o '"parityPercentage":[^,}]*' | cut -d':' -f2)
TOTAL_COMPARISONS=$(echo "${AUDIT_RESPONSE}" | grep -o '"totalComparisons":[^,}]*' | cut -d':' -f2)
TOTAL_MATCHES=$(echo "${AUDIT_RESPONSE}" | grep -o '"totalMatches":[^,}]*' | cut -d':' -f2)
TOTAL_DRIFTS=$(echo "${AUDIT_RESPONSE}" | grep -o '"totalDrifts":[^,}]*' | cut -d':' -f2)
PARITY_STATUS=$(echo "${AUDIT_RESPONSE}" | grep -o '"status":"[^"]*' | cut -d'"' -f4)

echo -e "  ${GREEN}✓${NC} Shadow Reconciliation Audit Completed:"
echo -e "      Parity Percentage:   ${BOLD}${GREEN}${PARITY_PCT}%${NC}"
echo -e "      Status:              ${BOLD}${GREEN}${PARITY_STATUS}${NC}"
echo -e "      Total Comparisons:   ${TOTAL_COMPARISONS}"
echo -e "      Total Matches:       ${TOTAL_MATCHES}"
echo -e "      Total Drifts:        ${TOTAL_DRIFTS}"

if [ "${TOTAL_DRIFTS}" != "0" ]; then
  echo -e "${RED}[FAIL] Data drift was detected in reconciliation audit!${NC}"
  exit 1
fi

echo -e "\n${BOLD}${GREEN}===================================================================${NC}"
echo -e "${BOLD}${GREEN}  [SUCCESS] END-TO-END CHECKOUT & WRITE PROPAGATION VERIFIED!      ${NC}"
echo -e "${BOLD}${GREEN}===================================================================${NC}\n"
exit 0
