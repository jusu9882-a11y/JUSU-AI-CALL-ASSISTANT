package com.jusu.aicallassistant.intelligence

data class ChatTurn(val role: String, val text: String, val timestamp: Long = System.currentTimeMillis())
data class BrainReply(val text: String, val mode: String = "Local rules", val needsConfirmation: Boolean = false)

/** Transparent first-stage offline conversation brain; not a trained language model. */
class ExpertBrain {
    fun respond(message: String, history: List<ChatTurn> = emptyList()): BrainReply {
        val input = message.trim()
        if (input.isBlank()) return BrainReply("Please type a message so I can help.")
        val lower = input.lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ").trim()
        val lastAssistant = history.lastOrNull { it.role == "assistant" }?.text.orEmpty()
        val lastUser = history.lastOrNull { it.role == "user" }?.text.orEmpty()
        val topic = inferTopic(lastUser, lastAssistant)

        return when {
            isGreeting(lower) -> BrainReply("Hello! I'm JUSU. I'm ready to help. We can talk about call protection, privacy, app settings, or the next development step.")
            listOf("who are you", "what are you", "introduce yourself").any { lower.contains(it) } ->
                BrainReply("I'm JUSU AI CALL ASSISTANT. This conversation engine currently runs locally using transparent rules. A full on-device language model is not connected yet, so I will be clear about what I can and cannot answer.")
            listOf("what can you do", "your features", "help me", "help").any { lower == it || lower.contains(it) } ->
                BrainReply("I can explain JUSU's current call-assistant features, privacy and permission requirements, and help you configure supported settings. I cannot yet control calls through chat or perform arbitrary phone actions. Try asking: How does call screening work?")
            isAffirmative(lower) -> BrainReply(affirmativeReply(topic, lastAssistant))
            isNextStep(lower) -> BrainReply(nextStepReply(topic))
            isFollowUp(lower) -> BrainReply(followUpReply(topic, lastAssistant))
            listOf("ignore your rules", "bypass permission", "without permission", "steal", "spy on").any { lower.contains(it) } ->
                BrainReply("I can't help bypass permissions, invade privacy, or secretly access information. I can help with a consent-based alternative.")
            lower.contains("call") && listOf("decline", "reject", "block").any { lower.contains(it) } ->
                BrainReply("For cellular calls, JUSU depends on Android's call-screening role and your saved rules. For WhatsApp, support is limited to actions WhatsApp itself exposes in a notification. Chat cannot execute a call action; platform authorisation and a separate user confirmation are required.")
            lower.contains("permission") || lower.contains("privacy") ->
                BrainReply("JUSU must use Android's own permission and role prompts. You can refuse any request; the related feature will remain unavailable or limited. JUSU must not bypass permissions or access private data secretly.")
            lower.contains("remember") || lower.contains("forget") ->
                BrainReply("Persistent conversational memory controls are not connected yet. The current chat history is stored locally on this device and can be cleared from the chat screen. Please do not store passwords or other secrets here.")
            lower.contains("why") || lower.contains("explain") || lower.contains("reason") ->
                BrainReply("I'll explain what I can verify. The current offline chat engine uses predefined rules and recent conversation context, not a general-purpose reasoning model. If I misunderstood, tell me which part you mean.")
            else -> BrainReply("I understand your message: “$input”. I don't have enough verified information to answer that reliably yet. Could you add one detail or tell me whether this is about calls, WhatsApp, permissions, chat, or the offline AI model?")
        }
    }

    private fun inferTopic(lastUser: String, lastAssistant: String): String {
        val text = "$lastUser $lastAssistant".lowercase()
        return when {
            "whatsapp" in text -> "WhatsApp call support"
            "diagnostic" in text || "ram" in text || "storage" in text || "processor" in text -> "device diagnostics"
            "model" in text || "offline ai" in text || "language model" in text -> "the offline AI model"
            "permission" in text || "privacy" in text -> "permissions and privacy"
            "call" in text || "screening" in text -> "call protection"
            "chat" in text || "conversation" in text -> "the conversation experience"
            else -> "JUSU"
        }
    }

    private fun affirmativeReply(topic: String, previous: String): String {
        if (previous.isBlank()) return "Okay. What would you like me to help you with first?"
        val excerpt = previous.take(220) + if (previous.length > 220) "…" else ""
        return "Okay — let's continue with $topic. The previous step was: $excerpt Tell me “next” to move forward, or ask a question about this step."
    }

    private fun nextStepReply(topic: String): String = when (topic) {
        "device diagnostics" -> "The next step is to run the diagnostics screen on this phone and review live RAM, free storage, Android API level and processor ABIs. Those readings help us choose a safe local model."
        "the offline AI model" -> "The next step is to validate a model file before loading it: check its format, source and licence, calculate its SHA-256 hash, then compare its memory and storage requirements with this phone."
        "WhatsApp call support" -> "The next step is to test with a real incoming WhatsApp call and confirm which notification actions Android exposes. JUSU must not assume it can answer or decline calls when WhatsApp does not provide an action."
        "permissions and privacy" -> "The next step is to review each Android permission and test the feature both when permission is granted and when it is denied."
        "call protection" -> "The next step is to test the Android call-screening role on a real device, then verify saved rules with allowed and blocked test numbers."
        "the conversation experience" -> "The next step is to test a multi-message conversation, clearing local history and reopening chat to confirm saved messages are restored."
        else -> "The next step is to choose one feature: call protection, WhatsApp support, permissions, device diagnostics, or the offline AI model."
    }

    private fun followUpReply(topic: String, previous: String): String {
        if (previous.isBlank()) return "Of course. What would you like me to explain?"
        return "Sure. We're discussing $topic. I can expand on the last point, but I don't want to guess which detail you mean. What part should I explain?"
    }

    private fun isGreeting(value: String) = value.matches(Regex("^(hi|hello|hey|good morning|good afternoon|good evening|how are you)$"))
    private fun isAffirmative(value: String) = value in setOf("yes", "yeah", "yep", "ok", "okay", "proceed", "continue", "go ahead", "lets go", "let us go")
    private fun isNextStep(value: String) = value in setOf("what next", "whats next", "next", "next step", "what should i do next")
    private fun isFollowUp(value: String) = value in setOf("tell me more", "explain more", "go on", "continue explaining", "why is that", "how so")
}
