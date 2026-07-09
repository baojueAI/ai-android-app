# 安卓端侧离线 AI 对话 App — 质量审查报告（QA）

- **审查人**：严过关（QA Engineer）
- **项目**：`com.aichat.app` 安卓端侧离线 AI 对话 App（T01–T05 全量落盘）
- **审查方式**：静态一致性审查 + 纯算法正确性论证（**本环境无 Android SDK / NDK / kotlinc，未执行任何编译或运行**，结论为静态推理）
- **审查范围**：JNI 签名、Room、Manifest/res、依赖/import、DI 装配、ViewModel/UI 衔接、CMake、BM25 算法、提示词拼装、sendMessage 编排

---

## 一、执行摘要

对 83 个文件逐模块开展静态审查，**未发现任何致命源码 bug（无编译必失败、无运行时必崩的源码级缺陷）**。JNI 桥接函数名/参数、Room 实体与 Dao、Manifest/res 引用、Gradle 依赖与代码 import、DI 构造装配、ViewModel 与 UI 方法/状态字段衔接、CMake 源文件引用，**全部逐项一致（PASS）**。BM25 检索算法与提示词拼装、sendMessage 编排经论证**逻辑正确、边界安全**。

遗留项主要为**构建期/环境性风险**与**非阻塞功能性缺口**（详见第五节），均不阻断静态审查结论。

**路由结论：`ROUTE: NoOne`**（静态审查通过，仅遗留环境性与非阻塞项）。

---

## 二、静态一致性审查清单

> 证据格式：`文件:行号`

