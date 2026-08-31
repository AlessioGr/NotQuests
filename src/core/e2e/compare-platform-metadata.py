#!/usr/bin/env python3
"""Compare generated Paper and NeoForge NotQuests metadata for portable feature parity."""

import argparse
import difflib
import json
import pathlib
import sys


REGISTRY_KEYS = ("objectives", "actions", "conditions", "triggers", "variables")
CAPABILITY_GATED_REGISTRY_IDS = {"variables": {"Permission"}}
CAPABILITY_GATED_COMMAND_WORDS = {"Permission"}


def load_metadata(path: pathlib.Path) -> dict:
    if not path.exists():
        raise FileNotFoundError(f"metadata file does not exist: {path}")
    return json.loads(path.read_text())


def comparable_registry(metadata: dict) -> dict:
    registry = metadata.get("registry", {})
    comparable = {}
    for key in REGISTRY_KEYS:
        entries = []
        for entry in registry.get(key, []):
            if not isinstance(entry, dict):
                continue
            if entry.get("integrationOnly"):
                continue
            if entry.get("id") in CAPABILITY_GATED_REGISTRY_IDS.get(key, set()):
                continue
            entries.append(sort_nested_lists(entry))
        comparable[key] = sorted(entries, key=lambda entry: entry.get("id", ""))
    return comparable


def comparable_commands(metadata: dict) -> list[dict]:
    commands = metadata.get("commands", {})
    if isinstance(commands, dict):
        commands = commands.get("commands", [])
    return sorted(
        (sort_nested_lists(command) for command in commands if not capability_gated_command(command)),
        key=lambda command: command.get("syntax", ""))


def capability_gated_command(command: dict) -> bool:
    return bool(CAPABILITY_GATED_COMMAND_WORDS.intersection(command.get("syntax", "").split()))


def sort_nested_lists(value):
    if isinstance(value, dict):
        return {key: sort_nested_lists(item) for key, item in sorted(value.items())}
    if isinstance(value, list):
        if all(isinstance(item, dict) and "name" in item for item in value):
            return sorted((sort_nested_lists(item) for item in value), key=lambda item: item.get("name", ""))
        if all(isinstance(item, dict) and "id" in item for item in value):
            return sorted((sort_nested_lists(item) for item in value), key=lambda item: item.get("id", ""))
        return [sort_nested_lists(item) for item in value]
    return value


def ids(entries: list[dict]) -> set[str]:
    return {entry.get("id", "") for entry in entries if entry.get("id")}


def report_registry_set_diffs(paper_registry: dict, neoforge_registry: dict) -> list[str]:
    errors = []
    for key in REGISTRY_KEYS:
        paper_ids = ids(paper_registry.get(key, []))
        neoforge_ids = ids(neoforge_registry.get(key, []))
        paper_only = sorted(paper_ids - neoforge_ids)
        neoforge_only = sorted(neoforge_ids - paper_ids)
        if paper_only or neoforge_only:
            errors.append(
                f"{key}: paper-only={paper_only or '[]'} neoforge-only={neoforge_only or '[]'}")
    return errors


def report_registry_value_diffs(paper_registry: dict, neoforge_registry: dict) -> list[str]:
    errors = []
    for key in REGISTRY_KEYS:
        paper_entries = {entry.get("id", ""): entry for entry in paper_registry.get(key, []) if entry.get("id")}
        neoforge_entries = {
            entry.get("id", ""): entry for entry in neoforge_registry.get(key, []) if entry.get("id")
        }
        for entry_id in sorted(set(paper_entries) & set(neoforge_entries)):
            diff = first_diff(f"{key} {entry_id}", paper_entries[entry_id], neoforge_entries[entry_id])
            if diff:
                errors.append(f"{key} metadata differs for {entry_id}:\n{diff}")
                break
    return errors


def report_command_diffs(paper_commands: list[dict], neoforge_commands: list[dict]) -> list[str]:
    paper_by_syntax = {command.get("syntax", ""): command for command in paper_commands}
    neoforge_by_syntax = {command.get("syntax", ""): command for command in neoforge_commands}
    paper_syntaxes = set(paper_by_syntax)
    neoforge_syntaxes = set(neoforge_by_syntax)
    paper_only = sorted(paper_syntaxes - neoforge_syntaxes)
    neoforge_only = sorted(neoforge_syntaxes - paper_syntaxes)
    errors = []
    if paper_only or neoforge_only:
        errors.append(
            "commands: "
            + f"paper-only={paper_only[:20] or '[]'}"
            + (" ..." if len(paper_only) > 20 else "")
            + f" neoforge-only={neoforge_only[:20] or '[]'}"
            + (" ..." if len(neoforge_only) > 20 else ""))
    for syntax in sorted(paper_syntaxes & neoforge_syntaxes):
        diff = first_diff(f"command {syntax}", paper_by_syntax[syntax], neoforge_by_syntax[syntax])
        if diff:
            errors.append(f"command metadata differs for {syntax}:\n{diff}")
            break
    return errors


def first_diff(label: str, paper_value, neoforge_value) -> str | None:
    if paper_value == neoforge_value:
        return None
    paper_lines = json.dumps(paper_value, indent=2, sort_keys=True).splitlines()
    neoforge_lines = json.dumps(neoforge_value, indent=2, sort_keys=True).splitlines()
    diff = difflib.unified_diff(
        paper_lines,
        neoforge_lines,
        fromfile=f"paper {label}",
        tofile=f"neoforge {label}",
        lineterm="")
    return "\n".join(list(diff)[:80])


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--paper",
        default="src/paper/run/plugins/NotQuests/generated/metadata.json",
        type=pathlib.Path)
    parser.add_argument(
        "--neoforge",
        default="src/neoforge/run/world/notquests/generated/metadata.json",
        type=pathlib.Path)
    args = parser.parse_args()

    paper = load_metadata(args.paper)
    neoforge = load_metadata(args.neoforge)
    paper_registry = comparable_registry(paper)
    neoforge_registry = comparable_registry(neoforge)
    paper_commands = comparable_commands(paper)
    neoforge_commands = comparable_commands(neoforge)

    errors = report_registry_set_diffs(paper_registry, neoforge_registry)
    errors.extend(report_registry_value_diffs(paper_registry, neoforge_registry))
    errors.extend(report_command_diffs(paper_commands, neoforge_commands))

    print("=" * 72)
    print("NotQuests Paper/NeoForge parity check")
    print("=" * 72)
    print(f"paper metadata      : {args.paper}")
    print(f"neoforge metadata   : {args.neoforge}")
    for key in REGISTRY_KEYS:
        print(
            f"{key:<18}: paper={len(paper_registry.get(key, []))} "
            f"neoforge={len(neoforge_registry.get(key, []))}")
    print(f"{'commands':<18}: paper={len(paper_commands)} neoforge={len(neoforge_commands)}")
    print("capability-gated  : Permission variable (Paper supports arbitrary permission nodes)")
    print()

    if errors:
        print("PARITY FAIL")
        for error in errors:
            print()
            print(error)
        return 1

    print("RESULT: PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
