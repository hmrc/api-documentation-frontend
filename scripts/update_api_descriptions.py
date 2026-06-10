#!/usr/bin/env python3
"""
update_api_descriptions.py

Updates api-description-overrides.json with rewritten descriptions sourced
directly from the comparison CSV.

The CSV must have columns:
  - Identifier              service slug (e.g. "agent-authorisation-api")
  - Rewritten Metadata Content  the new description text

Rows where Identifier is empty are skipped (out of scope for this update).

Usage:
    python3 scripts/update_api_descriptions.py <csv_path> <json_path>

Example:
    python3 scripts/update_api_descriptions.py \\
        API_home_page_metadata_rewrite_comparison.csv \\
        resources/api-description-overrides.json
"""

import csv
import json
import os
import sys
import tempfile
from pathlib import Path


def load_updates_from_csv(csv_path: str) -> dict:
    """
    Reads the CSV and returns {identifier: new_description} for every row
    that has a non-empty Identifier and a non-empty Rewritten Metadata Content.
    Strips surrounding whitespace from both values.

    Emits a warning to stderr if the same identifier appears more than once
    (last value wins).
    """
    updates = {}
    duplicates = []
    with open(csv_path, newline='', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            identifier = row.get('Identifier', '').strip()
            rewritten  = row.get('Rewritten Metadata Content', '').strip()
            if identifier and rewritten:
                if identifier in updates:
                    duplicates.append(identifier)
                updates[identifier] = rewritten
    if duplicates:
        print(
            f"WARNING: duplicate identifier(s) in CSV (last value used): "
            f"{sorted(set(duplicates))}",
            file=sys.stderr,
        )
    return updates


def apply_updates(entries: list, updates: dict) -> tuple:
    """
    Iterates over JSON entries and replaces description where the identifier
    matches an entry in updates.

    Returns (new_entries, updated_ids, unchanged_ids) where:
      - new_entries   a new list of entry dicts (originals are not mutated)
      - updated_ids   identifiers that received a new description
      - unchanged_ids identifiers that had no matching CSV row
    """
    result    = []
    updated   = []
    unchanged = []
    for entry in entries:
        identifier = entry.get('identifier', '')
        if identifier in updates:
            result.append({**entry, 'description': updates[identifier]})
            updated.append(identifier)
        else:
            result.append(entry)
            unchanged.append(identifier)
    return result, updated, unchanged


def write_json(entries: list, json_path: str) -> None:
    """
    Writes entries back to the JSON file using 2-space indentation,
    ensure_ascii=False (preserves unicode), and a trailing newline —
    matching the format of the original file.

    Uses an atomic write (temp file + os.replace) so the target is never
    left in a partially-written state if an error occurs mid-write.
    """
    dir_name = os.path.dirname(os.path.abspath(json_path))
    fd, tmp_path = tempfile.mkstemp(dir=dir_name, suffix='.tmp')
    try:
        with os.fdopen(fd, 'w', encoding='utf-8') as f:
            json.dump(entries, f, indent=2, ensure_ascii=False)
            f.write('\n')
        os.replace(tmp_path, json_path)
    except Exception:
        try:
            os.unlink(tmp_path)
        except OSError:
            pass
        raise


def run(csv_path: str, json_path: str) -> int:
    """
    Full pipeline: load CSV updates, apply to JSON, write back.
    Returns the count of updated entries.
    Exits with a non-zero status if no entries were updated.
    """
    updates = load_updates_from_csv(csv_path)

    with open(json_path, 'r', encoding='utf-8') as f:
        entries = json.load(f)

    original_count = len(entries)
    entries, updated_ids, _ = apply_updates(entries, updates)

    write_json(entries, json_path)

    print(f"Total entries in JSON : {original_count}")
    print(f"Updated               : {len(updated_ids)}")
    for ident in sorted(updated_ids):
        print(f"  ✓ {ident}")

    # Warn about identifiers in CSV that were not found in the JSON
    json_ids  = {e['identifier'] for e in entries if 'identifier' in e}
    not_found = [i for i in updates if i not in json_ids]
    if not_found:
        print(f"\nWARNING: {len(not_found)} identifier(s) from CSV not found in JSON:")
        for ident in sorted(not_found):
            print(f"  ✗ {ident}")

    return len(updated_ids)


if __name__ == '__main__':
    if len(sys.argv) != 3:
        print(f"Usage: {Path(sys.argv[0]).name} <csv_path> <json_path>", file=sys.stderr)
        sys.exit(1)
    count = run(sys.argv[1], sys.argv[2])
    if count == 0:
        print("No entries were updated.", file=sys.stderr)
        sys.exit(1)