| # | 审查项 | 结果 | 关键证据 |
|---|--------|------|----------|
| 1 | JNI llama 函数签名一致 | **PASS** | cpp `Java_com_aichat_app_jni_llama_Llama_createModel/generate/stop/freeModel`（llama-android-jni.cpp:83,121,192,201）逐字对应 `Llama.kt:34,46,50,54`；`onToken(Ljava/lang/String;)V`（cpp:131）↔ `TokenCallback.onToken`（Llama.kt:19） |
| 2 | JNI whisper 函数签名一致 | **PASS** | cpp `Java_..._Whisper_create/transcribeNative/free`（whisper-android-jni.cpp:65,82,120）↔ `Whisper.kt:61,65,69`；`onSegment(LWhisperSegment;)V`（cpp:104）↔ `SegmentCallback.onSegment`（Whisper.kt:19） |
| 3 | WhisperSegment 构造签名一致 | **PASS** | cpp `NewObject(...,"<init>","(IFFLjava/lang/String;)V")`（whisper-android-jni.cpp:107）↔ `data class WhisperSegment(index:Int,startTime:Float,endTime:Float,text:String)`（WhisperSegment.kt:15），无默认值→仅单一主构造，参数序与 JVM 签名一致 |
| 4 | Room @Database 实体一致 | **PASS** | `ChatDatabase.kt:14` `entities=[SessionEntity,MessageEntity], version=1`，与两个 `@Entity` 类一致；`sessionDao()/messageDao()`（:21-22） |
| 5 | Room 实体字段 ↔ Dao SQL | **PASS** | MessageEntity 列 `session_id`(@ColumnInfo:19)/`created_at`(:22) 与 MessageDao SQL（:16,20,28）一致；SessionEntity 列 `updated_at`/`id` 与 SessionDao（:17,25）一致 |
| 6 | Room Dao 返回类型 ↔ 调用方 | **PASS** | `ChatRepositoryImpl.kt:79 upsert(message.toEntity())`、`:95 getMessages().map{toDomain()}` 类型匹配 |
| 7 | Manifest 无 INTERNET | **PASS** | AndroidManifest.xml 仅声明 RECORD_AUDIO（:6），注释明确不声明 INTERNET（:13） |
| 8 | Manifest/res 引用存在 | **PASS** | `@drawable/ic_launcher`(:18-19)、`@string/app_name`(:20)、`@style/Theme.AIChatApp`(:22)、`@xml/backup_rules`(:23)、`@xml/data_extraction_rules`(:24) 均存在；`ic_launcher.xml` 引用的 bg/fg 存在；`.AiChatApp`/`.MainActivity` 存在 |
| 9 | 依赖 compose-markdown 解析与使用 | **PASS** | `app/build.gradle.kts:134` `com.github.jeziellago:compose-markdown:0.5.0`，`settings.gradle.kts:21` 接入 jitpack；`MessageBubble.kt:24,64` 使用 `dev.jeziellago.compose.markdowntext.MarkdownText`（包名与构件一致） |
| 10 | material:1.12.0 支撑 XML 主题 | **PASS** | `app/build.gradle.kts:141`；`themes.xml:9` `Theme.Material3.DayNight.NoActionBar`；material3 导入于 Theme.kt / MessageBubble.kt |
| 11 | accompanist-systemuicontroller 用法 | **PASS** | `app/build.gradle.kts:137` `0.34.0`；`Theme.kt:11,36-38` `rememberSystemUiController().setStatusBarColor/setNavigationBarColor` 用法正确 |
| 12 | 其余依赖 ↔ import 匹配 | **PASS** | lifecycle / datastore / navigation / room-ktx / coroutines / core-ktx / activity-compose / material-icons-extended / compose-ui 均被对应 import 使用 |
| 13 | DI loadLibrary 顺序 | **PASS** | `AppModule.kt:52-54` llama→llama-android→whisper，与 `Llama.kt:25-27`、`Whisper.kt:52-54` 一致 |
| 14 | DI 各 Impl 构造参数一致 | **PASS** | `SettingsRepository(ctx)`↔:19；`AssetExtractor(ctx.assets)`↔AssetExtractor.kt:20；`ModelManager(ctx)`↔:20；`LlmEngineImpl()`↔:19；`SpeechEngineImpl()`↔:17；`RagEngineImpl(ctx)`↔RagEngineImpl.kt:18；`ChatRepositoryImpl(llmEngine,speechEngine,ragEngine,database)`↔ChatRepositoryImpl.kt:27 |
| 15 | DI 提供 get* 方法 | **PASS** | `getChatRepository/getSettingsRepository/getModelManager`（AppModule.kt:74-87）；ViewModelFactory 6 参 ↔ `ChatViewModel.kt:32-39`，1 参 ↔ `SettingsViewModel.kt:18` |
| 16 | ViewModel/UI 方法衔接 | **PASS** | ChatScreen.kt:52/67/68-73/103/107 调用 `loadModel/onInputChange/sendText/startVoiceInput/stopVoiceInput/stopGeneration/clearError`；SettingsScreen.kt:87/101/128/155 调用 `setTemperature/setMaxTokens/setDarkMode/setNetworkFallback`，全部存在 |
| 17 | UiState 字段衔接 | **PASS** | ChatUiState(messages/isGenerating/isRecording/inputText/error/modelReady/loadProgress) UiState.kt:14-26 均被引用；SettingsUiState(temperature/maxTokens/darkMode/networkFallback/modelName) UiState.kt:36-42 均被引用 |
| 18 | CMake 源文件/子模块引用 | **PASS** | `add_library(llama-android SHARED llama-android-jni.cpp)`/`add_library(whisper SHARED whisper-android-jni.cpp)`（CMakeLists.txt:21,38）与 cpp 文件一致；`add_subdirectory(third-party/llama.cpp|whisper.cpp)`（:15-16）与 `.gitmodules` 路径一致 |

**一致性审查结论：18/18 项 PASS。** 未发现 JNI/签名/引用类致命不一致。

---

## 三、纯逻辑 / 算法正确性论证

### 3.1 BM25 检索算法（InvertedIndex.kt）

**算法实现（search，InvertedIndex.kt:63-92）**

标准 BM25 打分公式：
```
IDF = ln( (N - df + 0.5) / (df + 0.5) + 1 )          // :75
score = IDF * ( tf * (k1 + 1) / ( tf + k1 * (1 - b + b * |D| / avgdl) ) )  // :80-81
```
其中 `N = docs.size`、`df = postings.size`（含该词的文档数）、`tf` 词频、`|D| = doc.tokens.size`、`avgdl = avgLen`（:36-37）、`k1=1.5, b=0.75`。该实现与教科书 BM25 完全一致，且 IDF 采用 `+1` 平滑（atire 形式）**避免 log(0) 与负 IDF**。

