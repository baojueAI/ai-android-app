// ============================================================================
// llama-android-jni.cpp
// llama.cpp 的 Android JNI 桥接层（适配 llama.cpp@b3975 API）。
// 对应 Kotlin 侧：com.aichat.app.jni.llama.Llama 的 external 方法。
// ============================================================================

#include <jni.h>
#include <string>
#include <vector>
#include <atomic>
#include <cmath>
#include <random>
#include <algorithm>

#include "llama.h"

// 句柄：保存已加载的模型与上下文
struct LlamaHandle {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    std::atomic<bool> stop{false};
};

// JNI jstring -> std::string
static std::string jstringToStdString(JNIEnv* env, jstring js) {
    if (!js) return {};
    const char* cstr = env->GetStringUTFChars(js, nullptr);
    std::string out(cstr ? cstr : "");
    if (cstr) env->ReleaseStringUTFChars(js, cstr);
    return out;
}

// 采样：温度 -> softmax -> top-p 截断 -> 按概率采样
// b3975 API：没有 llama_vocab 概念，vocab 相关函数直接挂在 llama_model* 上。
static llama_token sample_token(struct llama_context* ctx, float temperature, float p_top,
                                 std::mt19937& rng) {
    const llama_model* model = llama_get_model(ctx);
    const int n_vocab = llama_n_vocab(model);
    const float* logits = llama_get_logits(ctx);

    std::vector<float> scores(n_vocab);
    const float t = std::max(temperature, 1e-4f);
    for (int i = 0; i < n_vocab; ++i) scores[i] = logits[i] / t;

    // softmax
    float maxScore = *std::max_element(scores.begin(), scores.end());
    float sum = 0.0f;
    for (int i = 0; i < n_vocab; ++i) { scores[i] = std::exp(scores[i] - maxScore); sum += scores[i]; }
    for (int i = 0; i < n_vocab; ++i) scores[i] /= sum;

    // top-p 截断
    std::vector<int> idx(n_vocab);
    for (int i = 0; i < n_vocab; ++i) idx[i] = i;
    std::sort(idx.begin(), idx.end(), [&](int a, int b) { return scores[a] > scores[b]; });

    float cum = 0.0f;
    std::vector<int> candidateTokens;
    std::vector<float> candidateProbs;
    for (int i = 0; i < n_vocab; ++i) {
        cum += scores[idx[i]];
        candidateTokens.push_back(idx[i]);
        candidateProbs.push_back(scores[idx[i]]);
        if (cum >= p_top) break;
    }

    std::discrete_distribution<int> dist(candidateProbs.begin(), candidateProbs.end());
    return static_cast<llama_token>(candidateTokens[dist(rng)]);
}

// ----------------------------------------------------------------------------
// 创建模型 + 上下文
// ----------------------------------------------------------------------------
extern "C" JNIEXPORT jlong JNICALL
Java_com_aichat_app_jni_llama_Llama_createModel(
        JNIEnv* env, jobject /*thiz*/, jstring modelPath, jint nCtx,
        jint nThreads, jfloat temperature, jfloat topP) {
    llama_backend_init();

    auto* handle = new (std::nothrow) LlamaHandle();
    if (!handle) return 0L;

    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = 0;

    const std::string path = jstringToStdString(env, modelPath);
    // b3975: llama_load_model_from_file（新版叫 llama_model_load_from_file）
    handle->model = llama_load_model_from_file(path.c_str(), mparams);
    if (!handle->model) { delete handle; return 0L; }

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = nCtx;
    cparams.n_threads = nThreads;
    cparams.n_threads_batch = nThreads;

    // b3975: llama_new_context_with_model（新版叫 llama_init_from_model）
    handle->ctx = llama_new_context_with_model(handle->model, cparams);
    if (!handle->ctx) {
        // b3975: llama_free_model（新版叫 llama_model_free）
        llama_free_model(handle->model);
        delete handle;
        return 0L;
    }
    return reinterpret_cast<jlong>(handle);
}

// ----------------------------------------------------------------------------
// 流式生成
// ----------------------------------------------------------------------------
extern "C" JNIEXPORT void JNICALL
Java_com_aichat_app_jni_llama_Llama_generate(
        JNIEnv* env, jobject /*thiz*/, jlong modelPtr, jstring prompt,
        jobject callback, jfloat temperature, jfloat topP, jint maxTokens) {
    auto* handle = reinterpret_cast<LlamaHandle*>(modelPtr);
    if (!handle || !handle->ctx || !callback) return;

    handle->stop.store(false);

    jclass cbClass = env->GetObjectClass(callback);
    jmethodID onTokenMid = env->GetMethodID(cbClass, "onToken", "(Ljava/lang/String;)V");
    if (!onTokenMid) return;

    const std::string promptStr = jstringToStdString(env, prompt);
    const llama_model* model = handle->model;
    if (!model) return;

    // tokenize prompt（b3975: llama_tokenize 直接接收 model，无 llama_vocab）
    const int nTokens = -llama_tokenize(model, promptStr.c_str(), promptStr.size(), nullptr, 0, true, true);
    std::vector<llama_token> tokens(nTokens);
    if (nTokens > 0) {
        llama_tokenize(model, promptStr.c_str(), promptStr.size(), tokens.data(), nTokens, true, true);
    }

    std::mt19937 rng(42);
    llama_token newToken = 0;

    auto batch = llama_batch_get_one(tokens.data(), tokens.size());

    for (int i = 0; i < maxTokens; ++i) {
        if (handle->stop.load()) break;

        if (llama_decode(handle->ctx, batch) != 0) break;

        newToken = sample_token(handle->ctx, temperature, topP, rng);

        // b3975: llama_token_is_eog(model, token)（新版叫 llama_vocab_is_eog）
        if (llama_token_is_eog(model, newToken)) break;

        std::vector<char> buf(256);
        // b3975: llama_token_to_piece(model, token, buf, length, lstrip, special)
        int len = llama_token_to_piece(model, newToken, buf.data(), buf.size(), 0, true);
        if (len < 0) {
            buf.resize(static_cast<size_t>(-len) + 1);
            len = llama_token_to_piece(model, newToken, buf.data(), buf.size(), 0, true);
        }
        if (len > 0) {
            const std::string piece(buf.data(), static_cast<size_t>(len));
            jstring jpiece = env->NewStringUTF(piece.c_str());
            env->CallVoidMethod(callback, onTokenMid, jpiece);
            if (jpiece) env->DeleteLocalRef(jpiece);
        }

        batch = llama_batch_get_one(&newToken, 1);
    }
}

// ----------------------------------------------------------------------------
// 停止生成
// ----------------------------------------------------------------------------
extern "C" JNIEXPORT void JNICALL
Java_com_aichat_app_jni_llama_Llama_stop(JNIEnv* /*env*/, jobject /*thiz*/, jlong modelPtr) {
    auto* handle = reinterpret_cast<LlamaHandle*>(modelPtr);
    if (handle) handle->stop.store(true);
}

// ----------------------------------------------------------------------------
// 释放模型与上下文
// ----------------------------------------------------------------------------
extern "C" JNIEXPORT void JNICALL
Java_com_aichat_app_jni_llama_Llama_freeModel(JNIEnv* /*env*/, jobject /*thiz*/, jlong modelPtr) {
    auto* handle = reinterpret_cast<LlamaHandle*>(modelPtr);
    if (!handle) return;
    if (handle->ctx) llama_free(handle->ctx);
    if (handle->model) llama_free_model(handle->model); // b3975 API
    delete handle;
}
