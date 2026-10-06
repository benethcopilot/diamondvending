#!/usr/bin/env bash
# Fails unless a node built exactly one mod jar, the jar is readable, and it has no classes from the other loaders.
# Usage: check-jar-contents.sh <node> [repo-root]   e.g. 26.1-neoforge
set -euo pipefail

node="$1"
root="${2:-.}"

case "$node" in
  *-fabric)   others="neoforge forge" ;;
  *-neoforge) others="fabric forge" ;;
  *-forge)    others="fabric neoforge" ;;
  *) echo "::error::unknown node '$node' (expected <version>-fabric, -neoforge or -forge)"; exit 1 ;;
esac

shopt -s nullglob
jars=()
for f in "$root/versions/$node/build/libs/"diamondvending-*.jar; do
  case "$f" in
    *-sources.jar) ;;
    *) jars+=("$f") ;;
  esac
done
if [ "${#jars[@]}" -ne 1 ]; then
  echo "::error::expected exactly one mod jar for $node, found ${#jars[@]}: ${jars[*]:-none}"
  exit 1
fi

jar="${jars[0]}"
echo "Checking $jar"
# Capture the listing first: under `set -e` an unreadable jar stops here instead of looking clean.
listing=$(unzip -Z1 "$jar")
for other in $others; do
  if grep -q "diamondvending/platform/$other/" <<< "$listing"; then
    echo "::error::$jar contains diamondvending/platform/$other classes"
    exit 1
  fi
done
echo "OK: $jar has no $others classes"
