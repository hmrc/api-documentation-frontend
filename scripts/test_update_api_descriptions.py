#!/usr/bin/env python3
"""
test_update_api_descriptions.py

Unit and integration tests for update_api_descriptions.py.

Run with:
    python3 -m unittest scripts/test_update_api_descriptions.py -v
  or from the project root:
    python3 -m unittest discover -s scripts -p 'test_*.py' -v
"""

import csv
import io
import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

# Make the scripts directory importable regardless of cwd
sys.path.insert(0, str(Path(__file__).parent))
from update_api_descriptions import (
    apply_updates,
    load_updates_from_csv,
    run,
    write_json,
)

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

REAL_JSON_PATH = Path(__file__).parent.parent / 'resources' / 'api-description-overrides.json'
REAL_CSV_PATH  = Path(__file__).parent.parent / 'API_home_page_metadata_rewrite_comparison.csv'

SAMPLE_FIELDNAMES = [
    'API Name', 'Identifier', 'Notes',
    'Original Metadata Content', 'Rewritten Metadata Content',
    "What's Changed?",
]

def make_csv(rows: list, path: str) -> None:
    """Write a minimal fixture CSV with the expected column layout."""
    with open(path, 'w', newline='', encoding='utf-8') as f:
        writer = csv.DictWriter(f, fieldnames=SAMPLE_FIELDNAMES, extrasaction='ignore')
        writer.writeheader()
        writer.writerows(rows)


def make_json(entries: list, path: str) -> None:
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(entries, f, indent=2, ensure_ascii=False)
        f.write('\n')


def load_json(path: str) -> list:
    with open(path, encoding='utf-8') as f:
        return json.load(f)


# ---------------------------------------------------------------------------
# Tests: load_updates_from_csv
# ---------------------------------------------------------------------------

class TestLoadUpdatesFromCsv(unittest.TestCase):

    def _write_and_load(self, rows):
        with tempfile.NamedTemporaryFile(mode='w', suffix='.csv', delete=False, encoding='utf-8') as f:
            tmp = f.name
        make_csv(rows, tmp)
        result = load_updates_from_csv(tmp)
        Path(tmp).unlink()
        return result

    def test_returns_identifier_to_rewritten_mapping(self):
        rows = [{'API Name': 'Foo', 'Identifier': 'foo-api',
                 'Rewritten Metadata Content': 'New description.'}]
        result = self._write_and_load(rows)
        self.assertEqual(result, {'foo-api': 'New description.'})

    def test_skips_rows_with_empty_identifier(self):
        rows = [
            {'API Name': 'No Slug', 'Identifier': '',
             'Rewritten Metadata Content': 'Some text.'},
            {'API Name': 'Has Slug', 'Identifier': 'has-slug',
             'Rewritten Metadata Content': 'Real text.'},
        ]
        result = self._write_and_load(rows)
        self.assertNotIn('', result)
        self.assertIn('has-slug', result)

    def test_skips_rows_with_empty_rewritten_content(self):
        rows = [{'API Name': 'Bar', 'Identifier': 'bar-api',
                 'Rewritten Metadata Content': ''}]
        result = self._write_and_load(rows)
        self.assertEqual(result, {})

    def test_strips_whitespace_from_identifier_and_description(self):
        rows = [{'API Name': 'Baz', 'Identifier': '  baz-api  ',
                 'Rewritten Metadata Content': '  Trimmed text.  '}]
        result = self._write_and_load(rows)
        self.assertEqual(result, {'baz-api': 'Trimmed text.'})

    def test_multiple_rows_all_loaded(self):
        rows = [
            {'API Name': 'A', 'Identifier': 'a-api', 'Rewritten Metadata Content': 'Desc A.'},
            {'API Name': 'B', 'Identifier': 'b-api', 'Rewritten Metadata Content': 'Desc B.'},
        ]
        result = self._write_and_load(rows)
        self.assertEqual(len(result), 2)
        self.assertEqual(result['a-api'], 'Desc A.')
        self.assertEqual(result['b-api'], 'Desc B.')

    def test_warns_on_duplicate_identifier_last_value_wins(self):
        """Duplicate identifiers emit a stderr warning; the last value is kept."""
        rows = [
            {'API Name': 'Foo',     'Identifier': 'foo-api', 'Rewritten Metadata Content': 'First.'},
            {'API Name': 'Foo Dup', 'Identifier': 'foo-api', 'Rewritten Metadata Content': 'Second.'},
        ]
        with tempfile.NamedTemporaryFile(mode='w', suffix='.csv', delete=False, encoding='utf-8') as f:
            tmp = f.name
        make_csv(rows, tmp)
        try:
            with patch('sys.stderr', new_callable=io.StringIO) as mock_err:
                result = load_updates_from_csv(tmp)
            self.assertIn('foo-api', mock_err.getvalue())
            self.assertEqual(result['foo-api'], 'Second.')
        finally:
            Path(tmp).unlink()


