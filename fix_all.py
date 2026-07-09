content = open("app/src/main/kotlin/com/aichat/app/engine/rag/RagEngineImpl.kt", "r", encoding="utf-8").read()
if "AssetExtractor()" in content:
    content = content.replace("AssetExtractor()", "AssetExtractor(context.assets)")
open("app/src/main/kotlin/com/aichat/app/engine/rag/RagEngineImpl.kt", "w", encoding="utf-8").write(content)

content = open("app/src/main/kotlin/com/aichat/app/ui/ChatScreen.kt", "r", encoding="utf-8").read()
if "import androidx.compose.foundation.layout.fillMaxWidth" not in content:
    content = content.replace(
        "import androidx.compose.foundation.layout.Column",
        "import androidx.compose.foundation.layout.Column\nimport androidx.compose.foundation.layout.fillMaxWidth\nimport androidx.compose.ui.unit.dp"
    )
open("app/src/main/kotlin/com/aichat/app/ui/ChatScreen.kt", "w", encoding="utf-8").write(content)

content = open("app/src/main/kotlin/com/aichat/app/ui/SettingsScreen.kt", "r", encoding="utf-8").read()
if "import androidx.compose.foundation.layout.Spacer" not in content:
    content = content.replace(
        "import androidx.compose.foundation.layout.Column",
        "import androidx.compose.foundation.layout.Column\nimport androidx.compose.foundation.layout.Spacer"
    )
open("app/src/main/kotlin/com/aichat/app/ui/SettingsScreen.kt", "w", encoding="utf-8").write(content)

content = open("app/src/main/kotlin/com/aichat/app/ui/component/InputBar.kt", "r", encoding="utf-8").read()
if "import androidx.compose.foundation.layout.padding" not in content:
    content = content.replace(
        "import androidx.compose.foundation.layout.Column",
        "import androidx.compose.foundation.layout.Column\nimport androidx.compose.foundation.layout.padding"
    )
open("app/src/main/kotlin/com/aichat/app/ui/component/InputBar.kt", "w", encoding="utf-8").write(content)

content = open("app/src/main/kotlin/com/aichat/app/ui/viewmodel/ChatViewModel.kt", "r", encoding="utf-8").read()
if "import kotlinx.coroutines.flow.update" not in content:
    content = content.replace(
        "import kotlinx.coroutines.flow.MutableStateFlow",
        "import kotlinx.coroutines.flow.MutableStateFlow\nimport kotlinx.coroutines.flow.update"
    )
open("app/src/main/kotlin/com/aichat/app/ui/viewmodel/ChatViewModel.kt", "w", encoding="utf-8").write(content)

content = open("app/src/main/kotlin/com/aichat/app/ui/viewmodel/SettingsViewModel.kt", "r", encoding="utf-8").read()
if "import kotlinx.coroutines.flow.update" not in content:
    content = content.replace(
        "import kotlinx.coroutines.flow.MutableStateFlow",
        "import kotlinx.coroutines.flow.MutableStateFlow\nimport kotlinx.coroutines.flow.update"
    )
open("app/src/main/kotlin/com/aichat/app/ui/viewmodel/SettingsViewModel.kt", "w", encoding="utf-8").write(content)

print("All fixes applied")
