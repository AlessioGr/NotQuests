#!/usr/bin/env bash
#
# Focused server-side E2E sweep for the BetonQuest 3.x integration.
#
# Boots a real Paper server with BetonQuest 3.0.0 installed from src/paper/libs, registers a tiny
# BetonQuest package that uses every restored NotQuests hook, then drives the NotQuests admin
# commands that create BetonQuest-backed actions, rewards, objectives, and variables.
set -euo pipefail

E2E="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$E2E/.." && pwd)"
cd "$REPO"

LOG="${E2E_BETONQUEST_LOG:-/tmp/nq-betonquest-server.log}"
STRIPPED_LOG="${E2E_BETONQUEST_STRIPPED_LOG:-/tmp/nq-betonquest-server.clean.log}"
FIFO="${E2E_BETONQUEST_FIFO:-/tmp/nq-betonquest.fifo}"
RUN="$REPO/src/paper/run"
BETONQUEST_JAR="$REPO/src/paper/libs/BetonQuest-3.0.0.jar"
BOOT_TIMEOUT_STEPS="${E2E_BOOT_STEPS:-240}" # x2s = 8 min max for first-time Paper setup

cleanup() { kill "${HOLDER:-}" "${GPID:-}" 2>/dev/null || true; rm -f "$FIFO"; }
trap cleanup EXIT

if [ ! -f "$BETONQUEST_JAR" ]; then
  echo "::error:: missing vendored BetonQuest test jar: $BETONQUEST_JAR"
  exit 1
fi

rm -f "$LOG" "$STRIPPED_LOG" "$FIFO"
mkfifo "$FIFO"
mkdir -p "$RUN/plugins/BetonQuest/QuestPackages/nqtest"
echo "eula=true" > "$RUN/eula.txt"
cp "$BETONQUEST_JAR" "$RUN/plugins/BetonQuest-3.0.0.jar"

# Keep BetonQuest's async updater out of deterministic CI logs.
(
  cd "$RUN/plugins/BetonQuest"
  jar xf "$BETONQUEST_JAR" config.yml
)
perl -0pi -e 's/(updater:\s*\n\s*enabled:\s*)true/${1}false/' "$RUN/plugins/BetonQuest/config.yml"

cat > "$RUN/plugins/BetonQuest/QuestPackages/nqtest/package.yml" <<'YAML'
actions:
  testAction: "notify BetonQuest action ran"
  notquestsAction: "nq_action SendMessage Hello from BetonQuest"
  notquestsTriggerObjective: "nq_triggerobjective SomeTrigger"
  notquestsStartQuest: "nq_startquest BQQuest -force -silent -notriggers"
  notquestsFailQuest: "nq_failquest BQQuest"
  notquestsAbortQuest: "nq_abortquest BQQuest"
  notquestsQuestPoints: "nq_questpoints add 1 -silent"
conditions:
  testCondition: "permission notquests.e2e.betonquest"
  notquestsCondition: "nq_condition Boolean Flying equals true"
objectives:
  testObjective: "delay 1"
YAML

rm -rf "$RUN/world" "$RUN/world_nether" "$RUN/world_the_end" "$RUN/plugins/NotQuests" 2>/dev/null
cat > "$RUN/server.properties" <<'PROPS'
online-mode=false
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

echo "BETONQUEST_DRIVER_START"
tail -f /dev/null > "$FIFO" &
HOLDER=$!
./gradlew :paper:runServer --console=plain --no-daemon < "$FIFO" > "$LOG" 2>&1 &
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
  echo "::error:: BetonQuest E2E server never reached READY ($state). Last 80 log lines:"
  tail -80 "$LOG"
  exit 1
fi

{
  echo "qa create BQQuest"
  echo "qa actions add BQFire BetonQuestFireEvent nqtest testAction"
  echo "qa actions add BQInline BetonQuestFireInlineEvent notify Inline from NotQuests"
  echo "qa edit BQQuest rewards add BetonQuestFireEvent nqtest testAction"
  echo "qa edit BQQuest rewards add BetonQuestFireInlineEvent notify Inline reward from NotQuests"
  echo "qa edit BQQuest objectives add BetonQuestObjectiveStateChange nqtest testObjective COMPLETED"
  echo "qa variables check BetonQuestCondition nqtest testCondition"
  echo "qa edit BQQuest objectives list"
  echo "qa actions"
  echo "save-all flush"
} > "$FIFO"

for i in $(seq 1 90); do
  grep -q 'Saved the game' "$LOG" 2>/dev/null && break
  sleep 1
done

echo "stop" > "$FIFO" 2>/dev/null || true
for i in $(seq 1 45); do
  kill -0 "$GPID" 2>/dev/null || break
  sleep 1
done

perl -pe 's/\e\[[0-9;]*m//g' "$LOG" > "$STRIPPED_LOG"

assert_log() {
  local pattern="$1"
  local message="$2"
  if ! rg -q "$pattern" "$STRIPPED_LOG"; then
    echo "::error:: $message"
    rg -n 'BetonQuest|BQQuest|Incorrect argument|Unknown or incomplete|Exception|ERROR' "$STRIPPED_LOG" || true
    exit 1
  fi
}

assert_log 'BetonQuest [^ ]+ found\. Enabled BetonQuest support!' 'NotQuests did not enable BetonQuest support.'
assert_log 'Registered BetonQuest interceptor: notquests' 'NotQuests did not register the BetonQuest conversation interceptor.'
assert_log 'There are \[7 Actions, .*2 Conditions, .*1 Objective, .*\] loaded from 1 packages\.' 'BetonQuest did not load the test package with the restored nq_* hooks.'
assert_log 'BetonQuestFireEvent Action with the name BQFire has been created successfully!' 'BetonQuestFireEvent saved-action command failed.'
assert_log 'BetonQuestFireInlineEvent Action with the name BQInline has been created successfully!' 'BetonQuestFireInlineEvent saved-action command failed.'
assert_log 'BetonQuestFireEvent Reward successfully added to Quest BQQuest!' 'BetonQuestFireEvent reward command failed.'
assert_log 'BetonQuestFireInlineEvent Reward successfully added to Quest BQQuest!' 'BetonQuestFireInlineEvent reward command failed.'
assert_log 'BetonQuestObjectiveStateChange Objective successfully added to Quest BQQuest!' 'BetonQuest objective command failed.'
assert_log 'BetonQuestCondition variable \(boolean\) result for player .*: false' 'BetonQuestCondition variable check command failed.'

if rg -n 'Incorrect argument|Unknown or incomplete|Exception|ERROR|Could not pass event|NoClassDefFoundError|zip file closed' "$STRIPPED_LOG"; then
  echo "::error:: BetonQuest E2E sweep produced parser/runtime errors."
  exit 1
fi

echo "RESULT: PASS"
