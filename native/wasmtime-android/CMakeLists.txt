# CMakeLists.txt for Wasmtime JNI bridge
cmake_minimum_required(VERSION 3.22.1)

project("wasmtime_jni")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find Java for JNI
find_package(JNI REQUIRED)

# Include directories
include_directories(
    ${JNI_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

# Source files
add_library(
    wasmtime_jni
    SHARED
    src/wasmtime_jni.c
)

# Link libraries
target_link_libraries(
    wasmtime_jni
    ${JNI_LIBRARIES}
    log
    # TODO: Link Wasmtime library when available
    # ${CMAKE_CURRENT_SOURCE_DIR}/libs/${ANDROID_ABI}/libwasmtime.a
)

# Set output directory
set_target_properties(
    wasmtime_jni
    PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/../app/src/main/jniLibs/${ANDROID_ABI}
)
