#!/usr/bin/env bash
# Compile tout le plugin (bytecode Java 21) et produit build/AldermyrGenerateur.jar.
set -euo pipefail
cd "$(dirname "$0")/.."
rm -rf build/classes && mkdir -p build/classes
javac --release 21 -encoding UTF-8 -d build/classes -cp lib/HytaleServer-api21.jar \
  $(find src/main/java -name '*.java') 2>&1 | grep -v '^Picked up'
cp -r src/main/resources/. build/classes/
(cd build/classes && jar --create --file ../AldermyrGenerateur.jar .)
echo "build/AldermyrGenerateur.jar produit"
