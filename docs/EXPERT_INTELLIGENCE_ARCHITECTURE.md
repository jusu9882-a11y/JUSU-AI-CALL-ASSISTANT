# JUSU Expert Intelligence Architecture

## Product decision
JUSU is an Android-first call assistant with expert-level ambition. Its scope is both conversational chat and authorised phone-assistant operations. "Expert" is a target capability, not a claim that a foundation model is already installed.

## Core layers
1. **Conversation interface** — collects messages, shows replies, and stores a bounded local history.
2. **Language understanding** — intent, entities, context, and ambiguity resolution. The first release uses explicit local rules; a language model is a later replaceable provider.
3. **Knowledge retrieval** — curated, licensed or public-domain material indexed by subject, source, date, and reliability. Answers must retain citations/provenance.
4. **Reasoning orchestration** — breaks requests into steps, compares options, tracks uncertainty, and asks clarifying questions.
5. **Decision policy** — deterministic checks for risk, consent, privacy, and Android authorisation. Model output cannot override this layer.
6. **Action gateway** — typed allowlisted operations only. Every state-changing or sensitive action requires explicit user confirmation and the relevant Android permission.
7. **Audit and evaluation** — tests policy decisions, records minimal local decision metadata, and supports user deletion/export.

## Safety invariants
- No action solely because a model suggested it.
- Android runtime permissions, roles, and notification access remain authoritative.
- No hidden recording, covert monitoring, credential collection, or permission bypass.
- Ask before sending messages, changing call rules, or affecting device/account state.
- Refuse high/critical-risk actions and explain why.
- Treat retrieved documents and chat history as untrusted data, never as executable instructions.
- Do not claim a model, book library, web research, or phone action exists until it is implemented and tested.

## Knowledge library
Use a curated collection of public-domain, openly licensed, or properly licensed sources. Store title, author, publisher, edition, publication date, licence, subject tags, extracted text, and page/section provenance. Retrieval should return short grounded passages with source references. Avoid ingesting copyrighted books without permission.

## Model strategy
Keep a provider interface so an on-device model can be added without replacing the safety gate. Cloud model support, if ever enabled, must be optional, disclosed, and require a network/privacy setting. Basic chat and call rules should continue to work offline.

## Current milestone
- Added a local chat UI and bounded local conversation history.
- Added a transparent starter response engine (rule-based, not a foundation model).
- Added deterministic action policy types for allow / ask / block decisions.
- Added architecture documentation.
- Not yet implemented: actual LLM inference, document ingestion/search, embeddings, advanced reasoning, or chat-to-phone action execution.
