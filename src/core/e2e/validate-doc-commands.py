#!/usr/bin/env python3
"""Validate command examples in the NotQuests docs against generated command schemas."""
import argparse
import json
import pathlib
import re
import shlex
import sys

COMMAND_ROOTS = ("/q", "/qa", "/nq", "/nqa", "/notquests", "/notquestsadmin")
INLINE_CODE = re.compile(r"`([^`\n]+)`")
FENCED_CODE = re.compile(r"```[^\n]*\n(.*?)```", re.DOTALL)


def command_examples(docs_dir):
    examples = []
    for path in sorted(pathlib.Path(docs_dir).rglob("*")):
        if path.suffix.lower() not in {".md", ".mdx"}:
            continue
        if "command-reference" in path.name:
            continue
        text = path.read_text(encoding="utf-8", errors="replace")
        without_fences = FENCED_CODE.sub("", text)
        for match in INLINE_CODE.finditer(without_fences):
            collect_commands(match.group(1), path, examples)
        for block in FENCED_CODE.finditer(text):
            for line in block.group(1).splitlines():
                collect_commands(line.strip(), path, examples)
    return examples


def collect_commands(raw, path, examples):
    if raw.strip().startswith("@optional-integration"):
        return
    analysis_raw = raw.split("=>", 1)[0] if "=>" in raw else raw
    for part in split_command_fragments(analysis_raw):
        command = part.strip()
        if not command.startswith(COMMAND_ROOTS):
            continue
        command = command.split("...", 1)[0].strip()
        command = command.removeprefix("/")
        command = command.replace("<true|false>", "true")
        command = re.sub(r"\[[^\]]+\]", "value", command)
        command = re.sub(r"<([^>\s]+)>", r"\1", command)
        command = command.strip()
        if command:
            examples.append((path, command, raw))


def split_command_fragments(raw):
    starts = []
    for match in re.finditer(r"(?=(/(?:q|qa|nq|nqa|notquests|notquestsadmin)\b))", raw):
        start = match.start()
        if start > 0 and re.match(r"[A-Za-z0-9_.-]", raw[start - 1]):
            continue
        starts.append(start)
    if not starts:
        return []
    starts.append(len(raw))
    fragments = []
    for index, start in enumerate(starts[:-1]):
        end = starts[index + 1]
        fragment = raw[start:end].strip()
        fragment = re.split(r"\s+(?:and|or)\s*$", fragment, maxsplit=1)[0].strip()
        fragment = fragment.rstrip(".,;")
        if fragment:
            fragments.append(fragment)
    return fragments


def load_schema(path):
    data = json.loads(pathlib.Path(path).read_text(encoding="utf-8"))
    commands = data.get("commands", data)
    if isinstance(commands, dict):
        commands = commands.get("commands", [])
    return [compile_command(command) for command in commands or []]


def compile_command(command):
    root = command.get("root", "")
    roots = {root, *(command.get("rootAliases") or [])}
    segments = command.get("segments") or []
    flags = {flag.get("name", "").lower(): flag for flag in command.get("flags") or []}
    return {"syntax": command.get("syntax", ""), "roots": roots, "segments": segments, "flags": flags}


def tokenize(command):
    lexer = shlex.shlex(command, posix=True)
    lexer.whitespace_split = True
    lexer.commenters = ""
    try:
        return list(lexer)
    except ValueError:
        return command.split()


def matches_any(command, schema):
    tokens = tokenize(command)
    if not tokens:
        return False
    return any(matches(tokens, candidate, allow_prefix=False) for candidate in schema) or any(
        matches(tokens, candidate, allow_prefix=True) for candidate in schema)


def matches(tokens, candidate, allow_prefix=False):
    segments = candidate["segments"]
    if len(tokens) < len([segment for segment in segments if segment.get("kind") == "literal"]):
        if not allow_prefix:
            return False
    index = 0
    for segment_index, segment in enumerate(segments):
        if index >= len(tokens):
            return allow_prefix
        token = tokens[index]
        if segment_index == 0:
            if token not in candidate["roots"]:
                return False
            index += 1
            continue
        if segment.get("kind") == "literal":
            if token.lower() != str(segment.get("name", "")).lower():
                return False
            index += 1
            continue
        remaining_required = sum(1 for later in segments[segment_index + 1:] if later.get("kind") != "literal")
        remaining_literals = [later for later in segments[segment_index + 1:] if later.get("kind") == "literal"]
        if is_greedy_argument(segment) and not remaining_literals:
            while index < len(tokens) and not tokens[index].startswith("--"):
                if remaining_required and len([t for t in tokens[index:] if not t.startswith("--")]) <= remaining_required:
                    break
                index += 1
            if index < len(tokens) and not tokens[index].startswith("--"):
                index += 1
        else:
            index += 1

    if allow_prefix and index == len(tokens):
        return True
    while index < len(tokens):
        token = tokens[index]
        if not token.startswith("--"):
            return False
        flag = candidate["flags"].get(token[2:].lower())
        if flag is None:
            return False
        index += 1
        if not flag.get("presenceOnly"):
            if index >= len(tokens):
                return False
            index += 1
    return True


def is_greedy_argument(segment):
    argument_type = str(segment.get("argumentType") or "").upper()
    value_type = str(segment.get("valueType") or "").lower()
    return "GREEDY" in argument_type or value_type in {"text", "message", "command", "task description"}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--docs", default="../notquests-docs/docs")
    parser.add_argument("--schema", action="append", required=True)
    args = parser.parse_args()

    examples = command_examples(args.docs)
    schemas = [(path, load_schema(pathlib.Path(path))) for path in args.schema]
    failures = []
    for source, command, raw in examples:
        for schema_path, schema in schemas:
            if not matches_any(command, schema):
                failures.append((schema_path, source, command, raw))

    print("=" * 72)
    print("NotQuests docs command validation")
    print("=" * 72)
    print(f"docs directory      : {pathlib.Path(args.docs).resolve()}")
    print(f"examples discovered : {len(examples)}")
    for schema_path, _ in schemas:
        print(f"schema              : {schema_path}")
    print()
    if failures:
        print(f"FAILURES — {len(failures)} docs command example(s) are not in a schema:")
        for schema_path, source, command, raw in failures[:80]:
            print(f"   {source}: {command}")
            print(f"      schema: {schema_path}")
            print(f"      raw   : {raw}")
        if len(failures) > 80:
            print(f"   ... {len(failures) - 80} more")
        print("RESULT: FAIL")
        return 1
    print("RESULT: PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
