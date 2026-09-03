#!/bin/bash
set -e

echo "=========================================================="
echo " Starting Authentic 2002 J2EE 1.3 PetStore Environment..."
echo "=========================================================="

export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-arm64
export PATH=$JAVA_HOME/bin:$PATH

cd /petstore/legacy_container/tomee/bin
exec ./catalina.sh run