# ---------------------------------------------------------------------------
# Tests: apply_updates
# ---------------------------------------------------------------------------

class TestApplyUpdates(unittest.TestCase):

    def _entries(self):
        return [
            {'description': 'Old A.', 'identifier': 'a-api'},
            {'description': 'Old B.', 'identifier': 'b-api'},
            {'description': 'Old C.', 'identifier': 'c-api'},
        ]

    def test_updates_matching_entries(self):
        entries = self._entries()
        result_entries, _, _ = apply_updates(entries, {'a-api': 'New A.'})
        self.assertEqual(result_entries[0]['description'], 'New A.')

    def test_untouched_entries_are_unchanged(self):
        entries = self._entries()
        result_entries, _, _ = apply_updates(entries, {'a-api': 'New A.'})
        self.assertEqual(result_entries[1]['description'], 'Old B.')
        self.assertEqual(result_entries[2]['description'], 'Old C.')

    def test_does_not_mutate_original_entries(self):
        """apply_updates must return new dicts; originals must be unchanged."""
        entries = self._entries()
        original_desc = entries[0]['description']
        apply_updates(entries, {'a-api': 'New A.'})
        self.assertEqual(entries[0]['description'], original_desc)

    def test_returns_updated_identifier_list(self):
        entries = self._entries()
        _, updated_ids, _ = apply_updates(entries, {'a-api': 'New A.', 'c-api': 'New C.'})
        self.assertIn('a-api', updated_ids)
        self.assertIn('c-api', updated_ids)
        self.assertNotIn('b-api', updated_ids)

    def test_returns_unchanged_identifier_list(self):
        entries = self._entries()
        _, _, unchanged_ids = apply_updates(entries, {'a-api': 'New A.'})
        self.assertIn('b-api', unchanged_ids)
        self.assertIn('c-api', unchanged_ids)
        self.assertNotIn('a-api', unchanged_ids)

    def test_entry_count_is_preserved(self):
        entries = self._entries()
        original_count = len(entries)
        result_entries, _, _ = apply_updates(entries, {'a-api': 'New A.'})
        self.assertEqual(len(result_entries), original_count)

    def test_no_updates_when_no_matches(self):
        entries = self._entries()
        _, updated_ids, unchanged_ids = apply_updates(entries, {'z-api': 'New Z.'})
        self.assertEqual(updated_ids, [])
        self.assertEqual(len(unchanged_ids), 3)

    def test_entries_missing_identifier_key_treated_as_unchanged(self):
        """Malformed JSON entries without an 'identifier' key are left as-is."""
        entries = [
            {'description': 'No id field.'},          # malformed — no 'identifier'
            {'description': 'Normal.', 'identifier': 'a-api'},
        ]
        result_entries, updated_ids, unchanged_ids = apply_updates(
            entries, {'a-api': 'New A.'}
        )
        self.assertEqual(result_entries[0]['description'], 'No id field.')
        self.assertEqual(result_entries[1]['description'], 'New A.')
        self.assertIn('a-api', updated_ids)
        self.assertNotIn('', updated_ids)


# ---------------------------------------------------------------------------
# Tests: write_json
# ---------------------------------------------------------------------------

