# CMakeLists.txt for Wasmtime JNI bridge
cmake_minimum_required(VERSION 3.22.1)

project("wasmtime_jni")

# Set C standard
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

# Find Java for JNI - but don't fail if desktop components are missing
find_package(JNI)

# On Android, the NDK provides JNI headers
# We don't need the full JDK components (AWT, JVM libs, etc.)
if(ANDROID)
    # Android NDK provides JNI headers
    if(NOT JNI_INCLUDE_DIRS)
        # Fallback: Use NDK's JNI headers directly
        set(JNI_INCLUDE_DIRS "${ANDROID_NDK}/sysroot/usr/include")
    endif()
    message(STATUS "Android build - using NDK JNI headers: ${JNI_INCLUDE_DIRS}")
else()
    # Desktop builds need full JNI
    if(NOT JNI_FOUND)
        message(FATAL_ERROR "JNI not found. Please install JDK and set JAVA_HOME.")
    endif()
endif()

# Include directories
include_directories(
    ${JNI_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

# Check if Wasmtime library exists
set(WASMTIME_LIB_PATH "${CMAKE_CURRENT_SOURCE_DIR}/libs/${ANDROID_ABI}/libwasmtime.so")
if(EXISTS ${WASMTIME_LIB_PATH})
    message(STATUS "Found Wasmtime library: ${WASMTIME_LIB_PATH}")
    set(WASMTIME_AVAILABLE TRUE)

    # Import Wasmtime as a prebuilt library
    add_library(wasmtime SHARED IMPORTED)
    set_target_properties(wasmtime PROPERTIES
        IMPORTED_LOCATION ${WASMTIME_LIB_PATH}
    )
else()
    message(WARNING "Wasmtime library not found at: ${WASMTIME_LIB_PATH}")
    message(WARNING "JNI bridge will be built but WASM execution will fail at runtime.")
    message(WARNING "Run scripts/build-wasmtime.sh to build Wasmtime for Android.")
    set(WASMTIME_AVAILABLE FALSE)
endif()

# Source files
add_library(
    wasmtime_jni
    SHARED
    src/wasmtime_jni.c
)

# Link libraries
if(WASMTIME_AVAILABLE)
    target_link_libraries(
        wasmtime_jni
        ${JNI_LIBRARIES}
        wasmtime
        log
    )
else()
    target_link_libraries(
        wasmtime_jni
        ${JNI_LIBRARIES}
        log
    )
endif()

# Set output directory
set_target_properties(
    wasmtime_jni
    PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/../app/src/main/jniLibs/${ANDROID_ABI}
)

# Add compile definitions
if(WASMTIME_AVAILABLE)
    target_compile_definitions(wasmtime_jni PRIVATE WASMTIME_ENABLED=1)
else()
    target_compile_definitions(wasmtime_jni PRIVATE WASMTIME_ENABLED=0)
endif()

# Optimization flags for release
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    target_compile_options(wasmtime_jni PRIVATE -O3 -DNDEBUG)
endif()
