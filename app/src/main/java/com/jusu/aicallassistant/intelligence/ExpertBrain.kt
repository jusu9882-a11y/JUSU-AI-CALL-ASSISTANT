package com.jusu.aicallassistant.intelligence

data class ChatTurn(val role: String, val text: String, val timestamp: Long = System.currentTimeMillis())
data class BrainReply(val text: String, val mode: String = "Local rules", val needsConfirmation: Boolean = false)

/**
 * First-stage offline conversation brain. This is intentionally transparent:
 * it uses explicit rules and does not pretend to be a trained large language model.
 * A future on-device/cloud model can propose answers, while DecisionPolicy remains
 * the mandatory action gate.
 */
class ExpertBrain {
    fun respond(message: String, history: List<ChatTurn> = emptyList()): BrainReply {
        val input = message.trim()
        if (input.isBlank()) return BrainReply("Please type a message so I can help.")

        val lower = input.lowercase()
        return when {
            lower.matches(Regex("^(hi|hello|hey|good morning|good afternoon|good evening)[.! ]*$")) ->
                BrainReply("Hello! I'm JUSU. I can help you think through questions and manage supported call-assistant settings. What would you like to do?")
            listOf("who are you", "what are you", "introduce yourself").any { lower.contains(it) } ->
                BrainReply("I'm JUSU AI CALL ASSISTANT. My current chat foundation runs locally using transparent rules. Advanced language-model reasoning and research retrieval are planned but are not connected yet.")
            listOf("what can you do", "your features", "help me").any { lower.contains(it) } ->
                BrainReply("I can explain the current call-assistant features, help organise caller instructions, and discuss safe ways to use the app. I cannot yet control calls through chat or perform arbitrary phone actions.")
            listOf("ignore your rules", "bypass permission", "without permission", "steal", "spy on").any { lower.contains(it) } ->
                BrainReply("I can't help bypass permissions, invade privacy, or secretly access information. I can suggest a consent-based alternative.")
            lower.contains("call") && (lower.contains("decline") || lower.contains("reject") || lower.contains("block")) ->
                BrainReply("I can explain the current call controls. Cellular screening depends on the Android call-screening role and saved rules. WhatsApp support is limited to notification actions that WhatsApp itself exposes. I won't execute a call action from chat without a separate, explicit confirmation and platform authorisation.")
            lower.contains("remember") || lower.contains("forget") ->
                BrainReply("Memory controls are not connected to chat yet. For now, don't send secrets or sensitive personal information here.")
            lower.contains("why") || lower.contains("explain") || lower.contains("reason") ->
                BrainReply("I'll explain what I can verify. My current offline chat engine uses predefined rules rather than a general-purpose reasoning model, so I may need you to clarify the question.")
            else -> BrainReply("I understand that you're asking: \"$input\". My local chat foundation is active, but I don't yet have a full language model or research library connected. I won't invent an answer. Please ask about the current app features, or we can expand the knowledge and reasoning modules next.")
        }
    }
}
