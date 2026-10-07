#!/usr/bin/env bash
# Self-test for check-jar-contents.sh: builds fake node outputs and checks the verdict for each.
# Needs the JDK's `jar` tool (uses $JAVA_HOME/bin/jar when JAVA_HOME is set).
set -uo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
check="$here/check-jar-contents.sh"
jartool="${JAVA_HOME:+$JAVA_HOME/bin/}jar"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
failures=0

# make_jar <jar path> <entry>... — creates a jar holding empty files at the given entry paths
make_jar() {
  local out="$1"; shift
  local src="$work/src-$RANDOM"
  mkdir -p "$src"
  for entry in "$@"; do
    mkdir -p "$src/$(dirname "$entry")"
    : > "$src/$entry"
  done
  mkdir -p "$(dirname "$out")"
  "$jartool" cf "$out" -C "$src" .
}

# expect <pass|fail> <description> <node> <case root>
expect() {
  local want="$1" what="$2" node="$3" caseroot="$4" got
  if bash "$check" "$node" "$caseroot" > "$work/out.txt" 2>&1; then got=pass; else got=fail; fi
  if [ "$got" = "$want" ]; then
    echo "ok   - $what"
  else
    echo "FAIL - $what (expected $want, got $got)"
    sed 's/^/       /' "$work/out.txt"
    failures=$((failures + 1))
  fi
}

libs() { echo "$work/$1/versions/$2/build/libs"; }

# 1. No jar at all (e.g. after an archivesName rename) must fail, not pass vacuously
mkdir -p "$(libs nojar 26.1-fabric)"
expect fail "no mod jar found" 26.1-fabric "$work/nojar"

# 2. Only a sources jar must fail
make_jar "$(libs onlysources 26.1-fabric)/diamondvending-fabric-0.1.0+26.1.2-sources.jar" diamondvending/DiamondVending.java
expect fail "only a sources jar" 26.1-fabric "$work/onlysources"

# 3. Two mod jars is ambiguous and must fail
make_jar "$(libs two 26.1-fabric)/diamondvending-fabric-0.1.0+26.1.2.jar" diamondvending/platform/fabric/A.class
make_jar "$(libs two 26.1-fabric)/diamondvending-fabric-0.2.0+26.1.2.jar" diamondvending/platform/fabric/A.class
expect fail "two mod jars" 26.1-fabric "$work/two"

# 4. The other loader's classes inside the jar must fail
make_jar "$(libs leak 26.1-fabric)/diamondvending-fabric-0.1.0+26.1.2.jar" \
  diamondvending/platform/fabric/A.class diamondvending/platform/neoforge/B.class diamondvending/platform/forge/C.class
expect fail "fabric jar with neoforge classes" 26.1-fabric "$work/leak"
make_jar "$(libs leakn 1.21.1-neoforge)/diamondvending-neoforge-0.1.0+1.21.1.jar" \
  diamondvending/platform/neoforge/A.class diamondvending/platform/fabric/B.class
expect fail "neoforge jar with fabric classes" 1.21.1-neoforge "$work/leakn"

# 5. A file that isn't a zip must fail
mkdir -p "$(libs corrupt 26.1-fabric)"
echo "not a zip" > "$(libs corrupt 26.1-fabric)/diamondvending-fabric-0.1.0+26.1.2.jar"
expect fail "corrupt jar" 26.1-fabric "$work/corrupt"

# 6. Unknown node names must fail
make_jar "$(libs odd 26.1-quilt)/diamondvending-quilt-0.1.0+26.1.2.jar" diamondvending/A.class
expect fail "unknown node name" 26.1-quilt "$work/odd"

# 4b. A forge jar must hold neither fabric nor neoforge classes
make_jar "$(libs leakf 1.20.1-forge)/diamondvending-forge-0.1.0+1.20.1.jar" \
  diamondvending/platform/forge/A.class diamondvending/platform/neoforge/B.class
expect fail "forge jar with neoforge classes" 1.20.1-forge "$work/leakf"
make_jar "$(libs leakff 1.20.1-forge)/diamondvending-forge-0.1.0+1.20.1.jar" \
  diamondvending/platform/forge/A.class diamondvending/platform/fabric/B.class
expect fail "forge jar with fabric classes" 1.20.1-forge "$work/leakff"
make_jar "$(libs goodf 1.20.1-forge)/diamondvending-forge-0.1.0+1.20.1.jar" \
  diamondvending/DiamondVending.class diamondvending/platform/forge/A.class META-INF/mods.toml
expect pass "clean forge jar" 1.20.1-forge "$work/goodf"

# 7. A clean jar (plus its sources jar) passes
make_jar "$(libs good 26.1-fabric)/diamondvending-fabric-0.1.0+26.1.2.jar" \
  diamondvending/DiamondVending.class diamondvending/platform/fabric/A.class fabric.mod.json
make_jar "$(libs good 26.1-fabric)/diamondvending-fabric-0.1.0+26.1.2-sources.jar" diamondvending/DiamondVending.java
expect pass "clean fabric jar" 26.1-fabric "$work/good"

if [ "$failures" -ne 0 ]; then
  echo "$failures check(s) failed"
  exit 1
fi
echo "all checks passed"
