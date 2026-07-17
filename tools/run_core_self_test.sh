#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$ROOT/build/selftest"
rm -rf "$OUT"
mkdir -p "$OUT"
mapfile -t SOURCES < <(find "$ROOT/game-core/src/main/kotlin" -name '*.kt' | sort)
kotlinc "${SOURCES[@]}" "$ROOT/tools/CoreSelfTest.kt" -include-runtime -d "$OUT/range-sense-core-selftest.jar"
java -jar "$OUT/range-sense-core-selftest.jar"
