# AI 对话（Android 端侧离线）

一个完全在手机端本地运行的离线 AI 对话 App：推理（Phi-3-mini）、语音识别（whisper.cpp）、
知识库检索（BM25 倒排索引）全部在设备端完成，**默认不联网**，保护隐私、零流量。

- 平台：Android（minSdk 24 / targetSdk 34）
- 语言：Kotlin + Jetpack Compose (Material 3) + C++ (JNI)
- 构建：Gradle + CMake（NDK）
- 端侧 LLM：`llama.cpp` 加载 `Phi-3-mini-4K-Instruct-Q4_K_M.gguf`
- 端侧 ASR：`whisper.cpp` 加载 `ggml-base.bin`
- 持久化：Room（KSP）
- 知识库：assets/knowledge/*.md，内存倒排索引 + BM25（无 embedding 模型）

## 1. 获取第三方推理引擎

本项目通过 git submodule 引入 `llama.cpp` 与 `whisper.cpp`：

```bash
git submodule update --init --recursive
```

若已克隆仓库但未拉取 submodule，请执行上面的命令。

## 2. 下载模型文件

> 模型权重**不入库**（体积过大）。请手动下载后放到
> `app/src/main/assets/models/` 目录，文件名必须与代码中的 `ModelPaths` 保持一致：

| 文件 | 大小 | 下载 |
| --- | --- | --- |
| `Phi-3-mini-4K-Instruct-Q4_K_M.gguf` | ~1.2GB | [HuggingFace 页面](https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf) → 选 Q4_K_M 的 .gguf |
| `ggml-base.bin` | ~150MB | [直接下载](https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin) |

下载完成后目录结构示例：

```
app/src/main/assets/models/
├── Phi-3-mini-4K-Instruct-Q4_K_M.gguf
├── ggml-base.bin
└── README.md
```

> 首启时 `AssetExtractor` 会把模型从 assets 拷贝到 `filesDir/models/`（带进度），
> DataStore 记录模型版本号，仅当版本变化或文件缺失时才重新拷贝。
> 若你希望直接把模型放到 `filesDir/models/`（`/data/data/com.aichat.app/files/models/`），
> 应用也会优先使用该路径，跳过拷贝步骤。

## 3. 更新知识库

把任意 `.md` 文档放入 `app/src/main/assets/knowledge/` 即可，例如：

```
app/src/main/assets/knowledge/
├── doc1_多节点对讲系统.md
├── doc2_手机端本地部署.md
├── doc3_AI语音对话系统.md
└── doc4_Phi-3模型介绍.md
```

`RagEngineImpl` 会在启动时读取该目录下所有 `*.md`，构建内存倒排索引（中文按字符 2-gram
+ 空白分词），检索时使用 BM25 打分。`RAG-lite` 不依赖任何 embedding 模型，纯端侧、零依赖。

## 4. 本地编译与安装

前置条件：

- Android Studio（Hedgehog 或更高）
- Android SDK Platform 34
- NDK `26.1.10909125`
- JDK 17

步骤：

1. 复制 `local.properties.template` 为 `local.properties`，填入本机 `sdk.dir`。
2. 打开 Android Studio → File → Open → 选择项目根目录。
3. 等待 Gradle Sync 完成（会自动拉取 NDK 与 submodule 头文件）。
4. 连接 Android 设备（或启动模拟器，**注意：模拟器无法使用 GPU/NNAPI，推理较慢**）。
5. 点击 ▶ Run，或命令行执行：

```bash
./gradlew installDebug
```

## 5. 使用 GitHub Actions 出包

仓库已配置 `.github/workflows/build.yml`：

- 推送 / 发起 PR 到 `main` 分支会自动构建 **Debug APK** 并作为 Action Artifact 上传。
- 推送形如 `v1.0.0` 的 **tag** 会触发 **Release 构建**，使用仓库 Secrets
  （`KEYSTORE_BASE64` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`）签名。
  请提前在仓库 Settings → Secrets 中配置你的签名信息。

## 6. 目录结构（本批 T01~T03）

```
app/src/main/
├── cpp/
│   ├── CMakeLists.txt                 # 接入 llama.cpp / whisper.cpp submodule
│   ├── llama-android-jni.cpp          # LLM JNI 桥
│   └── whisper-android-jni.cpp        # ASR JNI 桥
├── kotlin/com/aichat/app/
│   ├── jni/llama/*                     # LLM JNI 封装（Llama/Model/Chat/Sampler/Token）
│   ├── jni/whisper/*                   # ASR JNI 封装
│   ├── engine/model/*                  # 资源拷贝 / 模型管理
│   ├── engine/llm/*                    # LLM 引擎
│   ├── engine/speech/*                 # 语音引擎 + 录音
│   ├── engine/rag/*                    # RAG 倒排索引 + BM25
│   ├── domain/*                        # 领域模型 + Result
│   ├── config/*                        # 常量 / 路径 / 默认参数
│   ├── data/room/*                     # Room 持久化
│   ├── data/repository/*               # 仓库编排
│   ├── di/AppModule.*                  # 手动 DI 容器 + ViewModel 工厂（T05）
│   ├── ui/theme/*                      # Compose 主题（T04）
│   ├── ui/viewmodel/*                  # ChatViewModel / SettingsViewModel / UiState（T04）
│   ├── ui/component/*                  # TopBar / MessageBubble / MessageList / InputBar 等（T04）
│   ├── ui/ChatScreen.kt                # 聊天主界面（T04）
│   ├── ui/SettingsScreen.kt            # 设置界面（T04）
│   ├── ui/AppNavHost.kt                # 导航图（T04）
│   ├── AiChatApp.kt                    # Application：依赖装配 + 模型预热（T05）
│   └── MainActivity.kt                 # 入口 Activity：挂载 Compose + 权限申请（T05）
└── assets/
    ├── models/README.md                # 模型下载说明
    └── knowledge/*.md                  # RAG 示例语料
```

## 7. 后续批次

- T04：UI 层（Compose 聊天界面、语音按钮、设置页）—— 已落盘。
- T05：`AiChatApp` / `MainActivity` 依赖装配、ViewModel、离线状态机 —— 已落盘。

---

## 8. 快速开始 / 编译 / 安装

1. **拉取子模块**（第三方推理引擎 `llama.cpp` / `whisper.cpp`）：

   ```bash
   git submodule update --init --recursive
   ```

2. **用 Android Studio 打开并运行**：
   - 打开 `android-app` 目录 → 等待 Gradle Sync 完成；
   - 连接设备 / 模拟器，点击 ▶ Run，或命令行：

     ```bash
     ./gradlew installDebug
     ```

   - 也可通过 `Build → Build APK(s)` 生成安装包。

3. **放入模型文件**：将以下两个文件放到 `app/src/main/assets/models/`（文件名须与
   `config/ModelPaths.kt` 完全一致）：

   - `Phi-3-mini-4K-Instruct-Q4_K_M.gguf`
   - `ggml-base.bin`

   > 首启时 `AssetExtractor` 会把模型从 assets 拷贝到 `filesDir/models/`；
   > 若已手动放到 `filesDir/models/`，应用会优先使用、跳过拷贝。

4. **首次启动自动拷贝加载**：首次进入聊天页会触发 `ChatViewModel.loadModel()`，
   依次完成模型拷贝、LLM / ASR 加载与 RAG 索引构建（界面显示全屏进度覆盖层）。

5. **GitHub Actions 自动出包**：推送 / PR 到 `main` 自动构建 Debug APK 并作为
   Artifact 上传；打 `v*` tag 触发 Release 签名构建。

6. **硬件建议**：推理主要在 CPU 上进行，建议设备 **≥ 4GB RAM** 以保证 Phi-3-mini 流畅运行。
