# 模型文件存放说明

本目录用于放置端侧推理所需的模型权重。**模型文件不随仓库提交**（已在 `.gitignore` 中忽略 `*.gguf` / `*.bin`），请按下方说明手动获取后放入本目录。

## 必须的文件

| 文件名 | 作用 | 下载链接 |
| --- | --- | --- |
| `Phi-3-mini-4K-Instruct-Q4_K_M.gguf` | 端侧对话大模型（Phi-3-mini 4K 指令版，Q4_K_M 量化） | [HuggingFace 下载](https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/tree/main) → 选 `Phi-3-mini-4k-instruct-q4.gguf` 或同名文件 |
| `ggml-base.bin` | 端侧语音识别模型（whisper base） | [HuggingFace 下载](https://huggingface.co/ggerganov/whisper.cpp/blob/main/ggml-base.bin) |

> 文件名必须与 `com.aichat.app.config.ModelPaths` 中定义的常量完全一致：
> - `LLAMA_MODEL_FILE = "Phi-3-mini-4K-Instruct-Q4_K_M.gguf"`
> - `WHISPER_MODEL_FILE = "ggml-base.bin"`

## 放置方式（任选其一）

1. **打包进安装包（推荐调试用）**：把上面两个文件复制到本目录 `app/src/main/assets/models/`，
   应用首启时 `AssetExtractor` 会把它们流式拷贝到 `filesDir/models/`（带进度），
   DataStore 记录模型版本号，仅当版本变化或文件缺失时才重新拷贝。

2. **直接放入应用私有目录（推荐发布用，避免 APK 过大）**：
   把模型放到 `/data/data/com.aichat.app/files/models/` 下，文件名同上。
   应用会优先使用该路径，跳过拷贝步骤。

## 可选：首启联网拉取

代码中"首启可选联网拉取"开关**默认关闭**，以保持完全离线。如需开启，请在设置
（`SettingsRepository` 的 `networkFallback`）中显式打开，并实现下载逻辑（本批未包含下载 UI）。

## 直接下载命令（在模型目录执行 PowerShell）

```powershell
# 下载 Phi-3-mini GGUF（~1.2GB，如果文件名不完全匹配，请进 HuggingFace 页面确认实际文件名）
Invoke-WebRequest -Uri "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf" -OutFile "Phi-3-mini-4K-Instruct-Q4_K_M.gguf"

# 下载 whisper base 模型（~150MB）
Invoke-WebRequest -Uri "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin" -OutFile "ggml-base.bin"
```
