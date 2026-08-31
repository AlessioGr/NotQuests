#!/usr/bin/env python3
"""
Analyze a NotQuests E2E server-sweep run.

Two guarantees, both fail the build (exit 1) when violated:

  1. COVERAGE — every objective/action/condition/trigger type registered in the source
     (minus documented integration-gated and variable-based types) has at least one test command
     in commands.txt. A newly added type that ships without a test command fails CI automatically.

  2. CORRECTNESS — no command may produce a parser/handler error, exception or stack trace, except
     commands explicitly annotated as tolerated (# PLAYER-ONLY / # NEEDS-ECONOMY / # UNSURE /
     # OBJECTIVE-SCOPED), which only run for completeness.

Attribution does NOT rely on echo markers (the server defers `say` output, so markers don't
interleave with command output). Instead, Brigadier prints every parse failure with the offending
command via a "...<command><--[HERE]" line, which we match back to commands.txt. Exceptions / stack
traces / custom "Cannot parse" errors have no such echo and are always treated as failures.

Usage: analyze.py <server.log>
"""
import re
import sys
import pathlib
import json
import os

E2E = pathlib.Path(__file__).resolve().parent


def repo_root():
    current = E2E
    for candidate in (current, *current.parents):
        if (candidate / "settings.gradle.kts").exists():
            return candidate
    raise RuntimeError("Cannot locate NotQuests repository root")


REPO = repo_root()
PLATFORM = os.environ.get("NQ_E2E_PLATFORM", "unknown")
SRC = REPO / "src" / "builtin" / "src" / "main" / "java"
CMDS_FILE = pathlib.Path(os.environ.get("NQ_E2E_COMMANDS", E2E / "commands.txt"))
GENERATED = pathlib.Path(os.environ.get(
    "NQ_E2E_GENERATED",
    REPO / "src" / "paper" / "run" / "plugins" / "NotQuests" / "generated"))
RESPONSES_FILE = os.environ.get("NQ_E2E_RESPONSES", "")
EXPECTED_RESPONSES = int(os.environ.get("NQ_E2E_EXPECTED_RESPONSES", "0") or "0")
COMMAND_METADATA = GENERATED / "metadata.json"
COMMAND_SCHEMA = GENERATED / "commands.json"

EXCLUDE_INTEGRATION = {
    "EscortNPC", "JobsRebornReachJobLevel", "SlimefunResearch",
    "TownyNationReachTownCount", "TownyReachResidentCount", "TownyNationName",
    "UltimateClansClanLevel", "BetonQuestObjectiveStateChange", "BetonQuestFireEvent",
    "BetonQuestFireInlineEvent",
}
EXCLUDE_VARIABLE = {"Number", "String", "Boolean", "List", "ItemStackList"}
TOLERANT_TAGS = ("PLAYER-ONLY", "NEEDS-ECONOMY", "UNSURE", "OBJECTIVE-SCOPED")

HERE = re.compile(r"<--\[HERE\]\s*$")
TS = re.compile(r"^\[\d\d:\d\d:\d\d INFO\]:\s*")
# echo-less hard errors (crashes / custom parser failures) — never expected, always fail.
# Stack-frame lines ("at com.example...") are skipped separately; flagging the headline is enough.
CRASH = re.compile(r"Cannot parse|NullPointerException|Cannot invoke|\bException\b")
RESPONSE_ERROR = re.compile(
    r"Incorrect argument|Unknown or incomplete|Unknown command|"
    r"Cannot |Could not |No .+ found|does not exist|"
    r"This command can only be used by a Player|"
    r"failed to |failure|NoClassDefFoundError|NullPointerException|Cannot invoke",
    re.IGNORECASE)
BENIGN_RESPONSE_LINES = {
    "No unlock conditions found!",
    "No progress conditions found!",
    "No complete conditions found!",
    "No rewards found!",
}


def registered_types():
    if COMMAND_METADATA.exists():
        registry = json.loads(COMMAND_METADATA.read_text()).get("registry", {})
        return {
            "objective": ids_from_registry(registry.get("objectives", [])),
            "action": ids_from_registry(registry.get("actions", [])),
            "condition": ids_from_registry(registry.get("conditions", [])),
            "trigger": ids_from_registry(registry.get("triggers", [])),
        }

    kinds = {"objective": "registerObjective", "action": "registerAction",
             "condition": "registerCondition", "trigger": "registerTrigger"}
    found = {k: set() for k in kinds}
    pat = {k: re.compile(fn + r'\("([A-Za-z0-9]+)"') for k, fn in kinds.items()}
    for path in SRC.rglob("*.java"):
        text = path.read_text(errors="replace")
        for k, rx in pat.items():
            found[k].update(m.group(1) for m in rx.finditer(text))
    return found


