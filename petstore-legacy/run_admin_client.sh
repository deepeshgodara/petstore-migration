#!/bin/bash
# ==============================================================================
# Java Pet Store 1.3.1_02 - Pet Store Admin Rich Client Launcher
# ==============================================================================
# The Pet Store Admin module is an authentic Java Web Start / Swing desktop client.
# This script authenticates with the container admin portal, retrieves a session ID,
# and launches the Swing Administration Client GUI window.
# ==============================================================================

set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HOST="localhost"
PORT="8000"
COOKIE_JAR="/tmp/petstore_admin_cookie.txt"
LIB_DIR="/tmp/petstore_admin_client"

echo "=========================================================="
echo " Launching Pet Store Admin Rich Client (Java Swing)..."
echo "=========================================================="

# 1. Ensure required client JARs are extracted
if [ ! -f "${LIB_DIR}/AdminApp.jar" ]; then
    echo "Extracting Admin client libraries from petstoreadmin.ear..."
    mkdir -p "${LIB_DIR}"
    cd "${LIB_DIR}"
    jar xf "${DIR}/petstoreadmin.ear" petstoreadmin.war
    jar xf petstoreadmin.war AdminApp.jar jaxp.jar crimson.jar
    rm -f petstoreadmin.war
    cd "${DIR}"
fi

# 2. Authenticate against container admin portal
echo "Authenticating with container admin portal (http://${HOST}:${PORT}/admin)..."
rm -f "$COOKIE_JAR"
curl -s -c "$COOKIE_JAR" -b "$COOKIE_JAR" "http://${HOST}:${PORT}/admin/AdminRequestProcessor" > /dev/null
curl -L -s -c "$COOKIE_JAR" -b "$COOKIE_JAR" -d "j_username=jps_admin&j_password=admin" "http://${HOST}:${PORT}/admin/j_security_check" > /dev/null

# 3. Extract authenticated JSESSIONID
SESSION_ID=$(grep "JSESSIONID" "$COOKIE_JAR" | awk '{print $NF}' | tail -n 1)

if [ -z "$SESSION_ID" ]; then
    echo ""
    echo "Error: Could not obtain authenticated session from http://${HOST}:${PORT}/admin/"
    echo "Please ensure the Pet Store container is running first: ./docker/run_docker.sh"
    exit 1
fi

echo "Successfully authenticated as 'jps_admin'."
echo "Session ID: ${SESSION_ID}"
echo "Starting Java Swing Desktop Application..."
echo ""

CLASSPATH="${LIB_DIR}/AdminApp.jar:${LIB_DIR}/jaxp.jar:${LIB_DIR}/crimson.jar"

exec java -cp "$CLASSPATH" \
  com.sun.j2ee.blueprints.admin.client.PetStoreAdminClient \
  com.sun.j2ee.blueprints.admin.client.HttpPostPetStoreProxy \
  "$HOST" "$PORT" "$SESSION_ID"
