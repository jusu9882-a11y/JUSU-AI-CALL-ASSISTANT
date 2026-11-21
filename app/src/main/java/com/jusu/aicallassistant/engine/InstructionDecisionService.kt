package com.jusu.aicallassistant.engine

import com.jusu.aicallassistant.data.InstructionRepository

/**
 * Connects persisted rules to the deterministic decision engine.
 * This is not Android call-screening integration yet.
 */
class InstructionDecisionService(private val repository: InstructionRepository) {
    private val engine = CallDecisionEngine()

    suspend fun evaluate(caller: CallerInfo): CallDecision {
        val profile = repository.activeProfile()
        return engine.decide(caller, repository.all(), profile)
    }
}
