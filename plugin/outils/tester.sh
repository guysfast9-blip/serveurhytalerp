#!/usr/bin/env bash
# Compile le cœur et les tests, puis lance JUnit. Code de sortie non nul en cas d'échec.
set -euo pipefail
cd "$(dirname "$0")/.."
CP="lib/HytaleServer-api21.jar:/opt/gradle-8.14.3/lib/junit-4.13.2.jar:/opt/gradle-8.14.3/lib/hamcrest-core-1.3.jar"
rm -rf build/test && mkdir -p build/test
javac --release 21 -encoding UTF-8 -nowarn -d build/test -cp "$CP" \
  $(find src/main/java/fr/aldermyr/coeur src/test/java -name '*.java') 2>&1 | grep -v '^Picked up' || true
test -n "$(find build/test -name '*Test.class' 2>/dev/null)" || { echo "ÉCHEC DE COMPILATION"; exit 1; }
cp -r src/main/resources/. build/test/ 2>/dev/null || true
CLASSES=$(cd build/test && find . -name '*Test.class' | sed 's#^\./##; s#\.class$##; s#/#.#g' | sort)
java -cp "build/test:$CP" org.junit.runner.JUnitCore $CLASSES 2>&1 | grep -v '^Picked up'
