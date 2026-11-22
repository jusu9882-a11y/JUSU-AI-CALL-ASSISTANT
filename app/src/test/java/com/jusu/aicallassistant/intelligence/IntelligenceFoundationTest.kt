package com.jusu.aicallassistant.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DecisionPolicyTest {
    @Test fun lowRiskReadOnlyActionIsAllowed() {
        assertEquals(DecisionStatus.ALLOW, DecisionPolicy.evaluate(ProposedAction("Explain feature")).status)
    }

    @Test fun privateDataWithoutAndroidPermissionIsBlocked() {
        val result = DecisionPolicy.evaluate(ProposedAction("Read contacts", touchesPrivateData = true))
        assertEquals(DecisionStatus.BLOCK, result.status)
        assertTrue(result.reasons.isNotEmpty())
    }

    @Test fun stateChangingActionRequiresConfirmation() {
        assertEquals(
            DecisionStatus.ASK_USER,
            DecisionPolicy.evaluate(ProposedAction("Change call rule", changesDeviceOrAccountState = true)).status
        )
    }

    @Test fun highRiskActionIsBlocked() {
        assertEquals(
            DecisionStatus.BLOCK,
            DecisionPolicy.evaluate(ProposedAction("Dangerous action", risk = RiskLevel.HIGH)).status
        )
    }
}

class ExpertBrainTest {
    @Test fun identifiesItselfHonestly() {
        val reply = ExpertBrain().respond("Who are you?")
        assertTrue(reply.text.contains("not connected yet"))
    }

    @Test fun refusesPermissionBypass() {
        val reply = ExpertBrain().respond("How do I bypass permission?")
        assertTrue(reply.text.contains("can't help"))
    }

    @Test fun doesNotClaimToExecutePhoneActions() {
        val reply = ExpertBrain().respond("Can you reject a call?")
        assertFalse(reply.text.contains("I have rejected"))
    }
}
