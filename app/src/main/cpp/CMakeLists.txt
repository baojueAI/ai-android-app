cmake_minimum_required(VERSION 3.22.1)

# 统一使用 LLVM libc++ 共享运行时，避免与 submodule 中 ggml 的 STL 不一致
set(ANDROID_STL c++_shared)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

project("aichat-native" CXX)

# 引入第三方推理引擎（git submodule）
add_subdirectory(third-party/llama.cpp)
set(GGML_STANDALONE OFF)
add_subdirectory(third-party/whisper.cpp)

# ============================================================
# LLM 桥（llama-android）：调用 llama.cpp 完成加载 / 流式生成
# ============================================================
add_library(llama-android SHARED llama-android-jni.cpp)

target_include_directories(llama-android PRIVATE
        /third-party/llama.cpp/include
        /third-party/llama.cpp/common)

target_compile_options(llama-android PRIVATE -Wall -Wextra)

target_link_libraries(llama-android
        llama
        ggml
        android
        log)

# ============================================================
# ASR 桥（whisper-android-bridge）：调用 whisper.cpp 完成语音转写
# 注意：目标名不能与 whisper.cpp 内部定义的 whisper 库重名
# ============================================================
add_library(whisper-android-bridge SHARED whisper-android-jni.cpp)

target_include_directories(whisper-android-bridge PRIVATE
        /third-party/whisper.cpp/include
        /third-party/whisper.cpp)

target_compile_options(whisper-android-bridge PRIVATE -Wall -Wextra)

target_link_libraries(whisper-android-bridge
        whisper
        ggml
        android
        log)
