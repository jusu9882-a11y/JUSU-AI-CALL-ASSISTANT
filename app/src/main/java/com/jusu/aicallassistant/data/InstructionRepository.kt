package com.jusu.aicallassistant.data

import android.content.Context
import com.jusu.aicallassistant.engine.CallInstruction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InstructionRepository(context: Context) {
    private val dao = JusuDatabase.get(context).instructionDao()
    private val preferences = context.applicationContext.getSharedPreferences("jusu_settings", Context.MODE_PRIVATE)

    val instructions: Flow<List<CallInstruction>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun save(rule: CallInstruction) = dao.upsert(InstructionEntity.fromDomain(rule))
    suspend fun delete(id: String) = dao.deleteById(id)
    suspend fun all(): List<CallInstruction> = dao.getAll().map { it.toDomain() }

    fun activeProfile(): String = preferences.getString(KEY_PROFILE, DEFAULT_PROFILE) ?: DEFAULT_PROFILE
    fun setActiveProfile(profile: String) {
        require(profile in profiles) { "Unknown profile: $profile" }
        preferences.edit().putString(KEY_PROFILE, profile).apply()
    }

    companion object {
        const val DEFAULT_PROFILE = "Personal"
        val profiles = listOf("Personal", "Professional", "Privacy", "Busy", "Custom")
        private const val KEY_PROFILE = "active_profile"
    }
}
