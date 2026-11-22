package com.jusu.aicallassistant.intelligence

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelReadinessEvaluatorTest {
    private val healthyDevice = DeviceProfile(
        totalMemoryMb = 4096,
        availableMemoryMb = 2048,
        freeStorageMb = 4096,
        lowMemory = false,
        sdkInt = 31,
        supportedAbis = listOf("arm64-v8a"),
        thermalStatus = null
    )

    @Test
    fun acceptsModelWhenMemoryAndStorageAreSufficient() {
        assertEquals(
            ModelReadiness.READY,
            ModelReadinessEvaluator.evaluate(healthyDevice, 2048, 512L * 1024L * 1024L)
        )
    }

    @Test
    fun rejectsModelWhenTotalMemoryIsBelowRecommendation() {
        assertEquals(
            ModelReadiness.INSUFFICIENT_TOTAL_MEMORY,
            ModelReadinessEvaluator.evaluate(healthyDevice.copy(totalMemoryMb = 1024), 2048, 512L * 1024L * 1024L)
        )
    }

    @Test
    fun rejectsModelWhenAvailableMemoryIsTooLow() {
        assertEquals(
            ModelReadiness.LOW_AVAILABLE_MEMORY,
            ModelReadinessEvaluator.evaluate(healthyDevice.copy(availableMemoryMb = 256), 2048, 512L * 1024L * 1024L)
        )
    }

    @Test
    fun rejectsModelWhenStorageIsTooLow() {
        assertEquals(
            ModelReadiness.INSUFFICIENT_STORAGE,
            ModelReadinessEvaluator.evaluate(healthyDevice.copy(freeStorageMb = 100), 2048, 512L * 1024L * 1024L)
        )
    }

    @Test
    fun rejectsInvalidModelRequirements() {
        assertEquals(
            ModelReadiness.INVALID_REQUIREMENTS,
            ModelReadinessEvaluator.evaluate(healthyDevice, 0, 0)
        )
    }
}
