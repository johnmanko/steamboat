#!/usr/bin/env bash
# Copy testdata/images.json into the emulator photo library.
set -euo pipefail
cd "$(dirname "$0")"
exec python3 ./load-images.py
