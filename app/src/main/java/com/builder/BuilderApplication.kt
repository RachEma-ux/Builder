package com.builder

import android.app.Application
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

        Timber.i("Builder application started")
        Timber.d("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        Timber.d("Device: ${android.os.Build.MODEL} (API ${android.os.Build.VERSION.SDK_INT})")
        Timber.d("Supported ABIs: ${android.os.Build.SUPPORTED_ABIS.joinToString()}")
    }
}
