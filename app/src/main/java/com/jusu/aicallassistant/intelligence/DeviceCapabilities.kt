package com.jusu.aicallassistant.intelligence

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.StatFs

/** Snapshot of device resources used to decide whether a local model is safe to load. */
data class DeviceProfile(
    val totalMemoryMb: Long,
    val availableMemoryMb: Long,
    val freeStorageMb: Long,
    val lowMemory: Boolean,
    val sdkInt: Int,
    val supportedAbis: List<String>,
    val thermalStatus: Int?
)

object DeviceCapabilityInspector {
    fun inspect(context: Context): DeviceProfile {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val storage = StatFs(context.filesDir.absolutePath)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

        return DeviceProfile(
            totalMemoryMb = memory.totalMem / MB,
            availableMemoryMb = memory.availMem / MB,
            freeStorageMb = storage.availableBytes / MB,
            lowMemory = memory.lowMemory,
            sdkInt = Build.VERSION.SDK_INT,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            thermalStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                powerManager?.currentThermalStatus
            } else null
        )
    }

    private const val MB = 1024L * 1024L
}

enum class ModelReadiness {
    READY,
    INVALID_REQUIREMENTS,
    INSUFFICIENT_TOTAL_MEMORY,
    LOW_AVAILABLE_MEMORY,
    INSUFFICIENT_STORAGE,
    LOW_MEMORY_AND_STORAGE
}

/** Pure policy so device-fit decisions can be tested without an Android device. */
object ModelReadinessEvaluator {
    fun evaluate(
        device: DeviceProfile,
        minimumRecommendedRamMb: Int,
        modelBytes: Long
    ): ModelReadiness {
        if (minimumRecommendedRamMb <= 0 || modelBytes <= 0L) {
            return ModelReadiness.INVALID_REQUIREMENTS
        }

        val totalMemoryTooLow = device.totalMemoryMb < minimumRecommendedRamMb.toLong()
        val minimumFreeMemoryMb = maxOf(512L, minimumRecommendedRamMb / 4L)
        val availableMemoryTooLow = device.lowMemory ||
            device.availableMemoryMb < minimumFreeMemoryMb
        val requiredStorageMb = (modelBytes + modelBytes / 5L) / MB
        val storageTooLow = device.freeStorageMb < requiredStorageMb

        if ((totalMemoryTooLow || availableMemoryTooLow) && storageTooLow) {
            return ModelReadiness.LOW_MEMORY_AND_STORAGE
        }
        if (totalMemoryTooLow) return ModelReadiness.INSUFFICIENT_TOTAL_MEMORY
        if (availableMemoryTooLow) return ModelReadiness.LOW_AVAILABLE_MEMORY
        if (storageTooLow) return ModelReadiness.INSUFFICIENT_STORAGE
        return ModelReadiness.READY
    }

    private const val MB = 1024L * 1024L
}
