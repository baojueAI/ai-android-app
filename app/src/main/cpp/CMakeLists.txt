cmake_minimum_required(VERSION 3.22.1)

# 统一使用 LLVM libc++ 共享运行时，避免与 submodule 中 ggml 的 STL 不一致
set(ANDROID_STL c++_shared)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

project("aichat-native" CXX)

# 引入第三方推理引擎（git submodule）
#
# 注意：llama.cpp 与 whisper.cpp 各自携带一份 ggml，同时 add_subdirectory 会导致
# CMake 报 "add_library cannot create imported target 'ggml'" 目标重名错误。
#
# 修复方案：在添加 whisper.cpp 之前设置 GGML_STANDALONE=OFF，让其跳过自身 ggml 构建，
# 直接复用 llama.cpp 已提供的 ggml 库。二者 API 兼容，链接无冲突。
add_subdirectory(third-party/llama.cpp)
set(GGML_STANDALONE OFF)
add_subdirectory(third-party/whisper.cpp)

# ============================================================
# LLM 桥（llama-android）：调用 llama.cpp 完成加载 / 流式生成
# ============================================================
add_library(llama-android SHARED llama-android-jni.cpp)

target_include_directories(llama-android PRIVATE
        ${CMAKE_SOURCE_DIR}/third-party/llama.cpp/include
        ${CMAKE_SOURCE_DIR}/third-party/llama.cpp/common)

target_compile_options(llama-android PRIVATE -Wall -Wextra)

target_link_libraries(llama-android
        llama
        ggml
        android
        log)

# ============================================================
# ASR 桥（whisper）：调用 whisper.cpp 完成语音转写
# ============================================================
add_library(whisper SHARED whisper-android-jni.cpp)

target_include_directories(whisper PRIVATE
        ${CMAKE_SOURCE_DIR}/third-party/whisper.cpp/include
        ${CMAKE_SOURCE_DIR}/third-party/whisper.cpp)

target_compile_options(whisper PRIVATE -Wall -Wextra)

target_link_libraries(whisper
        whisper
        ggml
        android
        log)
