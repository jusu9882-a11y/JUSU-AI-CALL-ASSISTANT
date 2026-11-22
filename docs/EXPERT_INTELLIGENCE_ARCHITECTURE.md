# JUSU Expert Intelligence Architecture

## Product decision

JUSU is an Android-first call assistant with expert-level ambition. Its scope is both conversational chat and authorised phone-assistant operations.

**Hard product constraint: no online service is required.** Chat, knowledge retrieval, reasoning, memory, safety decisions, and supported phone operations must work locally. No mandatory account, cloud model, remote server, telemetry, or network permission. Any optional network feature would require a separate future product decision and explicit opt-in; it is not part of this architecture.

"Expert" is a target capability and an engineering/evaluation goal, not a claim that the current starter engine already has expert-level intelligence.

## The 30 intelligence requirements

### Core reasoning and intelligence
1. **Contextual awareness** — use the current conversation, relevant local memory, user-approved preferences, and current app/device state.
2. **Adaptive reasoning** — select an appropriate solving strategy and adjust when new evidence appears.
3. **Multi-step planning** — decompose goals into steps, track prerequisites, and verify each step. Provide concise decision summaries and plans rather than exposing private internal chain-of-thought.
4. **Abstract and analogical thinking** — identify structures and relationships that transfer between problems; state where an analogy stops applying.
5. **Causal and counterfactual reasoning** — distinguish correlation from causation and compare plausible outcomes without presenting speculation as fact.
6. **Mathematical and symbolic logic** — use deterministic calculators, parsers, and tested algorithms for exact operations.
7. **Practical creativity and synthesis** — generate alternatives and combine evidence while respecting constraints.
8. **Cross-domain and multimodal generalisation** — integrate text and supported on-device inputs (for example, user-selected images or app-visible notification metadata) only when permission and product scope allow.

### Reliability, truth, and calibration
9. **Epistemic honesty** — clearly distinguish known facts, inference, estimates, and unknowns.
10. **Hallucination resistance and source grounding** — ground factual knowledge answers in the local curated library where possible and show title/author/edition/page or section provenance.
11. **Uncertainty calibration** — estimate confidence using evaluated signals; never invent numerical confidence scores without calibration evidence.
12. **Self-correction and verification** — check calculations, tool results, constraints, and claims; revise when a check fails.
13. **Robustness to ambiguity and noisy input** — ask a focused clarification when uncertainty changes the answer or action.
14. **Temporal and state awareness** — distinguish timestamps, stale facts, current app state, and unavailable state; use Android system state rather than guessing.
15. **Error recovery and graceful degradation** — fall back to simpler local rules when a model, index, or device capability is unavailable; explain limitations.

### Execution, tools, and output quality
16. **Instruction and constraint adherence** — follow user instructions within safety, privacy, platform, and higher-priority product rules.
17. **Structured output precision** — validate JSON, XML, code, and schemas with deterministic parsers/validators before use.
18. **Tool and environment mastery** — expose only typed, allowlisted local actions with explicit preconditions and result handling.
19. **Long-context retrieval and memory continuity** — retrieve relevant local conversation and user-approved memories; provide inspect, export, and delete controls.
20. **Code generation and debugging** — use syntax-aware generation and local tests where supported; never claim code was run unless it was.
21. **Latency and compute economy** — select model size, context length, and retrieval depth to fit device memory, thermal, battery, and latency budgets.
22. **Clear communication** — answer directly, concisely, and in the user's language where supported.

### Alignment, interaction, and safety
23. **Intent inference and clarification discipline** — infer likely intent but ask before consequential ambiguous actions.
24. **Proactive problem-solving** — identify important prerequisites and edge cases without taking unauthorised actions.
25. **Emotional and social intelligence** — respond respectfully without manipulation, false intimacy, or pretending to have feelings.
26. **Cultural and linguistic adaptability** — handle supported languages and local phrasing; indicate when language coverage is limited.
27. **Nuanced safety** — refuse clearly harmful or unauthorised requests while allowing benign, legitimate assistance.
28. **Prompt-injection resilience** — treat retrieved documents, notifications, messages, and user content as untrusted data, never as policy or executable instructions.
29. **Privacy protection** — minimise collection, keep data local, encrypt sensitive data at rest where appropriate, and provide clear controls.
30. **Preference and feedback adaptation** — learn only from explicit user feedback and approved local preferences. RLHF/DPO/GRPO are model-training methods, not magic runtime features; any training pipeline must be separate, consented, privacy-reviewed, and evaluated before a model update is shipped.

## Offline-first runtime architecture

