// ============================================================================
// whisper-android-jni.cpp
// whisper.cpp 的 Android JNI 桥接层。
// 对应 Kotlin 侧：com.aichat.app.jni.whisper.Whisper 的 external 方法。
// JNI 函数名必须与 Kotlin external fun 名称逐字一致。
// ============================================================================

#include <jni.h>
#include <string>
#include <vector>
#include <thread>

#include "whisper.h"

// 句柄：保存已加载的 whisper 上下文
struct WhisperHandle {
    whisper_context* ctx = nullptr;
};

// JNI jstring -> std::string
static std::string jstringToStdString(JNIEnv* env, jstring js) {
    if (!js) return {};
    const char* cstr = env->GetStringUTFChars(js, nullptr);
    std::string out(cstr ? cstr : "");
    if (cstr) env->ReleaseStringUTFChars(js, cstr);
    return out;
}

// 分段回调的用户数据（whisper_full 为同步调用，回调与原生线程一致，env 有效）
struct SegmentCbData {
    JNIEnv* env = nullptr;
    jobject callback = nullptr;
    jmethodID onSegmentMid = nullptr;
    jclass segmentClass = nullptr;
    jmethodID segmentCtor = nullptr;
};

// whisper.cpp 的新分段回调：每识别出若干新分段即触发
static void new_segment_callback(struct whisper_context* ctx, struct whisper_state* /*state*/,
                                 int32_t n_new, void* user_data) {
    auto* data = static_cast<SegmentCbData*>(user_data);
    if (!data || !data->env || !data->callback) return;

    const int n_segments = whisper_full_n_segments(ctx);
    const int start = n_segments - n_new;
    if (start < 0) return;

    for (int i = start; i < n_segments; ++i) {
        const char* text = whisper_full_get_segment_text(ctx, i);
        const float t0 = whisper_full_get_segment_t0(ctx, i) / 100.0f; // centi-sec -> sec
        const float t1 = whisper_full_get_segment_t1(ctx, i) / 100.0f;

        jstring jtext = data->env->NewStringUTF(text ? text : "");
        jobject seg = data->env->NewObject(data->segmentClass, data->segmentCtor,
                                           static_cast<jint>(i), t0, t1, jtext);
        data->env->CallVoidMethod(data->callback, data->onSegmentMid, seg);
        if (seg) data->env->DeleteLocalRef(seg);
        if (jtext) data->env->DeleteLocalRef(jtext);
    }
}

// ----------------------------------------------------------------------------
// 加载模型
// ----------------------------------------------------------------------------
extern "C" JNIEXPORT jlong JNICALL
Java_com_aichat_app_jni_whisper_Whisper_create(JNIEnv* env, jclass /*clazz*/, jstring path) {
    auto* handle = new (std::nothrow) WhisperHandle();
    if (!handle) return 0L;

    const std::string modelPath = jstringToStdString(env, path);
    handle->ctx = whisper_init_from_file(modelPath.c_str());
    if (!handle->ctx) {
        delete handle;
        return 0L;
    }
    return reinterpret_cast<jlong>(handle);
}

// ----------------------------------------------------------------------------
// 转写：喂入 16k 单声道 PCM Float，分段回调文本
// ----------------------------------------------------------------------------
extern "C" JNIEXPORT void JNICALL
Java_com_aichat_app_jni_whisper_Whisper_transcribeNative(
        JNIEnv* env, jclass /*clazz*/, jlong ptr, jfloatArray samples, jobject callback) {
    auto* handle = reinterpret_cast<WhisperHandle*>(ptr);
    if (!handle || !handle->ctx) return;

    jfloat* pcm = env->GetFloatArrayElements(samples, nullptr);
    const jsize n = env->GetArrayLength(samples);

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.n_threads = static_cast<int>(std::min<size_t>(4, std::thread::hardware_concurrency()));
    params.print_progress = false;
    params.print_realtime = false;
    params.print_timestamps = false;
    params.translate = false;

    // 准备 Java 回调结构
    SegmentCbData data{};
    data.env = env;
    data.callback = callback;

    jclass cbClass = env->GetObjectClass(callback);
    data.onSegmentMid = env->GetMethodID(cbClass, "onSegment",
                                         "(Lcom/aichat/app/jni/whisper/WhisperSegment;)V");
    data.segmentClass = env->FindClass("com/aichat/app/jni/whisper/WhisperSegment");
    data.segmentCtor = env->GetMethodID(data.segmentClass, "<init>", "(IFFLjava/lang/String;)V");

    params.new_segment_callback = new_segment_callback;
    params.new_segment_callback_user_data = &data;

    whisper_full(handle->ctx, params, pcm, static_cast<int>(n));

    env->ReleaseFloatArrayElements(samples, pcm, JNI_ABORT);
}

// ----------------------------------------------------------------------------
// 释放模型
// ----------------------------------------------------------------------------
extern "C" JNIEXPORT void JNICALL
Java_com_aichat_app_jni_whisper_Whisper_free(JNIEnv* /*env*/, jclass /*clazz*/, jlong ptr) {
    auto* handle = reinterpret_cast<WhisperHandle*>(ptr);
    if (!handle) return;
    if (handle->ctx) whisper_free(handle->ctx);
    delete handle;
}

// ============================================================================
// JNI_OnLoad：显式注册 native 方法，确保在所有 Android ROM 上兼容
// ============================================================================
static const JNINativeMethod gMethods[] = {
    { "create",
      "(Ljava/lang/String;)J",
      reinterpret_cast<void*>(Java_com_aichat_app_jni_whisper_Whisper_create) },
    { "transcribeNative",
      "(J[FLcom/aichat/app/jni/whisper/Whisper$SegmentCallback;)V",
      reinterpret_cast<void*>(Java_com_aichat_app_jni_whisper_Whisper_transcribeNative) },
    { "free",
      "(J)V",
      reinterpret_cast<void*>(Java_com_aichat_app_jni_whisper_Whisper_free) },
};

extern "C" JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* /*reserved*/) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    jclass clazz = env->FindClass("com/aichat/app/jni/whisper/Whisper");
    if (!clazz) {
        return JNI_ERR;
    }
    if (env->RegisterNatives(clazz, gMethods, sizeof(gMethods) / sizeof(gMethods[0])) != JNI_OK) {
        return JNI_ERR;
    }
    return JNI_VERSION_1_6;
}
