#!/usr/bin/env bash
# ==============================================================================
# Script: query_legacy_db.sh
# Purpose: Interactive CLI utility to inspect and query data in the legacy
#          Pet Store database (HSQLDB/Cloudscape schema in docker/data/petstoredb).
#
# Usage:
#   ./scripts/query_legacy_db.sh                    # Displays summary of all tables
#   ./scripts/query_legacy_db.sh "<CUSTOM SQL>"      # Executes custom SQL query
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
DB_PATH="${ROOT_DIR}/docker/data/petstoredb"
JAR_PATH="${HOME}/.m2/repository/org/hsqldb/hsqldb/2.7.2/hsqldb-2.7.2.jar"

# ANSI colors
CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
NC='\033[0m'

if [ ! -f "${JAR_PATH}" ]; then
  # Try to find any hsqldb jar in m2 repo
  FOUND_JAR=$(find "${HOME}/.m2/repository" -name "hsqldb-*.jar" 2>/dev/null | head -n 1 || true)
  if [ -n "${FOUND_JAR}" ]; then
    JAR_PATH="${FOUND_JAR}"
  else
    echo "HSQLDB driver jar not found. Please run 'mvn dependency:resolve' in petstore-modern first."
    exit 1
  fi
fi

if [ $# -gt 0 ]; then
  QUERY="$1"
  echo -e "${BOLD}${CYAN}Executing Query:${NC} ${QUERY}"
  java -cp "${JAR_PATH}" "${SCRIPT_DIR}/LegacyDbQuery.java" "${DB_PATH}" "${QUERY}"
  exit 0
fi

echo -e "${BOLD}${CYAN}===================================================================${NC}"
echo -e "${BOLD}${CYAN}  LEGACY PET STORE DATABASE INSPECTION                             ${NC}"
echo -e "${BOLD}${CYAN}  Database: ${DB_PATH}                                              ${NC}"
echo -e "${BOLD}${CYAN}===================================================================${NC}"

echo -e "\n${BOLD}${YELLOW}1. CATEGORIES (CATEGORY & CATEGORY_DETAILS)${NC}"
java -cp "${JAR_PATH}" "${SCRIPT_DIR}/LegacyDbQuery.java" "${DB_PATH}" \
  "SELECT C.CATID, CD.NAME, CD.LOCALE FROM CATEGORY C JOIN CATEGORY_DETAILS CD ON C.CATID = CD.CATID WHERE CD.LOCALE = 'en_US'"

echo -e "\n${BOLD}${YELLOW}2. PRODUCTS (Sample from PRODUCT & PRODUCT_DETAILS)${NC}"
java -cp "${JAR_PATH}" "${SCRIPT_DIR}/LegacyDbQuery.java" "${DB_PATH}" \
  "SELECT P.PRODUCTID, P.CATID, PD.NAME FROM PRODUCT P JOIN PRODUCT_DETAILS PD ON P.PRODUCTID = PD.PRODUCTID WHERE PD.LOCALE = 'en_US' LIMIT 6"

echo -e "\n${BOLD}${YELLOW}3. USERS & CUSTOMERS (USER & PROFILE)${NC}"
java -cp "${JAR_PATH}" "${SCRIPT_DIR}/LegacyDbQuery.java" "${DB_PATH}" \
  "SELECT U.USERNAME, U.PASSWORD, P.PREFERREDLANGUAGE, P.FAVORITECATEGORY FROM USER U JOIN CUSTOMER C ON U.USERNAME = C.USERID JOIN PROFILE P ON C.PROFILE_OPENEJB_PK = P.OPENEJB_PK"

echo -e "\n${BOLD}${YELLOW}4. PURCHASE ORDERS & STATUS (PURCHASEORDER & MANAGER)${NC}"
java -cp "${JAR_PATH}" "${SCRIPT_DIR}/LegacyDbQuery.java" "${DB_PATH}" \
  "SELECT PO.POID, PO.POUSERID, PO.POVALUE, M.STATUS, PO.POLOCALE FROM PURCHASEORDER PO LEFT JOIN MANAGER M ON PO.POID = M.ORDERID"

echo -e "\n${BOLD}${YELLOW}5. SAMPLE ORDER LINE ITEMS (LINEITEM)${NC}"
java -cp "${JAR_PATH}" "${SCRIPT_DIR}/LegacyDbQuery.java" "${DB_PATH}" \
  "SELECT PURCHASEORDER_LINEITEMS_POID AS ORDER_ID, ITEMID, PRODUCTID, QUANTITY, UNITPRICE FROM LINEITEM WHERE PURCHASEORDER_LINEITEMS_POID IS NOT NULL LIMIT 6"

echo -e "\n${BOLD}${GREEN}===================================================================${NC}"
echo -e "${BOLD}Tip: Run custom queries using:${NC}"
echo -e "  ${CYAN}./scripts/query_legacy_db.sh \"SELECT * FROM INVENTORY LIMIT 5\"${NC}"
echo -e "${BOLD}${GREEN}===================================================================${NC}\n"
