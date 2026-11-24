#!/usr/bin/env python3
"""Load testdata/inbox.json into a running emulator.

Contacts go into the device address book. Threads and messages go into
Steamboat's database, including any picture named by a message. Running
this again skips rows that are already there.
"""

import json
import shlex
import subprocess
import sys
import time
from pathlib import Path

PACKAGE = "com.johnmanko.app.steamboat"
ROOT = Path(__file__).resolve().parent
INBOX = ROOT / "inbox.json"


def adb(*args: str, input_text: str | None = None, check: bool = True) -> str:
    result = subprocess.run(
        ["adb", *args],
        input=input_text,
        text=True,
        capture_output=True,
    )
    if check and result.returncode != 0:
        detail = (result.stderr or result.stdout).strip()
        raise SystemExit(f"adb {' '.join(args)} failed: {detail}")
    return result.stdout


def match_key(number: str) -> str:
    digits = "".join(ch for ch in number if ch.isdigit())
    digits = digits.lstrip("0")
    if len(digits) > 10:
        digits = digits[-10:]
    return digits


def mime_for(path: str) -> str:
    lower = path.lower()
    if lower.endswith(".png"):
        return "image/png"
    if lower.endswith((".jpg", ".jpeg")):
        return "image/jpeg"
    if lower.endswith((".mp4", ".webm")):
        return "video/mp4"
    return "application/octet-stream"


