#!/usr/bin/env bash
set -uo pipefail

REPO="$(cd "$(dirname "$0")/.." && pwd)"
exec "$REPO/src/core/e2e/run-server-sweep.sh" neoforge
