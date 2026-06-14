#!/usr/bin/env bash
#
# Server-side end-to-end command sweep for NotQuests.
#
# Boots a REAL Paper server (via the run-paper `:plugin:runServer` task) with the freshly built
# plugin, creates a quest, then runs every command in commands.txt through the server console —
# each preceded by an "NQE2E_MK <idx> <STRICT|TOLERANT> <command>" broadcast so analyze.py can map
# every log line back to the command that produced it. Finally analyze.py checks coverage and
# correctness and sets the exit code. No Minecraft client is involved (console only).
#
# Exit 0 = every type covered and every strict command ran cleanly; non-zero otherwise.
set -uo pipefail

E2E="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$E2E/.." && pwd)"
cd "$REPO"

LOG="${E2E_LOG:-/tmp/nq-e2e-server.log}"
FIFO="${E2E_FIFO:-/tmp/nq-e2e.fifo}"
RUN="$REPO/plugin/run"
CMDS="$E2E/commands.txt"
BOOT_TIMEOUT_STEPS="${E2E_BOOT_STEPS:-240}"   # x2s = 8 min max for first-time Paper download + build

cleanup() { kill "${HOLDER:-}" "${GPID:-}" 2>/dev/null; rm -f "$FIFO"; }
trap cleanup EXIT

rm -f "$LOG" "$FIFO"
mkfifo "$FIFO"
mkdir -p "$RUN"
echo "eula=true" > "$RUN/eula.txt"
# Fresh, deterministic state every run: a stale/half-written world from a previously killed server
# can hang the next boot, and old plugin data makes "already exists" noise. Both regenerate.
rm -rf "$RUN/world" "$RUN/world_nether" "$RUN/world_the_end" "$RUN/plugins/NotQuests" 2>/dev/null

# Fast, deterministic, offline boot: no auth round-trips, tiny flat world, no spawn protection.
cat > "$RUN/server.properties" <<'PROPS'
online-mode=false
level-type=minecraft:flat
generate-structures=false
spawn-protection=0
spawn-npcs=false
spawn-animals=false
spawn-monsters=false
view-distance=3
simulation-distance=3
max-players=5
allow-nether=false
allow-end=false
PROPS

echo "DRIVER_START"
# Hold the FIFO write end open so the server console never sees EOF.
tail -f /dev/null > "$FIFO" &
HOLDER=$!
./gradlew :plugin:runServer --console=plain --no-daemon < "$FIFO" > "$LOG" 2>&1 &
GPID=$!

state=TIMEOUT
for i in $(seq 1 "$BOOT_TIMEOUT_STEPS"); do
  if grep -q 'Done (' "$LOG" 2>/dev/null; then state=READY; break; fi
  if grep -qiE 'Error occurred while enabling|BUILD FAILED|Could not resolve dependencies' "$LOG" 2>/dev/null; then state=ENABLE_FAIL; break; fi
  if ! kill -0 "$GPID" 2>/dev/null; then state=GRADLE_DIED; break; fi
  sleep 2
done
echo "BOOT_STATE=$state after ~$((i*2))s"

if [ "$state" != "READY" ]; then
  echo "::error:: server never reached READY ($state). Last 40 log lines:"
  tail -40 "$LOG"
  exit 1
fi

# ---- drive the console ----
# No echo markers: the server defers `say` output so it doesn't interleave with command output.
# analyze.py attributes each failure via Brigadier's own "...<command><--[HERE]" echo instead.
{
  echo "qa create TestQuest"
  while IFS= read -r raw; do
    line="${raw%%$'\r'}"
    case "$line" in ''|'#'*) continue;; esac          # skip blanks and full-line comments
    cmd="$(printf '%s' "$line" | sed -E 's/[[:space:]]+#.*$//')"   # drop trailing "# annotation"
    [ -z "$cmd" ] && continue
    printf '%s\n' "$cmd"
  done < "$CMDS"
} > "$FIFO"

# Deterministic completion instead of a fixed sleep: console commands execute in order, so once
# save-all's "Saved the game" appears, every sweep command before it has run. Timeout as fallback.
echo "save-all flush" > "$FIFO"
for i in $(seq 1 60); do
  grep -q 'Saved the game' "$LOG" 2>/dev/null && break
  sleep 1
done

echo "=== SWEEP DONE; analyzing ==="
# Analyze before teardown so the exit code is produced even if shutdown is slow.
python3 "$E2E/analyze.py" "$LOG"; rc=$?

# Graceful teardown: ask the server to stop and wait for the gradle process to exit, so no orphaned
# server JVM lingers (an abruptly killed server can leave a half-written world that hangs the next
# boot). The EXIT trap force-kills only as a last resort.
echo "stop" > "$FIFO" 2>/dev/null
for i in $(seq 1 30); do
  kill -0 "$GPID" 2>/dev/null || break
  sleep 1
done
exit "$rc"