**分词（tokenize，:99-125）**
- ASCII/数字：按 `[\s\p{P}]+` 切分、过滤纯标点、小写化（与正文、查询同构，保证可匹配）。
- 中文：字符 2-gram（`"如何使用手机"` → `如何/何使/使用/用手/手机`）；单字整体作为一个 term。
- 索引构建（addDocument）与查询（search）使用**同一套分词**，因此查询 2-gram 必能与文档 2-gram 命中——检索自洽。

**边界安全性（已验证无崩溃）**
- 空查询 / 空索引：`qTerms.isEmpty() || docs.isEmpty()` → 直接 `emptyList()`（:65）。✅
- 无匹配 term：`index[term] ?: continue`（:72），若全词无命中则 `scores` 为空 → 返回 `emptyList()`。✅
- 分母为零：`avgLen` 在 `docs` 空时返回 `1f`（:36-37）；而若所有文档 token 数为 0，则无任何倒排记录，循环不执行、不触发除法；只要存在命中文档，该文档 `len>0` 且 `totalTokens>0` → `avg>0`，除法恒有限。✅

**数值例证（证明 IDF 可正确区分相关度）**
语料：`Doc0="如何使用手机"`、`Doc1="手机电池续航"`；查询 `"电池"` → 2-gram `["电池"]`。
- `电池` 仅出现在 Doc1 → `df=1`，`N=2` → `idf=ln((2-1+0.5)/(1+0.5)+1)=ln(2)=0.693`；Doc1 打分 `0.693*(1*2.5/2.5)=0.693`，Doc0 不在倒排中 → 0。
- 结果：Doc1 居首，检索正确。✅ 反之稀有词 IDF 更高，符合 BM25 预期。

**结论：BM25 算法逻辑正确、边界安全。**

### 3.2 提示词拼装（RagEngineImpl + ChatRepositoryImpl）

- `RagEngineImpl.load()` 读取 `assets/knowledge/*.md`，逐文件 `index.addDocument(text, file)`（RagEngineImpl.kt:25-33）；`retrieve` 要求已 `load()` 否则抛异常（:36），返回 `index.search(query, topK)`（:37）。✅
- 系统提示严格符合约定：`ChatRepositoryImpl.buildSystemPrompt`（:101-103）输出
  `你是一名完全离线运行的智能助手，仅依据【知识库】内容简洁专业回答；无相关信息如实说明。\n\n【知识库】\n{知识片段}`
  与 PRD/设计约定（"完全离线运行的智能助手…" + 【知识库】注入）**逐字一致**。✅
- `sendMessage`（:34-67）编排自洽：取末条用户消息作 query（:41）→ `ragEngine.retrieve(query, RAG_TOP_K=3)`（:43）→ 拼 system 提示并前置为 `[SYSTEM]+history`（:48-50）→ `llmEngine.generate` 流式 `onToken` 累加（:54-57）→ 落库 assistant 消息（:61-63）。
- `LlmEngineImpl.generate` 调用 `Chat.buildPrompt(messages)`（LlmEngineImpl.kt:50）时**不**重复传 systemPrompt（默认 null），而 history 已含 SYSTEM 消息，由 `buildPrompt` 遍历渲染（Chat.kt:35-50），**无重复 system 提示、无遗漏**。✅

### 3.3 sendMessage 竞态 / 空指针分析

- 单并发调度：`genDispatcher = Dispatchers.Default.limitedParallelism(1)`（ChatViewModel.kt:48），且 `sendText` 入口有 `isGenerating` 守卫（:77），避免重入。✅
- `history` 为进入 `launch` 前捕获的不可变快照，末条必为用户消息（`_uiState.value.messages + userMsg`，:80），故 `history.lastOrNull{role==USER}` 非空，query 不会抛异常（即便异常也被 `try/catch` 收敛为 `Result.Err`）。✅
- 流式回写 `onToken` 仅追加到最后一条消息（ChatViewModel.kt:99-107），`idx>=0` 保护，无越界/NPE。✅
- 语音：`whisper?.transcribe(...)` 对 null 安全返回空列表（SpeechEngineImpl.kt:40,58）；录音仅当 `modelReady` 后启动（ChatViewModel.kt:119），此时 whisper 必已加载（loadModel 顺序校验），无原生空指针。✅

