#!/usr/bin/env python3
"""Generate deterministic synthetic conversation examples for JUSU.

This creates JSONL training candidates only. It does not train or modify a model.
The output must be reviewed, deduplicated and evaluated before any training use.
"""
from __future__ import annotations

import argparse
import json
import random
from pathlib import Path
from typing import Any

SYSTEM_ROLES = [
    "You are a concise, accurate software engineering assistant.",
    "You are a grounded research assistant that never guesses when facts are missing.",
    "You are a practical problem-solving assistant that asks one clarifying question when inputs are incomplete.",
    "You are a structured data assistant that outputs valid JSON and clean step-by-step reasoning.",
]

INTENT_FLOWS = [
    {
        "category": "clarification_then_execution",
        "turns": [
            {"role": "user", "content": "Help me build a {project_type}."},
            {"role": "assistant", "content": "What target platform and primary constraint (speed, offline support, or low memory) should we optimize for?"},
            {"role": "user", "content": "Target {platform} and prioritize {constraint}."},
            {"role": "assistant", "content": "For {platform} prioritizing {constraint}, use a modular pipeline: 1) lightweight input parser, 2) local inference or deterministic core engine, and 3) buffered output writer."},
        ],
    },
    {
        "category": "grounded_abstention",
        "turns": [
            {"role": "user", "content": "Context: {fact_context}\nQuestion: What is the {missing_attribute} of this system?"},
            {"role": "assistant", "content": "The provided context does not mention the {missing_attribute}. Share that specification if you want me to factor it in."},
        ],
    },
    {
        "category": "self_correction_reasoning",
        "turns": [
            {"role": "user", "content": "Calculate the total storage needed for {item_count} items at {item_size_mb} MB each, converted to GB (decimal)."},
            {"role": "assistant", "content": "Step 1: {item_count} * {item_size_mb} MB = {total_mb} MB.\nStep 2: Divide by 1,000 for decimal GB = {total_gb} GB.\nFinal Answer: {total_gb} GB."},
        ],
    },
    {
        "category": "strict_structured_output",
        "turns": [
            {"role": "user", "content": "Return a valid JSON object with keys 'task', 'platform', and 'status' for building a {project_type} on {platform}."},
            {"role": "assistant", "content": '{{"task": "{project_type}", "platform": "{platform}", "status": "ready"}}'},
        ],
    },
    {
        "category": "multi_turn_constraint_update",
        "turns": [
            {"role": "user", "content": "Give me 3 steps to optimize a {project_type}."},
            {"role": "assistant", "content": "1. Profile memory bottlenecks.\n2. Quantize or compress heavy assets.\n3. Cache repeated computations."},
            {"role": "user", "content": "Now rewrite step 2 specifically for {platform} in under 15 words."},
            {"role": "assistant", "content": "Compress and quantize {platform} model weights to reduce memory footprint and load times."},
        ],
    },
]

VARIABLES = {
    "project_type": ["offline video generator", "cross-platform app", "vision classifier", "local search engine", "audio synthesizer"],
    "platform": ["Linux desktop", "Android mobile", "embedded ARM", "Windows GPU", "macOS Apple Silicon"],
    "constraint": ["offline execution", "low RAM usage", "fast startup latency", "zero external dependencies"],
    "fact_context": [
        "Module A processes 30 frames per second using 2 GB of VRAM.",
        "The local cache stores up to 500 embeddings in SQLite.",
    ],
    "missing_attribute": ["battery drain rate", "maximum concurrent user limit", "network timeout threshold"],
    "item_count": [250, 1200, 4500],
    "item_size_mb": [4, 12, 35],
}


def render_flow(seed_idx: int, base_seed: int = 0) -> dict[str, Any]:
    """Render one reproducible example. Seed controls both template and values."""
    rng = random.Random(base_seed + seed_idx)
    template = rng.choice(INTENT_FLOWS)
    count = rng.choice(VARIABLES["item_count"])
    size_mb = rng.choice(VARIABLES["item_size_mb"])
    total_mb = count * size_mb
    values = {
        "project_type": rng.choice(VARIABLES["project_type"]),
        "platform": rng.choice(VARIABLES["platform"]),
        "constraint": rng.choice(VARIABLES["constraint"]),
        "fact_context": rng.choice(VARIABLES["fact_context"]),
        "missing_attribute": rng.choice(VARIABLES["missing_attribute"]),
        "item_count": count,
        "item_size_mb": size_mb,
        "total_mb": total_mb,
        "total_gb": f"{total_mb / 1000:.3f}",
    }
    messages = [{"role": "system", "content": rng.choice(SYSTEM_ROLES)}]
    for turn in template["turns"]:
        messages.append({"role": turn["role"], "content": turn["content"].format(**values)})
    record = {"id": f"conv_{base_seed}_{seed_idx}", "category": template["category"], "messages": messages}
    validate_record(record)
    return record


def validate_record(record: dict[str, Any]) -> None:
    """Reject malformed examples before writing them."""
    if not record.get("id") or record.get("category") not in {flow["category"] for flow in INTENT_FLOWS}:
        raise ValueError("Record is missing a valid id or category")
    messages = record.get("messages")
    if not isinstance(messages, list) or len(messages) < 2:
        raise ValueError("Record must contain a system message and at least one conversation turn")
    if messages[0].get("role") != "system":
        raise ValueError("First message must be the system role")
    if any(m.get("role") not in {"system", "user", "assistant"} or not isinstance(m.get("content"), str) for m in messages):
        raise ValueError("Every message must have a supported role and string content")
    if record["category"] == "strict_structured_output":
        payload = json.loads(messages[-1]["content"])
        if set(payload) != {"task", "platform", "status"}:
            raise ValueError("Structured-output example does not contain the required JSON keys")
    if record["category"] == "self_correction_reasoning":
        user_text, answer = messages[1]["content"], messages[2]["content"]
        del user_text  # The answer is independently checked against its generated arithmetic.
        import re
        match = re.search(r"Step 1: (\d+) \* (\d+) MB = (\d+) MB", answer)
        if not match:
            raise ValueError("Arithmetic example is missing its multiplication")
        count, size, total = map(int, match.groups())
        if count * size != total or f"{total / 1000:.3f}" not in answer:
            raise ValueError("Arithmetic example contains an incorrect result")


def generate_dataset(output_path: str | Path, num_examples: int = 100_000, seed: int = 0) -> int:
    if num_examples < 1:
        raise ValueError("num_examples must be at least 1")
    path = Path(output_path)
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        for index in range(num_examples):
            record = render_flow(index, seed)
            handle.write(json.dumps(record, ensure_ascii=False, separators=(",", ":")) + "\n")
    return num_examples


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", default="english_conversation_flows.jsonl")
    parser.add_argument("--count", type=int, default=100_000)
    parser.add_argument("--seed", type=int, default=0)
    args = parser.parse_args()
    count = generate_dataset(args.output, args.count, args.seed)
    print(f"Generated {count} validated conversation examples -> {args.output}")


if __name__ == "__main__":
    main()
