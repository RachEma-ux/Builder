package com.builder

import android.app.Application
import com.builder.core.util.DebugLogger
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Main application class for Builder.
 * Initializes Hilt dependency injection and logging.
 */
@HiltAndroidApp
class BuilderApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize debug file logger
        DebugLogger.init(cacheDir)
        DebugLogger.logSync("INFO", "App", "Builder application started")
        DebugLogger.logSync("INFO", "App", "Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        DebugLogger.logSync("INFO", "App", "Device: ${android.os.Build.MODEL} (API ${android.os.Build.VERSION.SDK_INT})")
        DebugLogger.logSync("INFO", "App", "ABIs: ${android.os.Build.SUPPORTED_ABIS.joinToString()}")

        Timber.i("Builder application started")
        Timber.d("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        Timber.d("Device: ${android.os.Build.MODEL} (API ${android.os.Build.VERSION.SDK_INT})")
        Timber.d("Supported ABIs: ${android.os.Build.SUPPORTED_ABIS.joinToString()}")
    }
}
