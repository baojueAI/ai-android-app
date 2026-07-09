plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.aichat.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aichat.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // 原生构建仅保留常用 ABI，减小体积
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        // Room Schema 位置（exportSchema = false，无需导出 schema，故此处留空）
    }

    // 原生（C++/JNI）构建配置
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
            // 使用 libc++ 共享 STL（与 submodule 中 ggml 保持一致）
            arguments += listOf("-DANDROID_STL=c++_shared")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 签名配置由 CI / 本地 local.properties 注入（如需本地签名可自行添加 signingConfig）
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // gguf / bin 为已压缩模型文件，禁止 aapt 二次压缩
    aaptOptions {
        noCompress += listOf("gguf", "bin")
    }

    // 保留 jniLibs 原有打包方式（不使用 legacy 兼容包）
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    // Compose BOM（统一管理 Compose 版本）
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // 扩展图标
    implementation("androidx.compose.material:material-icons-extended")

    // Activity + Compose
    implementation("androidx.activity:activity-compose:1.9.0")

    // ViewModel / Lifecycle（Compose 集成）
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // 核心 KTX
    implementation("androidx.core:core-ktx:1.13.1")

    // 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // DataStore（偏好设置持久化）
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // 导航
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room（KSP 注解处理）
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Markdown 渲染（对话内容）
    implementation("com.github.jeziellago:compose-markdown:0.5.0")

    // 系统栏（状态栏/导航栏）颜色控制
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")

    // Material AndroidX 库：提供 XML 平台主题 Theme.Material3.DayNight.NoActionBar
    // （供 AndroidManifest 的 android:theme 使用；Compose 侧仍使用 AIChatTheme）
    implementation("com.google.android.material:material:1.12.0")

    // 预览 / 工具
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
