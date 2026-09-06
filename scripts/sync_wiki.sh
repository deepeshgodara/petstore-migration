#!/usr/bin/env bash
# ==============================================================================
# Script: sync_wiki.sh
# Purpose: Synchronizes the local wiki/ directory with the GitHub Wiki git repository
#          (https://github.com/deepeshgodara/petstore-migration.wiki.git).
# ==============================================================================

set -euo pipefail

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
WIKI_SRC="${ROOT_DIR}/wiki"
WIKI_REMOTE="https://github.com/deepeshgodara/petstore-migration.wiki.git"
TEMP_DIR="/tmp/petstore-wiki-sync-$$"

echo -e "${BOLD}${CYAN}===================================================================${NC}"
echo -e "${BOLD}${CYAN}  PET STORE GITHUB WIKI SYNCHRONIZATION TOOL                       ${NC}"
echo -e "${BOLD}${CYAN}===================================================================${NC}"

if [ ! -d "${WIKI_SRC}" ]; then
  echo -e "${RED}[ERROR] Wiki directory not found at: ${WIKI_SRC}${NC}"
  exit 1
fi

echo -e "\n${BOLD}[Step 1/3] Checking GitHub Wiki repository status...${NC}"
if ! git ls-remote "${WIKI_REMOTE}" &>/dev/null; then
  echo -e "  ${YELLOW}⚠${NC} The GitHub Wiki repository does not yet exist or is not activated."
  echo -e "      To activate GitHub Wiki:"
  echo -e "      1. Visit: https://github.com/deepeshgodara/petstore-migration/wiki"
  echo -e "      2. Click the green ${BOLD}'Create the first page'${NC} button."
  echo -e "      3. Click 'Save page'."
  echo -e "      4. Re-run this script to sync all pages automatically!"
  echo -e "\n  ${GREEN}✓${NC} All wiki markdown documentation is safely preserved in ${BOLD}wiki/${NC} in the main repository."
  exit 0
fi

echo -e "  ${GREEN}✓${NC} GitHub Wiki repository is active and reachable."

echo -e "\n${BOLD}[Step 2/3] Cloning Wiki remote into temporary staging directory...${NC}"
mkdir -p "${TEMP_DIR}"
git clone "${WIKI_REMOTE}" "${TEMP_DIR}" --quiet
echo -e "  ${GREEN}✓${NC} Staging clone completed."

echo -e "\n${BOLD}[Step 3/3] Copying local wiki pages and committing to Wiki remote...${NC}"
cp -r "${WIKI_SRC}"/* "${TEMP_DIR}/"

cd "${TEMP_DIR}"
git config user.name "$(git -C "${ROOT_DIR}" config user.name || echo 'PetStore Architect')"
git config user.email "$(git -C "${ROOT_DIR}" config user.email || echo 'architect@petstore.internal')"

git add .
if git diff --cached --quiet; then
  echo -e "  ${GREEN}✓${NC} GitHub Wiki is already up-to-date with local wiki/ documentation."
else
  git commit -m "docs(wiki): update enterprise architecture, service catalog, runbooks, and compass guide"
  git push origin master --quiet || git push origin main --quiet
  echo -e "  ${GREEN}✓${NC} Successfully pushed all wiki pages to GitHub Wiki!"
fi

rm -rf "${TEMP_DIR}"
echo -e "\n${BOLD}${GREEN}===================================================================${NC}"
echo -e "${BOLD}${GREEN}  [SUCCESS] WIKI SYNCHRONIZATION COMPLETE!                         ${NC}"
echo -e "${BOLD}${GREEN}  Visit: https://github.com/deepeshgodara/petstore-migration/wiki  ${NC}"
echo -e "${BOLD}${GREEN}===================================================================${NC}\n"
