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

        // 鍘熺敓鏋勫缓浠呬繚鐣欏父鐢?ABI锛屽噺灏忎綋绉?

        // Room Schema 浣嶇疆锛坋xportSchema = false锛屾棤闇€瀵煎嚭 schema锛屾晠姝ゅ鐣欑┖锛?
    }

    // 鍘熺敓锛圕++/JNI锛夋瀯寤洪厤缃?

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 绛惧悕閰嶇疆鐢?CI / 鏈湴 local.properties 娉ㄥ叆锛堝闇€鏈湴绛惧悕鍙嚜琛屾坊鍔?signingConfig锛?
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

    // gguf / bin 涓哄凡鍘嬬缉妯″瀷鏂囦欢锛岀姝?aapt 浜屾鍘嬬缉
    aaptOptions {
        noCompress.addAll(listOf("gguf", "bin"))
    }

    // 淇濈暀 jniLibs 鍘熸湁鎵撳寘鏂瑰紡锛堜笉浣跨敤 legacy 鍏煎鍖咃級
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
    // Compose BOM锛堢粺涓€绠＄悊 Compose 鐗堟湰锛?
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // 鎵╁睍鍥炬爣
    implementation("androidx.compose.material:material-icons-extended")

    // Activity + Compose
    implementation("androidx.activity:activity-compose:1.9.0")

    // ViewModel / Lifecycle锛圕ompose 闆嗘垚锛?
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // 鏍稿績 KTX
    implementation("androidx.core:core-ktx:1.13.1")

    // 鍗忕▼
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // DataStore锛堝亸濂借缃寔涔呭寲锛?
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // 瀵艰埅
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room锛圞SP 娉ㄨВ澶勭悊锛?
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Markdown 娓叉煋锛堝璇濆唴瀹癸級
    implementation("com.github.jeziellago:compose-markdown:0.5.0")

    // 绯荤粺鏍忥紙鐘舵€佹爮/瀵艰埅鏍忥級棰滆壊鎺у埗
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")

    // Material AndroidX 搴擄細鎻愪緵 XML 骞冲彴涓婚 Theme.Material3.DayNight.NoActionBar
    // 锛堜緵 AndroidManifest 鐨?android:theme 浣跨敤锛汣ompose 渚т粛浣跨敤 AIChatTheme锛?
    implementation("com.google.android.material:material:1.12.0")

    // 棰勮 / 宸ュ叿
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
