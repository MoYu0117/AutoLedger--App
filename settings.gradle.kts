pluginManagement {
    repositories {
        // 国内镜像优先：先问阿里云，没有再问官方源。网络不好时可把这两行删掉
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/google")

        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        // 同上：阿里云优先，官方源兜底
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")

        google()
        mavenCentral()
    }
}

rootProject.name = "AutoLedger"
include(":app")
