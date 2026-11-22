package com.jusu.aicallassistant.intelligence

import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationFlowTest {
    private val brain = ExpertBrain()

    @Test fun greetingStartsConversationNaturally() {
        assertTrue(brain.respond("Hello").text.contains("ready to help"))
    }

    @Test fun affirmativeUsesRecentConversationContext() {
        val history = listOf(ChatTurn("user", "How does WhatsApp support work?"), ChatTurn("assistant", "WhatsApp support depends on notification actions."))
        val reply = brain.respond("Okay", history).text
        assertTrue(reply.contains("WhatsApp call support"))
        assertTrue(reply.contains("previous step"))
    }

    @Test fun nextStepUsesRecentTopic() {
        val history = listOf(ChatTurn("user", "Tell me about device diagnostics"), ChatTurn("assistant", "Diagnostics show RAM and storage."))
        assertTrue(brain.respond("What's next?", history).text.contains("run the diagnostics screen"))
    }

    @Test fun followUpAsksForSpecificDetailInsteadOfInventing() {
        val reply = brain.respond("Tell me more", listOf(ChatTurn("assistant", "Call screening needs Android permission.")))
        assertTrue(reply.text.contains("What part should I explain"))
    }

    @Test fun permissionBypassIsRefused() {
        assertTrue(brain.respond("How do I bypass permission?").text.contains("can't help"))
    }

    @Test fun unknownQuestionsAreHandledHonestly() {
        assertTrue(brain.respond("What is the capital of Mars?").text.contains("don't have enough verified information"))
    }
}
