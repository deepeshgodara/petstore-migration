#!/usr/bin/env bash
# ==============================================================================
# Script: run_all_verifications.sh
# Purpose: Master test suite executing all Phase 7 verification playback scripts:
#          1. verify_e2e_checkout.sh (Customer checkout & write propagation)
#          2. verify_admin_approval.sh (Admin dashboard approval & metrics update)
#          3. chaos_mongo_failure_test.sh (Chaos experiment, zero downtime, DLQ recovery)
# ==============================================================================

set -euo pipefail

GREEN='\033[0;32m'
RED='\033[0;31m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo -e "${BOLD}${CYAN}===================================================================${NC}"
echo -e "${BOLD}${CYAN}  PET STORE MODERNIZATION: MASTER VERIFICATION PLAYBACK SUITE      ${NC}"
echo -e "${BOLD}${CYAN}===================================================================${NC}"

echo -e "\n${BOLD}>>> [1/3] Executing Task 7.1: End-to-End Checkout Verification...${NC}"
"${SCRIPT_DIR}/verify_e2e_checkout.sh"

echo -e "\n${BOLD}>>> [2/3] Executing Task 7.2: Automated Admin Approval Verification...${NC}"
"${SCRIPT_DIR}/verify_admin_approval.sh"

echo -e "\n${BOLD}>>> [3/3] Executing Task 7.3: Chaos Outage & DLQ Recovery Test...${NC}"
"${SCRIPT_DIR}/chaos_mongo_failure_test.sh"

echo -e "\n${BOLD}${GREEN}===================================================================${NC}"
echo -e "${BOLD}${GREEN}  ★ ALL PHASE 7 PLAYBACK SUITES PASSED WITH 100% SUCCESS! ★        ${NC}"
echo -e "${BOLD}${GREEN}===================================================================${NC}\n"
exit 0
