// 根 build.gradle.kts：只声明插件版本，不在这里上依赖
plugins {
    // AGP 8.6.1：正式支持 compileSdk 35，且兼容 JDK 17~21
    id("com.android.application") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}
