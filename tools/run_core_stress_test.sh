#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$ROOT/build/stress"
rm -rf "$OUT"
mkdir -p "$OUT"
mapfile -t SOURCES < <(find "$ROOT/game-core/src/main/kotlin" -name '*.kt' | sort)
kotlinc "${SOURCES[@]}" "$ROOT/tools/CoreStressTest.kt" -include-runtime -d "$OUT/range-sense-core-stress.jar"
java -jar "$OUT/range-sense-core-stress.jar"
