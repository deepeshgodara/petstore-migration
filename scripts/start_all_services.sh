#!/usr/bin/env bash
# ==============================================================================
# Script: start_all_services.sh
# Purpose: Single-command startup orchestration for the entire Pet Store platform:
#          1. Starts Docker infrastructure (MongoDB rs0, Kafka KRaft, UIs)
#          2. Verifies health checks
#          3. Starts modern Spring Boot microservices (Catalog, Order, Migration)
#          4. Starts modern React 18 frontend
#          5. Displays service dashboard with ports and credentials
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

GREEN='\033[0;32m'
RED='\033[0;31m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${BOLD}${CYAN}===================================================================${NC}"
echo -e "${BOLD}${CYAN}  PET STORE PLATFORM: ORCHESTRATED ENVIRONMENT STARTUP             ${NC}"
echo -e "${BOLD}${CYAN}===================================================================${NC}"

# 1. Start Docker containers
echo -e "\n${BOLD}[1/4] Starting Docker Infrastructure...${NC}"
docker compose -f "${ROOT_DIR}/petstore-modern/docker-compose.yml" up -d

echo -e "Waiting for MongoDB and Kafka cluster readiness..."
until docker exec petstore-mongo mongosh --quiet --eval "db.adminCommand('ping')" > /dev/null 2>&1; do
  sleep 1
done
echo -e "  ${GREEN}✓${NC} MongoDB replica set 'rs0' ready on port 27017"

until docker exec petstore-kafka /usr/bin/kafka-topics --bootstrap-server localhost:9092 --list > /dev/null 2>&1; do
  sleep 1
done
echo -e "  ${GREEN}✓${NC} Apache Kafka KRaft cluster ready on port 9092"

# 2. Start Microservices if not already running
echo -e "\n${BOLD}[2/4] Checking & Starting Spring Boot 3.3 Microservices (Java 21)...${NC}"
LOG_DIR="${ROOT_DIR}/logs"
mkdir -p "${LOG_DIR}"

check_port() {
  lsof -iTCP:"$1" -sTCP:LISTEN -t >/dev/null 2>&1
}

# Catalog Service (8081)
if check_port 8081; then
  echo -e "  ${GREEN}✓${NC} Catalog Service already listening on port 8081"
else
  echo -e "  Starting Catalog Service on port 8081..."
  (cd "${ROOT_DIR}/petstore-modern" && nohup mvn -pl petstore-catalog-service spring-boot:run > "${LOG_DIR}/catalog-service.log" 2>&1 &)
fi

# Order Service (8082)
if check_port 8082; then
  echo -e "  ${GREEN}✓${NC} Order Service already listening on port 8082"
else
  echo -e "  Starting Order Service on port 8082..."
  (cd "${ROOT_DIR}/petstore-modern" && nohup mvn -pl petstore-order-service spring-boot:run > "${LOG_DIR}/order-service.log" 2>&1 &)
fi

# Migration Service (8085)
if check_port 8085; then
  echo -e "  ${GREEN}✓${NC} Migration Service already listening on port 8085"
else
  echo -e "  Starting Migration Service on port 8085..."
  (cd "${ROOT_DIR}/petstore-modern" && nohup mvn -pl petstore-migration-service spring-boot:run > "${LOG_DIR}/migration-service.log" 2>&1 &)
fi

# 3. Start Frontend (3000)
echo -e "\n${BOLD}[3/4] Checking & Starting React 18 + Vite Frontend...${NC}"
if check_port 3000; then
  echo -e "  ${GREEN}✓${NC} Vite Frontend already listening on port 3000"
else
  echo -e "  Starting Vite dev server on port 3000..."
  (cd "${ROOT_DIR}/petstore-frontend" && nohup npm run dev > "${LOG_DIR}/frontend.log" 2>&1 &)
fi

# 4. Wait for endpoints
echo -e "\n${BOLD}[4/4] Verifying HTTP Endpoints...${NC}"
wait_for_http() {
  local url="$1"
  local name="$2"
  local retries=30
  local count=0
  until curl -s -f -o /dev/null "${url}" || [ ${count} -eq ${retries} ]; do
    sleep 1
    count=$((count + 1))
  done
  if [ ${count} -eq ${retries} ]; then
    echo -e "  ${YELLOW}⚠${NC} ${name} startup taking longer than expected (${url})"
  else
    echo -e "  ${GREEN}✓${NC} ${name} verified (${url})"
  fi
}

wait_for_http "http://localhost:8081/api/v1/categories" "Catalog Service"
wait_for_http "http://localhost:8082/api/v1/orders/admin/summary" "Order Service"
wait_for_http "http://localhost:8085/api/v1/migration/parity" "Migration Service"
wait_for_http "http://localhost:3000/" "Modern Frontend UI"

echo -e "\n${BOLD}${GREEN}===================================================================${NC}"
echo -e "${BOLD}${GREEN}  PET STORE PLATFORM IS UP AND RUNNING!                            ${NC}"
echo -e "${BOLD}${GREEN}===================================================================${NC}"
echo -e "  ${BOLD}Modern Storefront:${NC}       ${CYAN}http://localhost:3000/${NC}"
echo -e "  ${BOLD}Customer Account:${NC}        ${CYAN}http://localhost:3000/account${NC}"
echo -e "  ${BOLD}Admin Dashboard:${NC}         ${CYAN}http://localhost:3000/admin${NC}"
echo -e "  ${BOLD}Migration Parity Monitor:${NC} ${CYAN}http://localhost:3000/ops${NC}"
echo -e "  ${BOLD}Kafka UI Management:${NC}     ${CYAN}http://localhost:8087${NC}"
echo -e "  ${BOLD}Mongo Express DB Admin:${NC}  ${CYAN}http://localhost:8086${NC}"
echo -e "  ${BOLD}Legacy Pet Store (TomEE):${NC} ${CYAN}http://localhost:8000/petstore/${NC}"
echo -e "\n  ${BOLD}Demo Authentication Credentials:${NC}"
echo -e "    Customer:     ${YELLOW}j2ee${NC} / ${YELLOW}j2ee${NC} (ROLE_CUSTOMER)"
echo -e "    Admin:        ${YELLOW}admin${NC} / ${YELLOW}admin123${NC} (ROLE_ADMIN)"
echo -e "    Ops/Engineer: ${YELLOW}engineer${NC} / ${YELLOW}eng123${NC} (ROLE_ENGINEER)"
echo -e "    Superadmin:   ${YELLOW}root${NC} / ${YELLOW}root123${NC} (ROLE_SUPERADMIN)"
echo -e "\n  ${BOLD}Verification Playback Scripts:${NC}"
echo -e "    ${BOLD}./scripts/run_all_verifications.sh${NC}"
echo -e "${BOLD}${GREEN}===================================================================${NC}\n"
