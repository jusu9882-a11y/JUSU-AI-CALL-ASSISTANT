# Synthetic conversation dataset generator

This tool creates reproducible JSON Lines (JSONL) examples for future JUSU model
experiments. It includes five flow categories:

- clarification followed by execution
- grounded abstention when context lacks a fact
- arithmetic with a verifiable result
- strict JSON output
- multi-turn constraint updates

## Generate examples

Requires Python 3.10+ and uses only the standard library.

```sh
python3 tools/conversation_dataset/generate.py \
  --output english_conversation_flows.jsonl \
  --count 100000 \
  --seed 0
```

The generator validates each record before writing it, including JSON output
and arithmetic examples. Generation is deterministic for a given seed and
record index.

## Important limitations

These are synthetic templates, not human-reviewed conversations or evidence
that a model has been trained. They are suitable as an initial data-generation
utility only. Before training, JUSU must review quality, remove duplicates and
leakage, balance categories, add broader realistic language and multilingual
examples, and run held-out evaluations. Do not use generated examples as
factual sources or as a replacement for the app's independent safety policy.

The generated JSONL file is not bundled into the Android APK and is not read by
the current rule-based chat engine. No model is trained or downloaded by this tool.
