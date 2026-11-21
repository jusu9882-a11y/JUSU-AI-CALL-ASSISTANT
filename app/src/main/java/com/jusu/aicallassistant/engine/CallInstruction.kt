package com.jusu.aicallassistant.engine

enum class CallerScope { ALL, CONTACTS, UNKNOWN, PHONE_NUMBER }
enum class CallAction { ALLOW, DECLINE, SCREEN, SILENCE }

data class CallInstruction(
    val id: String,
    val title: String,
    val scope: CallerScope,
    val action: CallAction,
    val phoneNumber: String? = null,
    val enabled: Boolean = true,
    val priority: Int = 0,
    val profile: String = "Personal"
) {
    init {
        require(id.isNotBlank()) { "Instruction id must not be blank" }
        require(title.isNotBlank()) { "Instruction title must not be blank" }
        if (scope == CallerScope.PHONE_NUMBER) {
            require(!phoneNumber.isNullOrBlank()) { "PHONE_NUMBER scope requires a number" }
        }
    }
}

data class CallerInfo(
    val phoneNumber: String? = null,
    val isContact: Boolean = false
) {
    val isUnknown: Boolean get() = !isContact
}
