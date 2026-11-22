package com.jusu.aicallassistant.intelligence

enum class RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }
enum class DecisionStatus { ALLOW, ASK_USER, BLOCK }

data class ProposedAction(
    val name: String,
    val requiresExplicitConsent: Boolean = false,
    val touchesPrivateData: Boolean = false,
    val changesDeviceOrAccountState: Boolean = false,
    val risk: RiskLevel = RiskLevel.LOW,
    val authorisedByAndroid: Boolean = false
)

data class PolicyDecision(
    val status: DecisionStatus,
    val reasons: List<String>
)

/**
 * Deterministic safety gate. A language model may suggest actions, but it must
 * never override this policy or Android's permission system.
 */
object DecisionPolicy {
    fun evaluate(action: ProposedAction): PolicyDecision {
        val reasons = mutableListOf<String>()
        if (action.risk == RiskLevel.CRITICAL) {
            return PolicyDecision(DecisionStatus.BLOCK, listOf("The action is classified as critical risk."))
        }
        if (action.risk == RiskLevel.HIGH) {
            return PolicyDecision(DecisionStatus.BLOCK, listOf("The action is high risk and is not supported by the assistant."))
        }
        if (action.touchesPrivateData && !action.authorisedByAndroid) {
            reasons += "The required Android permission has not been granted."
            return PolicyDecision(DecisionStatus.BLOCK, reasons)
        }
        if (action.requiresExplicitConsent || action.changesDeviceOrAccountState) {
            reasons += "The user must explicitly confirm this action."
            return PolicyDecision(DecisionStatus.ASK_USER, reasons)
        }
        return PolicyDecision(DecisionStatus.ALLOW, listOf("The action is low risk and requires no additional authority."))
    }
}
