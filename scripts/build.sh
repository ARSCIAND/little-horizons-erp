#!/usr/bin/env bash
# Compiles the project into out/ (run from the project root).
set -euo pipefail
rm -rf out && mkdir -p out
find src -name "*.java" > out/sources.txt
javac -encoding UTF-8 --release 17 -d out -cp "lib/*" @out/sources.txt
rm out/sources.txt
echo "Build OK -> out/"