**结论：核心算法与编排逻辑正确、无致命缺陷。**

---

## 四、智能路由判定

**判定：`ROUTE: NoOne`**

理由：
1. 静态一致性审查 18/18 全 PASS，**JNI 签名、Room、Manifest/res、依赖/import、DI、ViewModel/UI、CMake 源文件引用均逐字一致**，不存在编译必失败的源码级缺陷。
2. BM25、提示词拼装、sendMessage 编排经论证**正确且边界安全**，无运行时必崩路径。
3. 所有遗留项均为**构建期/环境性风险**或**非阻塞功能性缺口**（见第五节），不构成"致命源码 bug"，按规程不属于 Engineer 路由；且无独立测试代码需 QA 自修（QA 路由不适用）。

---

## 五、已知问题清单（含验证边界说明）

> ⚠️ **验证边界**：本机无 Android SDK / NDK / kotlinc，**无法执行 `./gradlew assembleDebug`、无法编译 APK、无法运行任何单元/插桩测试**。下列"PASS"为静态推理结论；"Native/CMake"相关项因 submodule 未检出（`app/src/main/cpp/third-party/*` 不在仓库树中）而无法端到端验证，列为构建期风险。

### 5.1 构建期 / 环境性风险（建议在 Android Studio 首次构建时重点核验）

