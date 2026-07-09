# ============================================================
# 混淆规则 —— 端侧 AI 对话 App
# ============================================================

# 保留 JNI 桥接类与方法（方法名需与 C++ 中 Java_xxx 函数逐字对应）
-keep class com.aichat.app.jni.llama.** { *; }
-keep class com.aichat.app.jni.whisper.** { *; }

# 保留 JNI 回调接口（Kotlin SAM / 接口被 C++ 通过反射调用）
-keep interface com.aichat.app.jni.llama.Llama$TokenCallback { *; }
-keep interface com.aichat.app.jni.whisper.Whisper$SegmentCallback { *; }

# 保留 native 方法声明（防止方法名被混淆导致 JNI 查找失败）
-keepclasseswithmembernames class * {
    native <methods>;
}

# Room：保留实体与 DAO
-keep class com.aichat.app.data.local.room.** { *; }
-keep @androidx.room.Entity class * { *; }

# DataStore / Preferences 序列化
-keep class androidx.datastore.preferences.protobuf.** { *; }

# 协程
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# 保留领域模型（避免 Room / 反射序列化问题）
-keep class com.aichat.app.domain.model.** { *; }
-keep class com.aichat.app.domain.Result { *; }
-keep class com.aichat.app.domain.AppError* { *; }

# 通用：保留带 @Keep 注解的内容
-keep @androidx.annotation.Keep class * { *; }
-keepclasseswithmembers class * { @androidx.annotation.Keep <methods>; }
-keepclasseswithmembers class * { @androidx.annotation.Keep <fields>; }
