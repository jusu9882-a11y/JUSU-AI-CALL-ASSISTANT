package com.jusu.aicallassistant.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallDecisionEngineTest {
    private val engine = CallDecisionEngine()

    @Test fun defaultIsAllowWhenNoRulesMatch() {
        val result = engine.decide(CallerInfo(phoneNumber = "123", isContact = false), emptyList())
        assertEquals(CallAction.ALLOW, result.action)
        assertNull(result.matchedInstruction)
    }

    @Test fun disabledRuleDoesNotMatch() {
        val rule = CallInstruction("r1", "Block unknown", CallerScope.UNKNOWN, CallAction.DECLINE, enabled = false)
        val result = engine.decide(CallerInfo(isContact = false), listOf(rule))
        assertEquals(CallAction.ALLOW, result.action)
    }

    @Test fun profileFiltersRules() {
        val rule = CallInstruction("r1", "Privacy block", CallerScope.ALL, CallAction.DECLINE, profile = "Privacy")
        val result = engine.decide(CallerInfo(isContact = true), listOf(rule), "Personal")
        assertEquals(CallAction.ALLOW, result.action)
    }

    @Test fun higherPriorityRuleWins() {
        val low = CallInstruction("a", "Allow everyone", CallerScope.ALL, CallAction.ALLOW, priority = 1)
        val high = CallInstruction("b", "Screen unknown", CallerScope.UNKNOWN, CallAction.SCREEN, priority = 10)
        val result = engine.decide(CallerInfo(isContact = false), listOf(low, high))
        assertEquals(CallAction.SCREEN, result.action)
        assertEquals("b", result.matchedInstruction?.id)
    }

    @Test fun numberSpecificRuleWinsTie() {
        val broad = CallInstruction("a", "Allow all", CallerScope.ALL, CallAction.ALLOW, priority = 5)
        val specific = CallInstruction("z", "Decline number", CallerScope.PHONE_NUMBER, CallAction.DECLINE, phoneNumber = "+1 (555) 123-4567", priority = 5)
        val result = engine.decide(CallerInfo(phoneNumber = "15551234567", isContact = true), listOf(broad, specific))
        assertEquals(CallAction.DECLINE, result.action)
    }

    @Test fun numberMatchingIgnoresFormatting() {
        val rule = CallInstruction("n", "Match number", CallerScope.PHONE_NUMBER, CallAction.SILENCE, phoneNumber = "+44 20 1234 5678")
        val result = engine.decide(CallerInfo(phoneNumber = "442012345678", isContact = false), listOf(rule))
        assertEquals(CallAction.SILENCE, result.action)
    }
}
