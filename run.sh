#!/bin/bash
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

mkdir -p runner/bin

echo "Compiling Java Pet Store 1.3.1_02 Runner..."
javac -d runner/bin $(find runner/src -name "*.java")

echo "Starting Java Pet Store on http://localhost:8080/petstore/ ..."
exec java -cp runner/bin com.sun.j2ee.blueprints.petstore.runner.PetStoreServer
