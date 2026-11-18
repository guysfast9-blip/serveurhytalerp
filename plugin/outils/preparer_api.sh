#!/usr/bin/env bash
# Prépare la copie de compilation de l'API Hytale (jamais exécutée ni livrée).
# Usage : outils/preparer_api.sh /chemin/vers/HytaleServer.jar
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p lib
python3 -I outils/abaisser_version.py "$1" lib/HytaleServer-api21.jar
echo "lib/HytaleServer-api21.jar prêt"
