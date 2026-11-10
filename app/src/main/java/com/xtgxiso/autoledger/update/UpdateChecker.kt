package com.xtgxiso.autoledger.update

import com.xtgxiso.autoledger.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** 云端发布信息，对应 GitHub 仓库根目录的 version.json */
data class ReleaseInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val note: String,
)

/**
 * 联网检查更新：读 GitHub 仓库根目录 version.json，和本地 BuildConfig.VERSION_CODE 对比。
 * 只做「只读检查」，不做下载与安装；拿到云端最新版本后由 UI 决定是否弹窗。
 */
object UpdateChecker {

    // GitHub raw 文件直链：https://raw.githubusercontent.com/{owner}/{repo}/{branch}/{path}
    private const val VERSION_URL =
        "https://raw.githubusercontent.com/MoYu0117/AutoLedger--App/main/version.json"

    /** 仓库首页兜底（version.json 里没填下载地址时跳这里） */
    const val REPO_URL = "https://github.com/MoYu0117/AutoLedger--App"

    /**
     * 返回云端最新版本信息。
     * 网络异常 / 超时 / 解析失败 / 404 一律返回 null，静默失败，绝不打扰记账主流程。
     */
    suspend fun fetchLatest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = URL(VERSION_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "AutoLedger/${BuildConfig.VERSION_NAME}")
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            parse(text)
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun parse(json: String): ReleaseInfo? = try {
        val o = JSONObject(json)
        ReleaseInfo(
            versionCode = o.optInt("versionCode", 0),
            versionName = o.optString("versionName", ""),
            downloadUrl = o.optString("downloadUrl", ""),
            note = o.optString("note", ""),
        )
    } catch (e: Exception) {
        null
    }
}
