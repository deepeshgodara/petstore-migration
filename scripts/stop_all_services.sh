#!/usr/bin/env bash
# ==============================================================================
# Script: stop_all_services.sh
# Purpose: Gracefully stops all modern microservices, the Vite dev server,
#          and Docker containers.
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

GREEN='\033[0;32m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${BOLD}${CYAN}===================================================================${NC}"
echo -e "${BOLD}${CYAN}  PET STORE PLATFORM: ENVIRONMENT TEARDOWN                         ${NC}"
echo -e "${BOLD}${CYAN}===================================================================${NC}"

# Stop processes on ports 3000, 8081, 8082, 8085
kill_port() {
  local port="$1"
  local name="$2"
  local pids
  pids=$(lsof -tiTCP:"${port}" -sTCP:LISTEN 2>/dev/null || true)
  if [ -n "${pids}" ]; then
    echo -e "Stopping ${name} on port ${port} (PIDs: ${pids})..."
    kill ${pids} 2>/dev/null || true
    sleep 1
  fi
}

kill_port 3000 "Frontend Vite Dev Server"
kill_port 8081 "Catalog Microservice"
kill_port 8082 "Order Microservice"
kill_port 8085 "Migration Microservice"

# Stop Docker compose containers
echo -e "Stopping Docker infrastructure containers..."
docker compose -f "${ROOT_DIR}/petstore-modern/docker-compose.yml" stop

echo -e "\n${BOLD}${GREEN}===================================================================${NC}"
echo -e "${BOLD}${GREEN}  ALL PET STORE SERVICES SUCCESSFULLY STOPPED                      ${NC}"
echo -e "${BOLD}${GREEN}===================================================================${NC}\n"
