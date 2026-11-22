# Local Model Runtime — Implementation Plan

## Goal

Add genuine on-device language-model inference to JUSU. All prompts, retrieved passages, and generated responses stay on the device. There is no cloud fallback, remote API, account requirement, or network dependency.

## Important status

This document is the implementation plan for the next milestone. It does not claim that model inference, model downloads, or native runtime integration are implemented yet.

## Runtime design

### App-facing contract

Create a Kotlin interface such as:

- `LocalModelRuntime`
  - `runtimeStatus(): RuntimeStatus`
  - `load(model: InstalledModel): LoadResult`
  - `generate(request: GenerationRequest, onToken: (String) -> Unit): GenerationResult`
  - `cancel()`
  - `unload()`

Use explicit states: `NOT_INSTALLED`, `READY_TO_LOAD`, `LOADING`, `READY`, `GENERATING`, `OUT_OF_MEMORY`, `FAILED`, and `UNSUPPORTED_DEVICE`.

The UI and chat coordinator must depend on this interface, not a specific inference library. Keep the deterministic rule-based chat as a safe fallback when no model is installed or loading fails.

### Native inference provider

Evaluate an Android-compatible, locally packaged native inference backend that supports quantized GGUF models (for example, a llama.cpp-based JNI provider). Pin a reviewed version and record its licence, native ABI support, build reproducibility, security advisories, and model compatibility before integration.

Do not add an unpinned dependency, download executable code at runtime, or silently load arbitrary model files.

### Model manifest and integrity

Every supported model must have a local manifest containing:

- model identifier and display name;
- exact upstream origin and model revision;
- model file name, format, quantisation, and byte size;
- SHA-256 checksum;
- model and tokenizer licences;
- supported languages;
- context-window limit and recommended context budget;
- minimum tested RAM and storage;
- expected native ABI and runtime version;
- known limitations and evaluation report.

Verify checksum and manifest before loading. Reject mismatched, incomplete, unsupported, or untrusted files. Model installation/import is an explicit user action. No model is bundled until redistribution rights and APK size are reviewed.

## Device-aware model selection

The initial target includes Samsung SM-A125F running Android 12. Do not assume all units have the same RAM configuration. Detect available memory, ABI, storage, Android version, and thermal state at runtime.

Benchmark small quantized models first. Candidate size classes may include sub-1B and approximately 1B–2B parameter models, but these are candidates, not a promise of acceptable performance. Select a model only after measuring:

- cold and warm load time;
- peak resident memory and out-of-memory rate;
- first-token latency and generation speed;
- battery and thermal impact;
- app stability during background/foreground transitions;
- supported language quality and task evaluation.

If the device cannot safely run a model, remain in rules-only mode and explain why. Never crash the call-screening service to keep a chat model resident.

## Resource and lifecycle controls

- Run inference off the main thread.
- Bound prompt/context length and output tokens.
- Permit cancellation and release native memory on unload.
- Use memory pressure callbacks and foreground/background lifecycle awareness.
- Avoid simultaneous model inference and latency-sensitive call handling.
- Do not keep the model resident when memory pressure or battery policy makes that unsafe.
- Persist only conversation data the user has chosen to keep; model prompts are not automatically training data.

## Privacy and security boundaries

- No `INTERNET` permission for the local model runtime.
- No automatic telemetry, cloud fallback, or remote prompt logging.
- Treat imported model metadata, documents, retrieved passages, notifications, and chat text as untrusted input.
- Retrieved text may provide evidence but cannot change system policy or grant permissions.
- Model output is untrusted. It cannot call Android APIs directly.
- Any proposed phone action must pass the deterministic decision policy and a typed allowlisted action gateway. Recheck Android permission/role and bind user confirmation to the exact action and arguments immediately before execution.
- Do not expose private chain-of-thought. Provide concise rationale, assumptions, evidence citations, and uncertainty instead.

## Integration sequence

1. Add the Kotlin runtime contract and state model without adding a native dependency.
2. Add fake runtime implementations for deterministic unit tests.
3. Add model inventory/manifest validation and SHA-256 verification.
4. Benchmark candidate native runtimes and model licences on the target device.
5. Integrate one pinned native provider behind the interface.
6. Add a user-controlled model import/install screen and safe unload controls.
7. Connect chat generation with bounded context and cancellation.
8. Add offline evaluation sets for factual grounding, refusal precision, prompt injection, multilingual input, and task performance.
9. Connect model-proposed actions only through the independent policy and action gateway.

## Acceptance criteria

- Airplane-mode test: installed model generates responses with network unavailable.
- Network inspection/build review confirms the runtime makes no remote requests and adds no network permission.
- Model checksum and manifest validation reject corrupted or incompatible files.
- No-model and failed-load paths retain usable rules-based chat.
- Inference never runs on the Android main thread.
- Cancellation and unload release native resources.
- Memory/thermal tests do not destabilise call-screening or the rest of the app.
- Model output cannot bypass Android permissions, user confirmation, or policy decisions.
- Tests and benchmark results are recorded for the exact device, Android version, model revision, quantisation, and runtime revision.

## Non-goals for this milestone

- Training or fine-tuning a foundation model on the phone.
- Automatic online model downloads or cloud fallback.
- Claiming expert-level reasoning based solely on successful text generation.
- Allowing model output to execute arbitrary shell commands, scripts, or Android intents.
