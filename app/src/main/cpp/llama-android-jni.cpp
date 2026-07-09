// ============================================================================
// llama-android-jni.cpp
// llama.cpp 的 Android JNI 桥接层。
// 对应 Kotlin 侧：com.aichat.app.jni.llama.Llama 的 external 方法。
// JNI 函数名必须与 Kotlin external fun 名称逐字一致。
// ============================================================================

#include <jni.h>
#include <string>
#include <vector>
#include <atomic>
#include <cmath>
#include <random>
#include <algorithm>

#include "llama.h"

// 句柄：保存已加载的模型与上下文，以及停止标志、采样参数
struct LlamaHandle {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    std::atomic<bool> stop{false};
    float temperature = 0.7f;
    float top_p = 0.95f;
};

// JNI jstring -> std::string
static std::string jstringToStdString(JNIEnv* env, jstring js) {
    if (!js) return {};
    const char* cstr = env->GetStringUTFChars(js, nullptr);
    std::string out(cstr ? cstr : "");
    if (cstr) env->ReleaseStringUTFChars(js, cstr);
    return out;
}

// 简单且版本鲁棒的采样：温度 -> softmax -> top-p 截断 -> 候选集内按概率采样
// 不依赖 llama.cpp 的 sampler 链，避免不同版本 API 差异导致编译失败。
static llama_token sample_token(struct llama_context* ctx, float temperature, float top_p,
                                 std::mt19937& rng) {
    const llama_vocab* vocab = llama_model_get_vocab(llama_get_model(ctx));
    const int n_vocab = llama_vocab_n_tokens(vocab);
    const float* logits = llama_get_logits(ctx);

    // 应用温度
    std::vector<float> scores(n_vocab);
    const float t = std::max(temperature, 1e-4f);
    for (int i = 0; i < n_vocab; ++i) {
        scores[i] = logits[i] / t;
    }

    // softmax
    float maxScore = *std::max_element(scores.begin(), scores.end());
    float sum = 0.0f;
    for (int i = 0; i < n_vocab; ++i) {
        scores[i] = std::exp(scores[i] - maxScore);
        sum += scores[i];
    }
    for (int i = 0; i < n_vocab; ++i) scores[i] /= sum;

    // top-p 截断：按概率降序累加
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
        if (cum >= top_p) break;
    }

    // 在候选集内按概率采样
    std::discrete_distribution<int> dist(candidateProbs.begin(), candidateProbs.end());
    return static_cast<llama_token>(candidateTokens[dist(rng)]);
}

// ----------------------------------------------------------------------------
// 创建模型 + 上下文
// ----------------------------------------------------------------------------
extern "C" JNIEXPORT jlong JNICALL
Java_com_aichat_app_jni_llama_Llama_createModel(
        JNIEnv* env, jclass /*clazz*/, jstring modelPath, jint nCtx,
        jint nThreads, jfloat temperature, jfloat topP) {
    (void)env;
    llama_backend_init();

    auto* handle = new (std::nothrow) LlamaHandle();
    if (!handle) return 0L;

    handle->temperature = temperature > 0.0f ? temperature : 0.7f;
    handle->top_p = top_p > 0.0f ? top_p : 0.95f;

    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = 0; // 纯 CPU 推理（端侧设备）

    const std::string path = jstringToStdString(env, modelPath);
    handle->model = llama_load_model_from_file(path.c_str(), mparams);

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = nCtx;
    cparams.n_threads = nThreads;
    cparams.n_threads_batch = nThreads;

    handle->ctx = llama_new_context_with_model(handle->model, cparams);

    if (!handle->model || !handle->ctx) {
        if (handle->ctx) llama_free(handle->ctx);
        if (handle->model) llama_model_free(handle->model);
        delete handle;
        return 0L;
    }
    return reinterpret_cast<jlong>(handle);
}