def ids_from_registry(entries):
    if isinstance(entries, dict):
        return set(entries.keys())
    if isinstance(entries, list):
        return {entry["id"] for entry in entries if isinstance(entry, dict) and entry.get("id")}
    return set()


def load_commands():
    out = []
    for raw in CMDS_FILE.read_text().splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        tolerant = any(t in line for t in TOLERANT_TAGS)
        cmd = re.sub(r"\s+#.*$", "", line).strip()
        if cmd.startswith("/"):
            cmd = cmd[1:]
        out.append((cmd, tolerant))
    return out


def echo_command(line):
    s = TS.sub("", line).rstrip()
    s = HERE.sub("", s)
    return s.lstrip(".").strip()


def rcon_responses():
    if not RESPONSES_FILE:
        return []
    path = pathlib.Path(RESPONSES_FILE)
    if not path.exists():
        raise FileNotFoundError(f"RCON response file was not written: {path}")
    responses = []
    for raw in path.read_text(errors="replace").splitlines():
        if not raw.strip():
            continue
        try:
            entry = json.loads(raw)
        except json.JSONDecodeError:
            continue
        responses.append((str(entry.get("command", "")), str(entry.get("response", ""))))
    return responses


def response_for_error_scan(response):
    return "\n".join(
        line for line in response.splitlines()
        if line.strip() not in BENIGN_RESPONSE_LINES)


