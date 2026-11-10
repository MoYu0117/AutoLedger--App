// 注意：必须在 plugins 块之前 import。
// 直接写 java.util.Properties 会报 "Unresolved reference: util" ——
// 因为在 .kts 脚本里 java 指的是 Gradle 的 java 扩展，不是 java 包。
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.xtgxiso.autoledger"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.xtgxiso.autoledger"
        minSdk = 26          // Android 8.0 起，JAVA 8 时间 API、通知接口都齐全
        targetSdk = 34
        versionCode = 9
        versionName = "1.4.1"
    }

    // 读取 key.properties 做正式签名；文件不存在时自动降级为 debug 签名（自己用完全够）
    signingConfigs {
        create("release") {
            val propFile = rootProject.file("key.properties")
            if (propFile.exists()) {
                val p = Properties()
                propFile.inputStream().use { stream -> p.load(stream) }
                storeFile = rootProject.file(p.getProperty("storeFile"))
                storePassword = p.getProperty("storePassword")
                keyAlias = p.getProperty("keyAlias")
                keyPassword = p.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            if (rootProject.file("key.properties").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        // 设置页要显示版本号（BuildConfig.VERSION_NAME）
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // 自定义 APK 输出文件名：AutoLedger-v1.4.1.apk（不再叫 app-release.apk）
    // 注意：版本号改 versionName 时，这里也同步改一下
    androidComponents {
        onVariants(selector().all()) { variant ->
            variant.outputs.forEach { output ->
                (output as? com.android.build.api.variant.impl.VariantOutputImpl)?.outputFileName =
                    "AutoLedger-v1.4.1.apk"
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // 本地 OCR 识图（离线、免费、图片不出手机）。用中文模型，体积约 +3MB
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")
    // ML Kit 的 Task 转协程 await
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
