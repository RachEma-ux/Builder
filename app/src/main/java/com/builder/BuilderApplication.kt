package com.builder

import android.app.Application
import com.builder.core.util.DebugLogger
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Main application class for Builder.
 * Initializes Hilt dependency injection and logging.
 */
@HiltAndroidApp
class BuilderApplication : Application() {

    @Inject
    lateinit var debugLogger: DebugLogger

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize debug file logger
        debugLogger.init(this)
        debugLogger.logSync("INFO", "App", "Builder application started")
        debugLogger.logSync("INFO", "App", "Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        debugLogger.logSync("INFO", "App", "Device: ${android.os.Build.MODEL} (API ${android.os.Build.VERSION.SDK_INT})")
        debugLogger.logSync("INFO", "App", "ABIs: ${android.os.Build.SUPPORTED_ABIS.joinToString()}")

        Timber.i("Builder application started")
        Timber.d("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        Timber.d("Device: ${android.os.Build.MODEL} (API ${android.os.Build.VERSION.SDK_INT})")
        Timber.d("Supported ABIs: ${android.os.Build.SUPPORTED_ABIS.joinToString()}")
    }
}