def main():
    log_path = sys.argv[1] if len(sys.argv) > 1 else "/tmp/nq-e2e-server.log"
    log = pathlib.Path(log_path).read_text(errors="replace").splitlines()
    commands = load_commands()
    tol_by_cmd = {c: t for c, t in commands}
    all_cmds = [c for c, _ in commands]

    # ---------- coverage ----------
    reg = registered_types()
    blob = "\n".join(all_cmds)
    missing = []
    for kind, names in reg.items():
        for n in sorted(names):
            if n in EXCLUDE_INTEGRATION or n in EXCLUDE_VARIABLE:
                continue
            if not re.search(r"(?<![A-Za-z0-9])" + re.escape(n) + r"(?![A-Za-z0-9])", blob):
                missing.append(f"{kind} {n}")

    # ---------- correctness ----------
    ready = any("Done (" in l for l in log)
    real_fails, tolerated = [], []
    response_records = []
    try:
        response_records = rcon_responses()
    except FileNotFoundError as exc:
        real_fails.append(("<harness>", str(exc)))
    if EXPECTED_RESPONSES and len(response_records) != EXPECTED_RESPONSES:
        real_fails.append((
            "<harness>",
            f"RCON response file contains {len(response_records)} response(s), expected exactly {EXPECTED_RESPONSES}."))

    for command, response in response_records:
        checked_response = response_for_error_scan(response)
        if not checked_response or not RESPONSE_ERROR.search(checked_response):
            continue
        tolerant_command = tol_by_cmd.get(command, False)
        rec = (command, response.strip().replace("\n", " | "))
        (tolerated if tolerant_command else real_fails).append(rec)

    for line in log:
        if HERE.search(line) and not response_records:  # Brigadier parse failure (has the command echo)
            # The echo shows the input UP TO the failure cursor (then truncated from the left with
            # "..."), so the visible tail is a SUBSTRING of the command — not necessarily a suffix
            # (mid-command failures cut before the end). Match by containment; with several matches
            # stay conservative: only tolerate if every matching command is tolerated.
            tail = echo_command(line)
            matches = [c for c in all_cmds if tail and tail in c]
            if matches:
                tol = all(tol_by_cmd[c] for c in matches)
                rec = (matches[0], line.strip())
                (tolerated if tol else real_fails).append(rec)
            else:
                real_fails.append((f"<unmatched echo: {tail}>", line.strip()))
        elif CRASH.search(line) and not line.strip().startswith("at "):
            # echo-less crash / custom parser error: never expected, can't be attributed -> fail
            real_fails.append(("<crash / unattributed>", line.strip()))

    # ---------- report ----------
    print("=" * 72)
    print("NotQuests E2E sweep analysis")
    print("=" * 72)
    print(f"platform             : {PLATFORM}")
    print(f"commands file        : {CMDS_FILE}")
    print(f"metadata directory   : {GENERATED}")
    if RESPONSES_FILE:
        print(f"rcon responses       : {RESPONSES_FILE}")
    print(f"server reached READY : {ready}")
    print(f"commands in sweep    : {len(commands)}")
    if response_records:
        print(f"rcon response count  : {len(response_records)}")
    print("registered types     : " + ", ".join(f"{k}={len(v)}" for k, v in reg.items()))
    print(f"tolerated edge errors: {len(tolerated)}")
    print()
    if missing:
        print(f"COVERAGE GAP — {len(missing)} registered type(s) with no test command:")
        for m in missing:
            print(f"   - {m}")
        print()
    if real_fails:
        print(f"FAILURES — {len(real_fails)} unexpected error(s):")
        for cmd, line in real_fails:
            print(f"   {cmd}")
            print(f"       -> {line}")
        print()
    if tolerated:
        print("Tolerated (ran for completeness, errors expected):")
        for cmd, _ in tolerated:
            print(f"   {cmd}")
        print()

    schema_errors = []
    try:
        schema = load_metadata_or_command_schema()
        exported = schema.get("commands", [])
        syntaxes = {entry.get("syntax") for entry in exported}
        if not exported:
            schema_errors.append("schema contains no commands")
        if "/nqa debug exportCommandSchema" not in syntaxes:
            schema_errors.append("schema is missing /nqa debug exportCommandSchema")
        if "/nqa debug exportMetadata" not in syntaxes:
            schema_errors.append("schema is missing /nqa debug exportMetadata")
        if COMMAND_METADATA.exists():
            metadata = json.loads(COMMAND_METADATA.read_text())
            registry = metadata.get("registry", {})
            for key in ("objectives", "actions", "conditions", "triggers", "variables"):
                if not registry.get(key):
                    schema_errors.append(f"metadata registry has no {key}")
        else:
            schema_errors.append(f"metadata file was not written: {COMMAND_METADATA}")
        for entry in exported:
            for segment in entry.get("segments", []):
                if weak_description(segment.get("description"), segment.get("name"), segment.get("token")):
                    schema_errors.append(
                        "weak command-segment description in "
                        + str(entry.get("syntax"))
                        + ": "
                        + str(segment.get("token"))
                        + " -> "
                        + repr(segment.get("description")))
            for flag in entry.get("flags", []):
                if weak_description(flag.get("description"), flag.get("name"), flag.get("token")):
                    schema_errors.append(
                        "weak flag description in "
                        + str(entry.get("syntax"))
                        + ": --"
                        + str(flag.get("name"))
                        + " -> "
                        + repr(flag.get("description")))
    except FileNotFoundError:
        schema_errors.append(f"schema file was not written: {COMMAND_METADATA}")
    except json.JSONDecodeError as exc:
        schema_errors.append(f"schema JSON is invalid: {exc}")
    if schema_errors:
        print("COMMAND SCHEMA ERROR:")
        for error in schema_errors:
            print(f"   - {error}")
        print()

    ok = ready and not missing and not real_fails and not schema_errors
    print("RESULT:", "PASS" if ok else "FAIL")
    sys.exit(0 if ok else 1)


def weak_description(description, name, token):
    text = str(description or "").strip()
    if not text or text == "Variable Name":
        return True
    weak_phrases = {
        "adds a new entry in this command branch.",
        "checks the selected value or condition.",
        "creates a new entry.",
        "deletes the selected entry.",
        "lists matching entries.",
        "opens edit commands for the selected entry.",
        "optional command flags for this command.",
        "removes all entries in this command branch.",
        "removes the selected entry or value.",
        "sets a new value.",
        "shows detailed information about the selected entry.",
        "shows or changes the category assigned to this entry.",
        "shows the current value.",
        "shows, sets, or removes user-facing description text.",
    }
    if text.lower() in weak_phrases:
        return True
    token_text = str(token or "").strip().replace("[", "").replace("]", "").replace("<", "").replace(">", "")
    candidates = {str(name or "").strip(), token_text}
    return text.lower() in {candidate.lower() for candidate in candidates if candidate}


def load_metadata_or_command_schema():
    if COMMAND_METADATA.exists():
        metadata = json.loads(COMMAND_METADATA.read_text())
        commands = metadata.get("commands", {})
        if isinstance(commands, dict):
            return commands
        return {"commands": commands}
    return json.loads(COMMAND_SCHEMA.read_text())


if __name__ == "__main__":
    main()
