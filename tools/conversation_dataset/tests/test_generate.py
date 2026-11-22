import json
import tempfile
import unittest
from pathlib import Path

from generate import generate_dataset, render_flow, validate_record


class ConversationDatasetTests(unittest.TestCase):
    def test_seed_is_reproducible(self):
        self.assertEqual(render_flow(17, 9), render_flow(17, 9))

    def test_different_indices_have_unique_ids(self):
        self.assertNotEqual(render_flow(1)["id"], render_flow(2)["id"])

    def test_all_templates_can_be_rendered_and_validated(self):
        categories = set()
        for index in range(500):
            record = render_flow(index)
            validate_record(record)
            categories.add(record["category"])
        self.assertEqual(
            categories,
            {
                "clarification_then_execution",
                "grounded_abstention",
                "self_correction_reasoning",
                "strict_structured_output",
                "multi_turn_constraint_update",
            },
        )

    def test_jsonl_output_is_valid_and_has_requested_count(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            output = Path(temp_dir) / "nested" / "flows.jsonl"
            self.assertEqual(generate_dataset(output, 25, seed=4), 25)
            rows = [json.loads(line) for line in output.read_text(encoding="utf-8").splitlines()]
            self.assertEqual(len(rows), 25)
            self.assertEqual(len({row["id"] for row in rows}), 25)

    def test_zero_count_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            with self.assertRaises(ValueError):
                generate_dataset(Path(temp_dir) / "empty.jsonl", 0)


if __name__ == "__main__":
    unittest.main()
