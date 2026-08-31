#!/usr/bin/env bash
#
# Shared real-server command sweep for NotQuests.
#
# Usage:
#   src/core/e2e/run-server-sweep.sh paper
#   src/core/e2e/run-server-sweep.sh neoforge
#
# The command list is platform-neutral and lives next to this script. Platform modules only provide
# the real server process and generated-metadata location.
set -uo pipefail

CORE_E2E="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$CORE_E2E/../../.." && pwd)"
cd "$REPO"

PLATFORM="${1:-paper}"
CMDS="${NQ_E2E_COMMANDS:-$CORE_E2E/commands.txt}"
BOOT_TIMEOUT_STEPS="${E2E_BOOT_STEPS:-240}" # x2s = 8 min max for first-time downloads + build

case "$PLATFORM" in
  paper)
    LOG="${E2E_LOG:-/tmp/nq-e2e-paper-server.log}"
    FIFO="${E2E_FIFO:-/tmp/nq-e2e-paper.fifo}"
    RUN="$REPO/src/paper/run"
    GRADLE_TASK=":paper:runServer"
    GENERATED="$RUN/plugins/NotQuests/generated"
    DRIVE_MODE="rcon"
    RCON_PORT="${E2E_RCON_PORT:-25575}"
    RCON_PASSWORD="${E2E_RCON_PASSWORD:-notquests-e2e}"
    RESPONSES="${E2E_RESPONSES:-/tmp/nq-e2e-paper-rcon.jsonl}"
    CLEAN_PATHS=(
      "$RUN/world" "$RUN/world_nether" "$RUN/world_the_end"
      "$RUN/plugins/NotQuests"
      "$RUN/plugins/BetonQuest"
      "$RUN/plugins/BetonQuest-3.0.0.jar"
      "$RUN/plugins/BetonQuest-3.2.0.jar"
      "$RUN/plugins/BetonQuest.jar"
    )
    ;;
  neoforge)
    LOG="${E2E_LOG:-/tmp/nq-e2e-neoforge-server.log}"
    FIFO="${E2E_FIFO:-/tmp/nq-e2e-neoforge.fifo}"
    RUN="$REPO/src/neoforge/run"
    GRADLE_TASK=":neoforge:runServer"
    GENERATED="$RUN/world/notquests/generated"
    DRIVE_MODE="rcon"
    RCON_PORT="${E2E_RCON_PORT:-25575}"
    RCON_PASSWORD="${E2E_RCON_PASSWORD:-notquests-e2e}"
    RESPONSES="${E2E_RESPONSES:-/tmp/nq-e2e-neoforge-rcon.jsonl}"
    CLEAN_PATHS=(
      "$RUN/world"
      "$RUN/notquests"
      "$RUN/config/notquests"
    )
    ;;
  *)
    echo "::error:: unknown NotQuests E2E platform: $PLATFORM"
    exit 2
    ;;
esac

cleanup() {
  kill "${HOLDER:-}" "${GPID:-}" 2>/dev/null || true
  rm -f "$FIFO" "${COMMAND_BATCH:-}"
}
trap cleanup EXIT

if [ ! -f "$CMDS" ]; then
  echo "::error:: command sweep file does not exist: $CMDS"
  exit 1
fi

rm -f "$LOG" "$FIFO" "${RESPONSES:-}"
mkfifo "$FIFO"
mkdir -p "$RUN"
echo "eula=true" > "$RUN/eula.txt"
rm -rf "${CLEAN_PATHS[@]}" 2>/dev/null || true

# Fast, deterministic, offline boot: no auth round-trips, tiny world, no spawn protection.
cat > "$RUN/server.properties" <<PROPS
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
enable-rcon=true
rcon.port=${RCON_PORT:-25575}
rcon.password=${RCON_PASSWORD:-notquests-e2e}
PROPS

echo "DRIVER_START platform=$PLATFORM"
# Hold the FIFO write end open so the server console never sees EOF.
tail -f /dev/null > "$FIFO" &
HOLDER=$!
./gradlew "$GRADLE_TASK" --console=plain --no-daemon < "$FIFO" > "$LOG" 2>&1 &
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
  echo "::error:: $PLATFORM server never reached READY ($state). Last 60 log lines:"
  tail -60 "$LOG"
  exit 1
fi

COMMAND_BATCH="$(mktemp "/tmp/nq-e2e-$PLATFORM-commands.XXXXXX")"
{
  echo "qa create TestQuest"
  cat "$CMDS"
} > "$COMMAND_BATCH"

if [ "$DRIVE_MODE" = "rcon" ]; then
  if ! python3 "$CORE_E2E/send-rcon.py" \
    --port "$RCON_PORT" \
    --password "$RCON_PASSWORD" \
    --commands "$COMMAND_BATCH" \
    --responses "$RESPONSES" >/tmp/nq-e2e-"$PLATFORM"-rcon.out; then
    echo "::error:: failed to send $PLATFORM E2E commands through RCON"
    python3 "$CORE_E2E/send-rcon.py" \
      --port "$RCON_PORT" \
      --password "$RCON_PASSWORD" \
      --command "stop" >/tmp/nq-e2e-"$PLATFORM"-stop-rcon.out 2>/dev/null || true
    exit 1
  fi
  if ! python3 "$CORE_E2E/send-rcon.py" \
    --port "$RCON_PORT" \
    --password "$RCON_PASSWORD" \
    --command "save-all flush" \
    --responses /tmp/nq-e2e-"$PLATFORM"-save-rcon.jsonl >/tmp/nq-e2e-"$PLATFORM"-save-rcon.out; then
    echo "::error:: failed to save $PLATFORM E2E server through RCON"
    python3 "$CORE_E2E/send-rcon.py" \
      --port "$RCON_PORT" \
      --password "$RCON_PASSWORD" \
      --command "stop" >/tmp/nq-e2e-"$PLATFORM"-stop-rcon.out 2>/dev/null || true
    exit 1
  fi
else
  while IFS= read -r raw; do
    line="${raw%%$'\r'}"
    case "$line" in ''|'#'*) continue;; esac
    cmd="$(printf '%s' "$line" | sed -E 's/[[:space:]]+#.*$//')"
    [ -z "$cmd" ] && continue
    printf '%s\n' "$cmd"
  done < "$COMMAND_BATCH" > "$FIFO"
  echo "save-all flush" > "$FIFO"
fi
for i in $(seq 1 90); do
  grep -q 'Saved the game' "$LOG" 2>/dev/null && break
  sleep 1
done

echo "=== SWEEP DONE; analyzing ==="
NQ_E2E_PLATFORM="$PLATFORM" \
NQ_E2E_COMMANDS="$CMDS" \
NQ_E2E_GENERATED="$GENERATED" \
NQ_E2E_EXPECTED_RESPONSES="$(grep -Ev '^[[:space:]]*(#|$)' "$COMMAND_BATCH" | wc -l | tr -d ' ')" \
NQ_E2E_RESPONSES="${RESPONSES:-}" \
python3 "$CORE_E2E/analyze.py" "$LOG"
rc=$?

if [ "$DRIVE_MODE" = "rcon" ]; then
  python3 "$CORE_E2E/send-rcon.py" \
    --port "$RCON_PORT" \
    --password "$RCON_PASSWORD" \
    --command "stop" >/tmp/nq-e2e-"$PLATFORM"-stop-rcon.out 2>/dev/null || true
else
  echo "stop" > "$FIFO" 2>/dev/null || true
fi
for i in $(seq 1 45); do
  kill -0 "$GPID" 2>/dev/null || break
  sleep 1
done
exit "$rc"