1. **Chat and interaction UI** — local conversation interface, accessible controls, language selection, and clear status for offline/model availability.
2. **Context assembler** — selects only relevant conversation turns, approved memory, and permission-checked app state.
3. **Local model runtime** — replaceable inference provider for a quantized on-device language model. Model files are installed or imported locally; inference does not call a cloud API.
4. **Knowledge library and retrieval** — local document store, text extraction, chunking, lexical search and (where device resources permit) local embeddings/vector retrieval. Every passage retains source and page/section provenance.
5. **Reasoning orchestrator** — task decomposition, planning, evidence comparison, uncertainty tracking, and verification.
6. **Deterministic specialist tools** — calculator, date/time logic, schema validators, local search, and tested app-specific functions. Exact tasks should not rely on language-model arithmetic.
7. **Decision policy** — deterministic checks for risk, consent, privacy, and Android authorisation. Model output cannot override this layer.
8. **Action gateway** — typed allowlisted operations only. Every state-changing or sensitive action requires explicit user confirmation and the relevant Android permission/role. The gateway rechecks permissions immediately before execution.
9. **Local memory and audit controls** — bounded conversation storage, user-approved preferences, minimal action audit records, and inspect/export/delete controls.
10. **Evaluation harness** — offline test sets, regression tests, safety scenarios, grounded-answer checks, latency and memory benchmarks.

## Action decision contract

Every proposed phone operation is represented as a typed request with an action identifier, arguments, risk classification, required Android permissions/roles, and whether explicit user confirmation is required. The policy returns exactly one of:
- `ALLOW` — low-risk, read-only, authorised action within the allowlist.
- `ASK_USER` — action may proceed only after clear confirmation; confirmation is bound to the exact action and arguments.
- `BLOCK` — unauthorised, unsupported, or disallowed action.

The language model may propose an action but cannot grant itself permission, fabricate user consent, change policy, or invoke Android APIs directly. Android remains the final authority for platform permissions and roles. A failed or revoked permission blocks execution.

## Knowledge library and books

Use public-domain, openly licensed, or properly licensed sources. Do not ingest copyrighted books without permission. Store title, author, publisher, edition, publication date, licence, subject tags, language, checksum, extracted text, and page/section provenance. Retrieval must show which sources support an answer, distinguish conflicting sources, and avoid treating a book as automatically authoritative for every claim. Books are knowledge sources, not behavioural policy; safety rules are maintained separately and take precedence.

## Model and device strategy

- The app must remain useful without a model installed: deterministic call rules, basic chat/help, policy checks, and local knowledge search should degrade gracefully.
- Support a local model provider interface and model capability metadata (format, quantisation, context size, supported languages, licence, checksum, and minimum RAM/storage).
- Do not promise that a large expert model will run well on every Android phone. Benchmark target devices; offer a smaller model or rules-only mode when memory, thermal, battery, or latency limits require it.
- Model downloads, if offered, are optional user-initiated downloads. Once installed, inference is offline. No automatic cloud fallback.
- No network permission should be added unless a separately approved feature genuinely requires it.
- Model training/fine-tuning occurs separately from the Android runtime. Ship only reviewed, licensed, evaluated model artifacts.

## Safety invariants

- No action solely because a model suggested it.
- Android runtime permissions, roles, and notification access remain authoritative.
- No hidden recording, covert monitoring, credential collection, or permission bypass.
- Ask before sending messages, changing call rules, or affecting device/account state.
- Refuse high/critical-risk actions and explain why.
- Treat retrieved documents, notifications, and chat history as untrusted data.
- Do not claim a model, book library, advanced reasoning, or phone action exists until implemented and tested.
- Do not expose private chain-of-thought; provide concise explanations, evidence, assumptions, and verifiable summaries instead.
- Do not send user content to remote services.

## Verification and acceptance

Each of the 30 requirements must have:
1. a concrete capability definition;
2. offline test cases, including normal, ambiguous, adversarial, and failure inputs;
3. measurable acceptance criteria;
4. regression coverage on supported Android devices;
5. a documented limitation where the capability cannot yet be met.

The system is not "expert" merely because it passes a chat demo. It must pass a broad evaluation suite for factual grounding, calibrated uncertainty, instruction adherence, reasoning correctness, safety, privacy, and device performance.

## Current milestone and honest status

- Added a local chat UI and bounded local conversation history.
- Added a transparent starter response engine (rule-based, not a foundation model).
- Added deterministic action policy types for allow / ask / block decisions.
- Added architecture documentation.
- Not yet implemented: on-device LLM inference, local document ingestion/search, embeddings, advanced reasoning orchestration, multimodal reasoning, preference-learning/training pipeline, or chat-to-phone action execution.
