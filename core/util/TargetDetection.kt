package com.builder.core.util

/**
 * Detects the device target platform for pack selection.
 * See Builder_Final.md §10 for target detection specification.
 */
object TargetDetection {
    /**
     * Android ARM64 target identifier.
     */
    const val ANDROID_ARM64 = "android-arm64"

    /**
     * Android universal (fallback) target identifier.
     */
    const val ANDROID_UNIVERSAL = "android-universal"

    /**
     * Detects the current device target.
     * For Android:
     * - ABI arm64-v8a → android-arm64
     * - otherwise → android-universal
     *
     * @param supportedAbis List of supported ABIs (from Build.SUPPORTED_ABIS)
     * @return Target identifier
     */
    fun detect(supportedAbis: Array<String>): String {
        return if (supportedAbis.contains("arm64-v8a")) {
            ANDROID_ARM64
        } else {
            ANDROID_UNIVERSAL
        }
    }

    /**
     * Returns the preferred target order for the device.
     * Tries ARM64 first, then falls back to universal.
     *
     * @param deviceTarget The detected device target
     * @return List of targets in preference order
     */
    fun preferredTargets(deviceTarget: String): List<String> {
        return when (deviceTarget) {
            ANDROID_ARM64 -> listOf(ANDROID_ARM64, ANDROID_UNIVERSAL)
            ANDROID_UNIVERSAL -> listOf(ANDROID_UNIVERSAL)
            else -> listOf(ANDROID_UNIVERSAL)
        }
    }

    /**
     * Checks if a target is compatible with the device.
     *
     * @param deviceTarget The detected device target
     * @param packTarget The pack target to check
     * @return true if compatible, false otherwise
     */
    fun isCompatible(deviceTarget: String, packTarget: String): Boolean {
        return preferredTargets(deviceTarget).contains(packTarget)
    }
}
