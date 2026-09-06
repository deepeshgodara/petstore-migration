#!/usr/bin/env bash
# ==============================================================================
# Script: mongo_compass_connect.sh
# Purpose: Launches MongoDB Compass directly with the pre-configured local PetStore
#          replica set connection string for developers and on-call engineers.
# ==============================================================================

set -euo pipefail

COMPASS_URI="mongodb://localhost:27017/petstore?replicaSet=rs0&directConnection=true"

echo "======================================================================"
echo "          Pet Store Modernization - MongoDB Compass Connector         "
echo "======================================================================"
echo "Target Replica Set URI:"
echo "  ${COMPASS_URI}"
echo ""

# Verify local MongoDB port connectivity
if nc -z -w 2 localhost 27017 2>/dev/null; then
  echo "✓ MongoDB instance on localhost:27017 is reachable and active."
else
  echo "⚠ Warning: Port 27017 is not responding. Ensure MongoDB replica set container is running:"
  echo "  docker compose -f docker/docker-compose.yml up -d mongo"
fi

echo ""

# Check for macOS MongoDB Compass Application
if [ -d "/Applications/MongoDB Compass.app" ]; then
  echo "✓ Located MongoDB Compass at /Applications/MongoDB Compass.app"
  echo "Launching MongoDB Compass with Pet Store connection string..."
  open -a "MongoDB Compass" "${COMPASS_URI}" 2>/dev/null || open "${COMPASS_URI}"
  echo "✓ MongoDB Compass triggered successfully."
elif which open >/dev/null 2>&1; then
  echo "Attempting to open registered mongodb:// protocol handler on macOS..."
  if open "${COMPASS_URI}" 2>/dev/null; then
    echo "✓ URI opened with default system database client."
  else
    echo "ℹ MongoDB Compass application not found at /Applications/MongoDB Compass.app."
    echo "Please copy the connection string below and paste it into MongoDB Compass:"
    echo "  ${COMPASS_URI}"
  fi
else
  echo "Please copy the connection string below and paste it into MongoDB Compass:"
  echo "  ${COMPASS_URI}"
fi

echo "======================================================================"
