package com.xtgxiso.autoledger.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/** 判断「通知使用权」是否已开启 —— 这是整个 App 的总开关 */
fun isNotificationAccessGranted(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

private fun Context.safeStart(intent: Intent) {
    runCatching {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** 跳系统「通知使用权」列表页，用户在里面勾选「自动记账」 */
fun openNotificationListenerSettings(context: Context) {
    context.safeStart(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
}

/** 申请忽略电池优化（保活关键），失败则退回到系统电池优化列表 */
fun openBatteryOptimizationSettings(context: Context) {
    val direct = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${context.packageName}")
    )
    val ok = runCatching { context.startActivity(direct.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
    if (!ok) {
        context.safeStart(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }
}

/** 打开本应用详情页（在这里可以设置「自启动」「后台运行」「省电策略：无限制」） */
fun openAppDetailSettings(context: Context) {
    context.safeStart(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
    )
}

data class AppInfo(val label: String, val pkg: String)

/** 列出手机里能点开的 App（用于设置页勾选监听范围） */
fun loadLaunchableApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return runCatching {
        pm.queryIntentActivities(intent, 0)
            .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .filter { it.pkg != context.packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label }
    }.getOrDefault(emptyList())
}

/** 包名 -> 中文名，界面上显示"微信/支付宝"而不是一串包名 */
fun sourceLabel(source: String): String = when (source) {
    "com.tencent.mm" -> "微信"
    "com.eg.android.AlipayGphone" -> "支付宝"
    "manual" -> "手动"
    else -> source
}
