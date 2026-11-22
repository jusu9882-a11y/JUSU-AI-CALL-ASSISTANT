package com.jusu.aicallassistant.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalModelRuntimeContractTest {
    @Test
    fun runtimeStartsWithoutAnInstalledModel() {
        val status = RuntimeStatus(RuntimeState.NOT_INSTALLED)
        assertEquals(RuntimeState.NOT_INSTALLED, status.state)
        assertNull(status.modelId)
    }

    @Test
    fun modelDescriptorRetainsIntegrityAndLicenceMetadata() {
        val model = InstalledModel(
            id = "test-small-model",
            displayName = "Test model",
            absolutePath = "/models/test.gguf",
            sha256 = "a".repeat(64),
            expectedBytes = 1024,
            format = "GGUF",
            quantization = "Q4",
            licence = "Test licence",
            minimumRecommendedRamMb = 2048,
            contextLimitTokens = 2048
        )

        assertEquals(64, model.sha256.length)
        assertEquals("GGUF", model.format)
        assertTrue(model.expectedBytes > 0)
    }

    @Test
    fun failedGenerationCanReportAnExplicitReason() {
        val result: GenerationResult = GenerationResult.Failed("Model is not loaded")
        assertTrue(result is GenerationResult.Failed)
        assertEquals("Model is not loaded", (result as GenerationResult.Failed).reason)
    }
}