def sql_quote(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def device_ready() -> None:
    listed = adb("devices")
    serials = [
        line.split()[0]
        for line in listed.splitlines()
        if "\tdevice" in line
    ]
    if not serials:
        raise SystemExit("No emulator is connected. Start one, then run this again.")
    path = adb("shell", "pm", "path", PACKAGE, check=False)
    if PACKAGE not in path:
        raise SystemExit(f"{PACKAGE} is not installed. Install the debug build first.")


def existing_numbers() -> set[str]:
    raw = adb(
        "shell",
        "content",
        "query",
        "--uri",
        "content://com.android.contacts/data/phones",
        "--projection",
        "data1",
        check=False,
    )
    return {match_key(line) for line in raw.splitlines() if match_key(line)}


def insert_contact(name: str, number: str) -> None:
    inserted = adb(
        "shell",
        "content",
        "insert",
        "--uri",
        "content://com.android.contacts/raw_contacts",
        "--bind",
        "account_type:n:",
        "--bind",
        "account_name:n:",
    )
    raw_id = inserted.strip().rstrip("/").split("/")[-1]
    if not raw_id.isdigit():
        raise SystemExit(f"Could not create a contact for {name}: {inserted.strip()}")
    adb(
        "shell",
        "content",
        "insert",
        "--uri",
        "content://com.android.contacts/data",
        "--bind",
        f"raw_contact_id:i:{raw_id}",
        "--bind",
        "mimetype:s:vnd.android.cursor.item/name",
        "--bind",
        f"data1:s:{name}",
    )
    adb(
        "shell",
        "content",
        "insert",
        "--uri",
        "content://com.android.contacts/data",
        "--bind",
        f"raw_contact_id:i:{raw_id}",
        "--bind",
        "mimetype:s:vnd.android.cursor.item/phone_v2",
        "--bind",
        f"data1:s:{number}",
        "--bind",
        "data2:i:2",
    )


def copy_media(relative: str) -> str:
    source = ROOT / relative
    if not source.is_file():
        raise SystemExit(f"Missing attachment {source}")
    data_dir = adb("shell", "run-as", PACKAGE, "pwd").strip()
    remote_name = source.name
    remote = f"mkdir -p files/media && cat > files/media/{remote_name}"
    subprocess.run(
        ["adb", "shell", f"run-as {PACKAGE} sh -c {shlex.quote(remote)}"],
        input=source.read_bytes(),
        check=True,
    )
    return f"{data_dir}/files/media/{remote_name}"


def load_messages(contacts: dict[str, str], messages: list[dict]) -> None:
    now = int(time.time() * 1000)
    statements = ["BEGIN;"]
    touched: set[str] = set()
    for message in messages:
        number = message["number"]
        key = match_key(number)
        if not key:
            raise SystemExit(f"No digits in {number}")
        name = contacts.get(key, number)
        minutes = int(message["minutesAgo"])
        sent_at = now - minutes * 60_000
        provider_key = f"sample:{key}:{minutes}"
        media_path = None
        media_mime = None
        if message.get("media"):
            media_path = copy_media(message["media"])
            media_mime = mime_for(str(message["media"]))
        body = message.get("body") or ""
        outgoing = 1 if message.get("outgoing") else 0
        seen = 1 if message.get("seen") or message.get("outgoing") else 0
        status = "sent" if outgoing else "received"
        touched.add(key)
        statements.append(
            "INSERT INTO conversations (address, matchKey, title, photoUri, contactLookupKey, contactLinkedManually, snippet, updatedAt, unreadCount) "
            f"SELECT {sql_quote(number)}, {sql_quote(key)}, {sql_quote(name)}, NULL, NULL, 0, '', 0, 0 "
            f"WHERE NOT EXISTS (SELECT 1 FROM conversations WHERE matchKey = {sql_quote(key)});"
        )
        statements.append(
            "INSERT INTO messages (conversationId, body, sentAt, outgoing, status, seen, providerKey, mediaPath, mediaMime) "
            "SELECT c.id, "
            f"{sql_quote(body)}, {sent_at}, {outgoing}, {sql_quote(status)}, {seen}, {sql_quote(provider_key)}, "
            f"{sql_quote(media_path) if media_path else 'NULL'}, {sql_quote(media_mime) if media_mime else 'NULL'} "
            "FROM conversations c "
            f"WHERE c.matchKey = {sql_quote(key)} "
            f"AND NOT EXISTS (SELECT 1 FROM messages WHERE providerKey = {sql_quote(provider_key)});"
        )
        if media_path:
            statements.append(
                "UPDATE messages SET "
                f"mediaPath = {sql_quote(media_path)}, mediaMime = {sql_quote(media_mime)} "
                f"WHERE providerKey = {sql_quote(provider_key)};"
            )
    keys = ", ".join(sql_quote(key) for key in sorted(touched))
    statements.append(
        "UPDATE conversations SET "
        "snippet = (SELECT CASE "
        "WHEN body != '' THEN body "
        "WHEN mediaMime LIKE 'video/%' THEN 'Video' "
        "WHEN mediaPath IS NOT NULL THEN 'Photo' "
        "ELSE '' END FROM messages WHERE conversationId = conversations.id "
        "ORDER BY sentAt DESC, id DESC LIMIT 1), "
        "updatedAt = (SELECT MAX(sentAt) FROM messages WHERE conversationId = conversations.id), "
        "unreadCount = (SELECT COUNT(*) FROM messages WHERE conversationId = conversations.id "
        "AND outgoing = 0 AND seen = 0) "
        f"WHERE matchKey IN ({keys});"
    )
    statements.append("COMMIT;")
    adb("shell", "am", "force-stop", PACKAGE)
    adb(
        "shell",
        "run-as",
        PACKAGE,
        "sqlite3",
        "databases/steamboat.db",
        input_text="\n".join(statements),
    )


def main() -> None:
    device_ready()
    inbox = json.loads(INBOX.read_text())
    known = existing_numbers()
    added = 0
    by_key = {}
    for contact in inbox["contacts"]:
        key = match_key(contact["number"])
        by_key[key] = contact["name"]
        if key in known:
            continue
        insert_contact(contact["name"], contact["number"])
        added += 1
    load_messages(by_key, inbox["messages"])
    adb("shell", "am", "start", "-n", f"{PACKAGE}/.MainActivity")
    print(
        f"Applied inbox.json: {len(inbox['contacts'])} contacts ({added} new in the address book) "
        f"and {len(inbox['messages'])} messages. Rows already on the device were left in place."
    )


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        sys.exit(1)
