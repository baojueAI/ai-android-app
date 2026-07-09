cmake_minimum_required(VERSION 3.22.1)

set(ANDROID_STL c++_shared)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

project("aichat-native" CXX)

# ============================================================
# Build all third-party libs as STATIC to eliminate cross-lib
# dependency issues at runtime on certain Android ROMs
# ============================================================
# Save original and force static libs for third-party
set(BUILD_SHARED_LIBS_BACKUP ${BUILD_SHARED_LIBS})
set(BUILD_SHARED_LIBS OFF)

# llama.cpp - will build libllama.a, libggml.a, libggml-base.a, libggml-cpu.a
add_subdirectory(third-party/llama.cpp)

# whisper.cpp - will build libwhisper.a (needs GGML_STANDALONE=OFF to skip its own ggml)
set(GGML_STANDALONE OFF)
add_subdirectory(third-party/whisper.cpp)

# Restore BUILD_SHARED_LIBS
set(BUILD_SHARED_LIBS ${BUILD_SHARED_LIBS_BACKUP})

# ============================================================
# SINGLE shared library: llama + whisper + JNI bridges
# All compiled into one .so with statically linked dependencies
# ============================================================
add_library(aichat-core SHARED
    llama-android-jni.cpp
    whisper-android-jni.cpp
)

target_include_directories(aichat-core PRIVATE
    ${CMAKE_SOURCE_DIR}/third-party/llama.cpp/include
    ${CMAKE_SOURCE_DIR}/third-party/llama.cpp/common
    ${CMAKE_SOURCE_DIR}/third-party/llama.cpp/ggml/include
    ${CMAKE_SOURCE_DIR}/third-party/whisper.cpp/include
    ${CMAKE_SOURCE_DIR}/third-party/whisper.cpp
)

target_compile_options(aichat-core PRIVATE -Wall -Wextra)

# Link everything statically
# 注意：llama.cpp@b3975 的 ggml 仅产出单一 target `ggml`（CPU 后端已编入），
# 不存在 ggml-base / ggml-cpu 独立 target。
target_link_libraries(aichat-core
    llama           # static - from llama.cpp
    whisper         # static - from whisper.cpp
    ggml            # static - from llama.cpp/ggml (含 CPU 后端)
    android
    log
    z               # zlib (needed by ggml)
)

# llama.cpp@b3975 与 whisper.cpp@v1.6.1 各自内嵌了一份 ggml 实现，
# 链接时会产生重复符号。两份 ggml 实现本质相同，允许链接器取其一。
target_link_options(aichat-core PRIVATE -Wl,--allow-multiple-definition)
