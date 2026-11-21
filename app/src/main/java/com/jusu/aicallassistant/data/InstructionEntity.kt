package com.jusu.aicallassistant.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jusu.aicallassistant.engine.CallAction
import com.jusu.aicallassistant.engine.CallerScope
import com.jusu.aicallassistant.engine.CallInstruction

@Entity(tableName = "call_instructions")
data class InstructionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val scope: String,
    val action: String,
    val phoneNumber: String?,
    val enabled: Boolean,
    val priority: Int,
    val profile: String
) {
    fun toDomain() = CallInstruction(
        id = id, title = title,
        scope = CallerScope.valueOf(scope),
        action = CallAction.valueOf(action),
        phoneNumber = phoneNumber, enabled = enabled,
        priority = priority, profile = profile
    )

    companion object {
        fun fromDomain(rule: CallInstruction) = InstructionEntity(
            id = rule.id, title = rule.title, scope = rule.scope.name,
            action = rule.action.name, phoneNumber = rule.phoneNumber,
            enabled = rule.enabled, priority = rule.priority, profile = rule.profile
        )
    }
}