class TestWriteJson(unittest.TestCase):

    def _roundtrip(self, entries):
        with tempfile.NamedTemporaryFile(suffix='.json', delete=False, encoding='utf-8', mode='w') as f:
            tmp = f.name
        write_json(entries, tmp)
        content = Path(tmp).read_text(encoding='utf-8')
        loaded  = json.loads(content)
        Path(tmp).unlink()
        return content, loaded

    def test_output_is_valid_json_with_correct_content(self):
        entries = [{'description': 'Desc.', 'identifier': 'my-api'}]
        _, loaded = self._roundtrip(entries)
        self.assertEqual(loaded[0]['description'], 'Desc.')
        self.assertEqual(loaded[0]['identifier'], 'my-api')

    def test_output_ends_with_trailing_newline(self):
        entries = [{'description': 'D.', 'identifier': 'x'}]
        content, _ = self._roundtrip(entries)
        self.assertTrue(content.endswith('\n'), "File must end with a newline")

    def test_output_uses_two_space_indentation(self):
        entries = [{'description': 'D.', 'identifier': 'x'}]
        content, _ = self._roundtrip(entries)
        self.assertIn('  "description"', content)

    def test_key_order_description_before_identifier(self):
        entries = [{'description': 'D.', 'identifier': 'x'}]
        content, _ = self._roundtrip(entries)
        desc_pos  = content.index('"description"')
        ident_pos = content.index('"identifier"')
        self.assertLess(desc_pos, ident_pos,
                        "description key must appear before identifier key")

    def test_unicode_characters_preserved(self):
        entries = [{'description': 'Use self\u2011employment data.', 'identifier': 'x'}]
        content, loaded = self._roundtrip(entries)
        self.assertIn('\u2011', content)  # non-breaking hyphen not escaped
        self.assertEqual(loaded[0]['description'], 'Use self\u2011employment data.')


# ---------------------------------------------------------------------------
# Tests: run (end-to-end)
# ---------------------------------------------------------------------------

class TestRun(unittest.TestCase):

    def setUp(self):
        for attr, suffix in (('tmp_csv', '.csv'), ('tmp_json', '.json')):
            tf = tempfile.NamedTemporaryFile(suffix=suffix, delete=False)
            tf.close()
            setattr(self, attr, tf.name)

    def tearDown(self):
        Path(self.tmp_csv).unlink(missing_ok=True)
        Path(self.tmp_json).unlink(missing_ok=True)

    def test_full_pipeline_updates_correct_entries(self):
        make_csv([
            {'API Name': 'Foo', 'Identifier': 'foo-api',
             'Rewritten Metadata Content': 'New Foo.'},
            {'API Name': 'Bar', 'Identifier': 'bar-api',
             'Rewritten Metadata Content': 'New Bar.'},
        ], self.tmp_csv)
        make_json([
            {'description': 'Old Foo.', 'identifier': 'foo-api'},
            {'description': 'Old Bar.', 'identifier': 'bar-api'},
            {'description': 'Old Baz.', 'identifier': 'baz-api'},
        ], self.tmp_json)

        count = run(self.tmp_csv, self.tmp_json)

        result = load_json(self.tmp_json)
        self.assertEqual(count, 2)
        self.assertEqual(result[0]['description'], 'New Foo.')
        self.assertEqual(result[1]['description'], 'New Bar.')
        self.assertEqual(result[2]['description'], 'Old Baz.')  # untouched

    def test_total_entry_count_unchanged_after_run(self):
        make_csv([
            {'API Name': 'Foo', 'Identifier': 'foo-api',
             'Rewritten Metadata Content': 'New Foo.'},
        ], self.tmp_csv)
        original_entries = [
            {'description': 'Old Foo.', 'identifier': 'foo-api'},
            {'description': 'Old Bar.', 'identifier': 'bar-api'},
        ]
        make_json(original_entries, self.tmp_json)

        run(self.tmp_csv, self.tmp_json)

        result = load_json(self.tmp_json)
        self.assertEqual(len(result), len(original_entries))

    def test_out_of_scope_rows_without_identifier_do_not_affect_json(self):
        make_csv([
            {'API Name': 'No Slug', 'Identifier': '',
             'Rewritten Metadata Content': 'Should be ignored.'},
            {'API Name': 'Foo', 'Identifier': 'foo-api',
             'Rewritten Metadata Content': 'New Foo.'},
        ], self.tmp_csv)
        make_json([
            {'description': 'Old Foo.', 'identifier': 'foo-api'},
        ], self.tmp_json)

        count = run(self.tmp_csv, self.tmp_json)
        self.assertEqual(count, 1)

if __name__ == '__main__':
    unittest.main(verbosity=2)
