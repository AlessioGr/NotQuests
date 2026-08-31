#!/usr/bin/env python3
"""Compatibility wrapper for the shared NotQuests server-sweep analyzer."""
import pathlib
import runpy
import sys


def main():
    repo = pathlib.Path(__file__).resolve().parents[1]
    analyzer = repo / "src" / "core" / "e2e" / "analyze.py"
    sys.argv[0] = str(analyzer)
    runpy.run_path(str(analyzer), run_name="__main__")


if __name__ == "__main__":
    main()
