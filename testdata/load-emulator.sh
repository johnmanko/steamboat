#!/usr/bin/env bash
# Load testdata/inbox.json onto the connected emulator.
set -euo pipefail
cd "$(dirname "$0")"
exec python3 ./load-emulator.py
