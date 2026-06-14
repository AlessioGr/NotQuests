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

E2E = pathlib.Path(__file__).resolve().parent
SRC = E2E.parent / "paper" / "src" / "main" / "java"
CMDS_FILE = E2E / "commands.txt"

EXCLUDE_INTEGRATION = {
    "EscortNPC", "JobsRebornReachJobLevel", "SlimefunResearch", "ReachLocation",
    "TownyNationReachTownCount", "TownyReachResidentCount", "TownyNationName",
    "UltimateClansClanLevel",
}
EXCLUDE_VARIABLE = {"Number", "String", "Boolean", "List", "ItemStackList"}
TOLERANT_TAGS = ("PLAYER-ONLY", "NEEDS-ECONOMY", "UNSURE", "OBJECTIVE-SCOPED")

HERE = re.compile(r"<--\[HERE\]\s*$")
TS = re.compile(r"^\[\d\d:\d\d:\d\d INFO\]:\s*")
# echo-less hard errors (crashes / custom parser failures) — never expected, always fail.
# Stack-frame lines ("at com.example...") are skipped separately; flagging the headline is enough.
CRASH = re.compile(r"Cannot parse|NullPointerException|Cannot invoke|\bException\b")


def registered_types():
    kinds = {"objective": "registerObjective", "action": "registerAction",
             "condition": "registerCondition", "trigger": "registerTrigger"}
    found = {k: set() for k in kinds}
    pat = {k: re.compile(fn + r'\("([A-Za-z0-9]+)"') for k, fn in kinds.items()}
    for path in SRC.rglob("*.java"):
        text = path.read_text(errors="replace")
        for k, rx in pat.items():
            found[k].update(m.group(1) for m in rx.finditer(text))
    return found


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
    for line in log:
        if HERE.search(line):                       # Brigadier parse failure (has the command echo)
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
    print(f"server reached READY : {ready}")
    print(f"commands in sweep    : {len(commands)}")
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

    ok = ready and not missing and not real_fails
    print("RESULT:", "PASS" if ok else "FAIL")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
