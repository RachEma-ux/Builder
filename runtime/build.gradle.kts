plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace = "com.builder.runtime"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

        // Only configure native build if Wasmtime library exists
        val wasmtimeLibExists = file("../native/wasmtime-android/libs/arm64-v8a/libwasmtime.so").exists() ||
                                file("../native/wasmtime-android/libs/x86_64/libwasmtime.so").exists()

        if (wasmtimeLibExists) {
            externalNativeBuild {
                cmake {
                    cppFlags += "-std=c++17"
                    arguments += listOf(
                        "-DANDROID_STL=c++_shared",
                        "-DANDROID_PLATFORM=android-26"
                    )
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Only enable native build if Wasmtime library exists
    val wasmtimeLibExists = file("../native/wasmtime-android/libs/arm64-v8a/libwasmtime.so").exists() ||
                            file("../native/wasmtime-android/libs/x86_64/libwasmtime.so").exists()

    if (wasmtimeLibExists) {
        externalNativeBuild {
            cmake {
                path = file("../native/wasmtime-android/CMakeLists.txt")
                version = "3.22.1"
            }
        }
        println("✅ Wasmtime library found - native build enabled")
    } else {
        println("⚠️  Wasmtime library not found - native build disabled")
        println("   WASM execution will not be available at runtime")
        println("   Run ./scripts/build-wasmtime.sh to enable WASM support")
    }
}

dependencies {
    // Project modules
    implementation(project(":core"))

    // AndroidX
    implementation("androidx.core:core-ktx:1.12.0")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-android-compiler:2.50")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Networking (for workflow HTTP steps)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Timber for logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

kapt {
    correctErrorTypes = true

    // Explicitly set JVM target for kapt to avoid inference from running JDK
    javacOptions {
        option("-source", "17")
        option("-target", "17")
    }
}

// Force kapt tasks to use JVM target 17
tasks.withType<org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}
