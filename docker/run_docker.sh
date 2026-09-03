#!/bin/bash
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="$(dirname "$DIR")"

export PATH="/Applications/Docker.app/Contents/Resources/bin:$HOME/.docker/bin:/usr/local/bin:$PATH"

echo "=========================================================="
echo " Building Docker image for authentic PetStore baseline..."
echo "=========================================================="
docker build -t petstore-authentic-2002 "$DIR"

echo "Stopping existing container if running..."
docker rm -f petstore-baseline 2>/dev/null || true

echo "Starting container on ports 8000/8088 (HTTP) and 61616 (JMS)..."
docker run -d \
  --name petstore-baseline \
  -p 8000:8088 \
  -p 8088:8088 \
  -p 61616:61616 \
  -v "$ROOT_DIR":/petstore \
  petstore-authentic-2002

echo ""
echo "=========================================================="
echo " Pet Store 1.3.1_02 baseline is now RUNNING!"
echo "=========================================================="
echo " Storefront URL: http://localhost:8000/petstore/"
echo " Admin URL:      http://localhost:8000/admin/AdminRequestProcessor"
echo " Supplier URL:   http://localhost:8000/supplier/RcvrRequestProcessor"
echo ""
echo " Default Credentials:"
echo "   Customer: j2ee / j2ee"
echo "   Admin:    jps_admin / admin"
echo "   Supplier: supplier / supplier"
echo "=========================================================="
