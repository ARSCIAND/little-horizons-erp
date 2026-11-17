#!/usr/bin/env bash
# Runs the application (compile first with scripts/build.sh).
set -euo pipefail
java -Dfile.encoding=UTF-8 -cp "out:lib/*" com.littlehorizons.erp.Main
