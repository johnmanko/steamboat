#!/usr/bin/env python3
"""Copy testdata/images.json onto the emulator's photo library.

Files land in Pictures/Steamboat and are registered with MediaStore, so the
photo picker can open them. Running this again skips names already there.
"""

import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
CATALOG = ROOT / "images.json"
REMOTE_DIR = "/sdcard/Pictures/Steamboat"


def adb(*args: str, check: bool = True) -> str:
    result = subprocess.run(["adb", *args], text=True, capture_output=True)
    if check and result.returncode != 0:
        detail = (result.stderr or result.stdout).strip()
        raise SystemExit(f"adb {' '.join(args)} failed: {detail}")
    return result.stdout


def device_ready() -> None:
    listed = adb("devices")
    serials = [line.split()[0] for line in listed.splitlines() if "\tdevice" in line]
    if not serials:
        raise SystemExit("No emulator is connected. Start one, then run this again.")


def indexed_names() -> set[str]:
    raw = adb(
        "shell",
        "content",
        "query",
        "--uri",
        "content://media/external/images/media",
        "--projection",
        "_display_name",
        check=False,
    )
    names = set()
    for line in raw.splitlines():
        marker = "_display_name="
        if marker in line:
            names.add(line.split(marker, 1)[1].strip())
    return names


def main() -> None:
    device_ready()
    catalog = json.loads(CATALOG.read_text())
    present = indexed_names()
    adb("shell", "mkdir", "-p", REMOTE_DIR)
    added = 0
    for image in catalog["images"]:
        source = ROOT / image["file"]
        if not source.is_file():
            raise SystemExit(f"Missing image {source}")
        name = source.name
        if name in present:
            continue
        adb("push", str(source), f"{REMOTE_DIR}/{name}")
        added += 1
    adb(
        "shell",
        "content",
        "call",
        "--uri",
        "content://media",
        "--method",
        "scan_volume",
        "--arg",
        "external_primary",
    )
    print(
        f"Applied images.json: {len(catalog['images'])} photos "
        f"({added} new) in {REMOTE_DIR}."
    )


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        sys.exit(1)
