#!/usr/bin/env bash
# Downloads MySQL Connector/J into lib/ (run from the project root).
set -euo pipefail
VERSION="${1:-9.1.0}"
mkdir -p lib
curl -fL -o "lib/mysql-connector-j-${VERSION}.jar" \
  "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/${VERSION}/mysql-connector-j-${VERSION}.jar"
echo "Driver saved to lib/mysql-connector-j-${VERSION}.jar"
