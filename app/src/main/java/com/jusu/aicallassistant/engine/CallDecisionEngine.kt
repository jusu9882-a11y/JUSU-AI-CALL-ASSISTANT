package com.jusu.aicallassistant.engine

data class CallDecision(
    val action: CallAction,
    val matchedInstruction: CallInstruction?,
    val reason: String
)

class CallDecisionEngine {
    fun decide(
        caller: CallerInfo,
        instructions: List<CallInstruction>,
        activeProfile: String = "Personal"
    ): CallDecision {
        val candidates = instructions.filter { it.enabled && it.profile == activeProfile && matches(it, caller) }
        val winner = candidates.sortedWith(
            compareByDescending<CallInstruction> { it.priority }
                .thenByDescending { specificity(it.scope) }
                .thenBy { it.id }
        ).firstOrNull()

        return if (winner == null) {
            CallDecision(CallAction.ALLOW, null, "No enabled instruction matched; default is allow.")
        } else {
            CallDecision(winner.action, winner, "Matched instruction: ${winner.title}")
        }
    }

    private fun matches(rule: CallInstruction, caller: CallerInfo): Boolean = when (rule.scope) {
        CallerScope.ALL -> true
        CallerScope.CONTACTS -> caller.isContact
        CallerScope.UNKNOWN -> !caller.isContact
        CallerScope.PHONE_NUMBER -> normalize(rule.phoneNumber) == normalize(caller.phoneNumber)
    }

    private fun specificity(scope: CallerScope): Int = when (scope) {
        CallerScope.PHONE_NUMBER -> 3
        CallerScope.CONTACTS, CallerScope.UNKNOWN -> 2
        CallerScope.ALL -> 1
    }

    private fun normalize(number: String?): String? = number?.filter { it.isDigit() }.takeIf { it.isNotEmpty() }
}