// ----------------------------------------------------------------------------
// 流式生成：将每个 token 的文本片段通过回调回传到 Kotlin
// ----------------------------------------------------------------------------
extern "C" JNIEXPORT void JNICALL
Java_com_aichat_app_jni_llama_Llama_generate(
        JNIEnv* env, jclass /*clazz*/, jlong modelPtr, jstring prompt,
        jobject callback, jfloat temperature, jfloat topP, jint maxTokens) {
    auto* handle = reinterpret_cast<LlamaHandle*>(modelPtr);
    if (!handle || !handle->ctx || !callback) return;

    handle->stop.store(false);

    // 解析 Kotlin 回调（TokenCallback.onToken(String)）
    jclass cbClass = env->GetObjectClass(callback);
    jmethodID onTokenMid = env->GetMethodID(cbClass, "onToken", "(Ljava/lang/String;)V");
    if (!onTokenMid) return;

    const std::string promptStr = jstringToStdString(env, prompt);
    const llama_vocab* vocab = llama_model_get_vocab(handle->model);

    // 构造输入 token（含 BOS）
    std::vector<llama_token> tokens;
    tokens.push_back(llama_vocab_bos(vocab));

    const int cap = 8192;
    std::vector<llama_token> tmp(cap);
    const int nt = llama_tokenize(vocab, promptStr.c_str(),
                                  static_cast<int>(promptStr.size()), tmp.data(), cap, true, false);
    if (nt > 0) {
        tokens.insert(tokens.end(), tmp.begin(), tmp.begin() + nt);
    }

    std::mt19937 rng(42);
    
    llama_token newToken = 0;

    llama_batch batch = llama_batch_get_one(tokens.data(),
                                            static_cast<int32_t>(tokens.size()), 0, 0);

    for (int i = 0; i < maxTokens; ++i) {
        if (handle->stop.load()) break;

        if (llama_decode(handle->ctx, batch) != 0) break;

        newToken = sample_token(handle->ctx, temperature, topP, rng);

        // 命中结束符（<|end|>/</s> 等）则停止
        if (llama_vocab_is_eog(vocab, newToken)) break;

        // token -> UTF-8 文本片段
        std::vector<char> buf(256);
        int len = llama_token_to_piece(vocab, newToken, buf.data(),
                                       static_cast<int>(buf.size()), 0, true);
        if (len < 0) {
            buf.resize(static_cast<size_t>(-len) + 1);
            len = llama_token_to_piece(vocab, newToken, buf.data(),
                                       static_cast<int>(buf.size()), 0, true);
        }
        if (len > 0) {
            const std::string piece(buf.data(), static_cast<size_t>(len));
            jstring jpiece = env->NewStringUTF(piece.c_str());
            env->CallVoidMethod(callback, onTokenMid, jpiece);
            if (jpiece) env->DeleteLocalRef(jpiece);
        }

        // 准备下一轮 batch
        batch = llama_batch_get_one(&newToken, 1, i + 1, 0);
    }

    llama_kv_cache_clear(handle->ctx);
}

// ----------------------------------------------------------------------------
// 停止生成
// ----------------------------------------------------------------------------
extern "C" JNIEXPORT void JNICALL
Java_com_aichat_app_jni_llama_Llama_stop(JNIEnv* /*env*/, jclass /*clazz*/, jlong modelPtr) {
    auto* handle = reinterpret_cast<LlamaHandle*>(modelPtr);
    if (handle) handle->stop.store(true);
}

// ----------------------------------------------------------------------------
// 释放模型与上下文
// ----------------------------------------------------------------------------
extern "C" JNIEXPORT void JNICALL
Java_com_aichat_app_jni_llama_Llama_freeModel(JNIEnv* /*env*/, jclass /*clazz*/, jlong modelPtr) {
    auto* handle = reinterpret_cast<LlamaHandle*>(modelPtr);
    if (!handle) return;
    if (handle->ctx) llama_free(handle->ctx);
    if (handle->model) llama_model_free(handle->model);
    delete handle;
}
