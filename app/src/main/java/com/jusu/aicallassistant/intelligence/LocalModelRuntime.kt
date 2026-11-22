package com.jusu.aicallassistant.intelligence

/**
 * App-facing boundary for a genuinely on-device language model.
 *
 * Implementations may block while loading or generating. Callers MUST invoke
 * these methods from a dedicated worker thread, never the Android main thread.
 * Implementations must not use network services or silently fall back to cloud.
 */
interface LocalModelRuntime {
    fun status(): RuntimeStatus
    fun load(model: InstalledModel): ModelLoadResult
    fun generate(request: GenerationRequest, onToken: (String) -> Unit = {}): GenerationResult
    fun cancel()
    fun unload()
}

enum class RuntimeState {
    NOT_INSTALLED,
    READY_TO_LOAD,
    LOADING,
    READY,
    GENERATING,
    OUT_OF_MEMORY,
    FAILED,
    UNSUPPORTED_DEVICE
}

data class RuntimeStatus(
    val state: RuntimeState,
    val modelId: String? = null,
    val detail: String? = null
)

/**
 * Metadata has already been parsed from a local manifest. The runtime must
 * independently verify the file checksum and compatibility before loading.
 */
data class InstalledModel(
    val id: String,
    val displayName: String,
    val absolutePath: String,
    val sha256: String,
    val expectedBytes: Long,
    val format: String,
    val quantization: String,
    val licence: String,
    val minimumRecommendedRamMb: Int,
    val contextLimitTokens: Int
)

data class GenerationRequest(
    val prompt: String,
    val maxOutputTokens: Int,
    val contextBudgetTokens: Int,
    val temperature: Float = 0.7f
)

sealed class ModelLoadResult {
    data object Loaded : ModelLoadResult()
    data class Rejected(val reason: String) : ModelLoadResult()
    data class Failed(val reason: String) : ModelLoadResult()
}

sealed class GenerationResult {
    data class Completed(val text: String) : GenerationResult()
    data object Cancelled : GenerationResult()
    data class Failed(val reason: String) : GenerationResult()
}

/**
 * No concrete native provider is wired in this milestone. The chat coordinator
 * should keep using its deterministic fallback unless status() is READY.
 */
