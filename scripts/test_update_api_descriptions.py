#!/usr/bin/env python3
"""
test_update_api_descriptions.py

Tests for update_api_descriptions.py.

Run with:
    python3 -m unittest scripts/test_update_api_descriptions.py -v
  or from the project root:
    python3 -m unittest discover -s scripts -p 'test_*.py' -v
"""

import csv
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from update_api_descriptions import apply_updates, load_updates_from_csv, run, write_json

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

SAMPLE_FIELDNAMES = [
    'API Name', 'Identifier', 'Notes',
    'Original Metadata Content', 'Rewritten Metadata Content',
    "What's Changed?",
]


def make_csv(rows: list, path: str) -> None:
    with open(path, 'w', newline='', encoding='utf-8') as f:
        writer = csv.DictWriter(f, fieldnames=SAMPLE_FIELDNAMES, extrasaction='ignore')
        writer.writeheader()
        writer.writerows(rows)


def make_json(entries: list, path: str) -> None:
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(entries, f, indent=2, ensure_ascii=False)
        f.write('\n')


# ---------------------------------------------------------------------------
# Tests: load_updates_from_csv
# ---------------------------------------------------------------------------

class TestLoadUpdatesFromCsv(unittest.TestCase):

    def setUp(self):
        self._tmp = tempfile.NamedTemporaryFile(suffix='.csv', delete=False)
        self._tmp.close()
        self.csv_path = self._tmp.name

    def tearDown(self):
        Path(self.csv_path).unlink(missing_ok=True)

    def test_loads_identifier_to_description_mapping(self):
        make_csv([
            {'Identifier': 'foo-api', 'Rewritten Metadata Content': 'Desc Foo.'},
            {'Identifier': 'bar-api', 'Rewritten Metadata Content': 'Desc Bar.'},
        ], self.csv_path)
        result = load_updates_from_csv(self.csv_path)
        self.assertEqual(result, {'foo-api': 'Desc Foo.', 'bar-api': 'Desc Bar.'})

    def test_skips_rows_with_empty_identifier_or_content(self):
        make_csv([
            {'Identifier': '',        'Rewritten Metadata Content': 'No slug.'},
            {'Identifier': 'bar-api', 'Rewritten Metadata Content': ''},
            {'Identifier': 'ok-api',  'Rewritten Metadata Content': 'Valid.'},
        ], self.csv_path)
        result = load_updates_from_csv(self.csv_path)
        self.assertEqual(result, {'ok-api': 'Valid.'})

    def test_strips_whitespace_from_identifier_and_description(self):
        make_csv([
            {'Identifier': '  baz-api  ', 'Rewritten Metadata Content': '  Trimmed.  '},
        ], self.csv_path)
        result = load_updates_from_csv(self.csv_path)
        self.assertEqual(result, {'baz-api': 'Trimmed.'})


# ---------------------------------------------------------------------------
# Tests: apply_updates
# ---------------------------------------------------------------------------

class TestApplyUpdates(unittest.TestCase):

    def _entries(self):
        return [
            {'description': 'Old A.', 'identifier': 'a-api'},
            {'description': 'Old B.', 'identifier': 'b-api'},
        ]

    def test_updates_matched_entries_and_leaves_others_unchanged(self):
        result, updated, unchanged = apply_updates(self._entries(), {'a-api': 'New A.'})
        self.assertEqual(result[0]['description'], 'New A.')
        self.assertEqual(result[1]['description'], 'Old B.')
        self.assertEqual(updated, ['a-api'])
        self.assertEqual(unchanged, ['b-api'])

    def test_does_not_mutate_original_entries(self):
        entries = self._entries()
        apply_updates(entries, {'a-api': 'New A.'})
        self.assertEqual(entries[0]['description'], 'Old A.')

    def test_returns_categorised_id_lists(self):
        entries = self._entries() + [{'description': 'Old C.', 'identifier': 'c-api'}]
        _, updated, unchanged = apply_updates(entries, {'a-api': 'X', 'c-api': 'Y'})
        self.assertCountEqual(updated, ['a-api', 'c-api'])
        self.assertCountEqual(unchanged, ['b-api'])


# ---------------------------------------------------------------------------
# Tests: write_json
# ---------------------------------------------------------------------------

class TestWriteJson(unittest.TestCase):

    def setUp(self):
        self._tmp = tempfile.NamedTemporaryFile(suffix='.json', delete=False, mode='w')
        self._tmp.close()
        self.json_path = self._tmp.name

    def tearDown(self):
        Path(self.json_path).unlink(missing_ok=True)

    def test_writes_json_with_correct_format(self):
        entries = [{'description': 'Desc.', 'identifier': 'my-api'}]
        write_json(entries, self.json_path)
        content = Path(self.json_path).read_text(encoding='utf-8')
        loaded  = json.loads(content)
        self.assertEqual(loaded[0], entries[0])
        self.assertTrue(content.endswith('\n'), "file must end with a newline")
        self.assertIn('  "description"', content, "must use 2-space indentation")
        self.assertLess(content.index('"description"'), content.index('"identifier"'),
                        "description key must precede identifier key")


# ---------------------------------------------------------------------------
# Tests: run (integration)
# ---------------------------------------------------------------------------

class TestRun(unittest.TestCase):

    def setUp(self):
        for attr, suffix in (('csv_path', '.csv'), ('json_path', '.json')):
            tf = tempfile.NamedTemporaryFile(suffix=suffix, delete=False)
            tf.close()
            setattr(self, attr, tf.name)

    def tearDown(self):
        for attr in ('csv_path', 'json_path'):
            Path(getattr(self, attr)).unlink(missing_ok=True)

    def test_full_pipeline(self):
        make_csv([
            {'Identifier': '',        'Rewritten Metadata Content': 'Ignored.'},
            {'Identifier': 'foo-api', 'Rewritten Metadata Content': 'New Foo.'},
            {'Identifier': 'bar-api', 'Rewritten Metadata Content': 'New Bar.'},
        ], self.csv_path)
        original = [
            {'description': 'Old Foo.', 'identifier': 'foo-api'},
            {'description': 'Old Bar.', 'identifier': 'bar-api'},
            {'description': 'Old Baz.', 'identifier': 'baz-api'},
        ]
        make_json(original, self.json_path)

        count = run(self.csv_path, self.json_path)

        result = json.loads(Path(self.json_path).read_text(encoding='utf-8'))
        self.assertEqual(count, 2)
        self.assertEqual(len(result), len(original))           # entry count preserved
        self.assertEqual(result[0]['description'], 'New Foo.') # csv row applied
        self.assertEqual(result[1]['description'], 'New Bar.') # csv row applied
        self.assertEqual(result[2]['description'], 'Old Baz.') # not in csv — untouched

if __name__ == '__main__':
    unittest.main(verbosity=2)