| 编号 | 问题 | 位置 | 影响 | 建议 |
|------|------|------|------|------|
| K1 | **CMake `ggml` target 重名风险**：llama.cpp 与 whisper.cpp 各自携带 ggml，同时 `add_subdirectory` 可能导致 `target "ggml" already exists` 的 configure 错误 | CMakeLists.txt:12-14（作者已自注）、.gitmodules:8,12（submodule 指向 master 未钉版本） | `externalNativeBuild` 配置阶段即失败，无法产出 APK | 首次构建若报 ggml 重名，按作者注释为 whisper.cpp 配置独立 ggml 命名空间或 `EXCLUDE_FROM_ALL`/ALIAS 规避；**建议将 submodule 钉到稳定 tag 以可复现** |
| K2 | **llama.cpp C++ API 面版本依赖**：cpp 使用 `llama_vocab_bos / llama_get_logits / llama_vocab_is_eog / llama_token_to_piece / llama_kv_cache_clear / llama_batch_get_one` 等新式 API | llama-android-jni.cpp:40,42,135,143,159,164,168,186 | 若 submodule 锁定为旧版 llama.cpp（旧式 `llama_token_bos` 等），NDK 编译失败 | 钉 submodule 版本使其 API 与 cpp 调用一致；本环境无法核验 |
| K3 | **模型/知识库资源必须随包提供**：`ModelManager` 在缺失时抛 `IllegalStateException` | ModelManager.kt:68-74；ModelPaths.kt:12,18,21 | 无 `assets/models/*.gguf|bin` 与 `assets/knowledge/*.md` 时首启动崩溃（被 loadModel try/catch 捕获为 ModelLoadFailed，不致命但功能不可用） | 按 `assets/models/README.md` 放置 `Phi-3-mini-4K-Instruct-Q4_K_M.gguf`、`ggml-base.bin` 与 knowledge/*.md |

### 5.2 非阻塞功能性缺口（不影响编译/运行，建议后续优化）

| 编号 | 问题 | 位置 | 说明 |
|------|------|------|------|
| F1 | **`LlmParams`（temperature/topP/maxTokens）在生成时未生效**：`LlmEngineImpl.generate` 忽略入参 `params`（LlmEngineImpl.kt:42-57），采样参数仅在 `load` 时经 `Model.Params` 写入 native（且 cpp `createModel` 仅取 temperature/topP，llama-android-jni.cpp:93-94，maxTokens 在 generate 中**硬编码 2048**，:150） | LlmEngineImpl.kt / llama-android-jni.cpp | 设置页的温度/最大 Token 滑块**仅在模型（重新）加载后生效**；maxTokens 实际恒为 2048。属功能与 UI 预期不一致，非崩溃 |
| F2 | **BM25 单字中文查询召回低**：索引仅存 2-gram，单字查询（如"手"）作为单 term，无法匹配任何 2-gram → 可能返回空 | InvertedIndex.kt:108-115 | 设计性限制（无分词器的小语料 RAG-lite），非 bug；长查询不受影响 |
| F3 | **`isChinese()` 含不可达区间 `0x30000..0x3134F`**：Kotlin `Char` 为 UTF-16 代码单元（上限 0xFFFF），该区间恒为 false；CJK 扩展 B 等增补平面字符（代理对）未被识别为汉字 | InvertedIndex.kt:120-125 | 死代码 + 罕见字（扩展 B）不被当作中文处理；常见汉字无影响 |
| F4 | 预留常量/接口未使用：`ModelPaths.KNOWLEDGE_GLOB`（ModelPaths.kt:24）、`Constants.*` 键、`ChatRepository.getSessions()`（ChatRepository.kt:97）、`GenerationState`（T05 规划） | 多处 | 无害；建议后续清理或接入 |
| F5 | 用户消息落库异常被 `runCatching` 吞掉时，会话实体不会创建，后续 assistant 的 `touchSession` 变为无操作（会话列表不出现） | ChatViewModel.kt:85、ChatRepositoryImpl.kt:80-91 | 极端边界，内存态仍正常；建议失败上报告警 |

---

## 六、Android Studio 构建注意事项（给用户）

1. **初始化 submodule**：`git submodule update --init --recursive`，并确保 `app/src/main/cpp/third-party/llama.cpp`、`whisper.cpp` 存在；建议将 `.gitmodules` 中 `branch=master` 改为具体 tag/commit 以可复现（见 K1/K2）。
2. **NDK 与 CMake**：`app/build.gradle.kts:32-38` 要求 CMake `3.22.1`、`ANDROID_STL=c++_shared`、ABI `arm64-v8a/armeabi-v7a`；本地需安装对应 NDK（Side-by-side）。
3. **jitpack 网络**：`compose-markdown` 经 `settings.gradle.kts:21` 的 jitpack 仓库解析，首次构建需联网拉取（仅此依赖需外网；运行时完全离线）。
4. **模型/知识库资源**：将 `Phi-3-mini-4K-Instruct-Q4_K_M.gguf`、`ggml-base.bin` 放入 `app/src/main/assets/models/`，将知识库 `.md` 放入 `app/src/main/assets/knowledge/`（参考 `assets/models/README.md`），否则首启动报"模型文件缺失"（错误已友好兜底，不崩溃）。
5. **若 CMake 报 `ggml` 重名（K1）**：按 `CMakeLists.txt:12-14` 注释处理 whisper.cpp 的 ggml 命名空间；若报 llama API 找不到（K2）：同步 submodule 版本与 cpp 调用。
6. 本 QA 报告所有"PASS"为静态审查结论；建议在 Android Studio 内运行一次 `assembleDebug` 与（若可）JVM 单元测试以闭环验证 K1/K2/K3。

---

## 七、路由判定（末行）

ROUTE: NoOne
摘要（3 行）：
1) 静态一致性审查 18/18 全 PASS，JNI 签名/Room/Manifest/res/依赖/DI/UI 衔接/CMake 源引用逐字一致，无致命源码缺陷。
2) BM25 算法与提示词拼装、sendMessage 编排经论证正确且边界安全（空查询/无匹配/分母保护均覆盖）。
3) 遗留项仅为构建期风险（ggml 重名、llama API 版本、资源就位）与非阻塞功能缺口（LlmParams 未实时生效、单字中文召回），均不阻断静态结论，建议 Android Studio 首构建核验 K1–K3。
