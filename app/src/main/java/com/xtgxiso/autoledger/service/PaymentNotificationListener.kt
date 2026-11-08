package com.xtgxiso.autoledger.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.xtgxiso.autoledger.AutoLedgerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 核心服务：监听支付类通知。
 *
 * 为什么优先用通知而不是识屏？
 * - 通知文本由 App 主动推送，格式相对稳定，且不需要无障碍权限；
 * - 无障碍读控件容易被版本更新搞坏，还带高敏感权限，放在 V2 再考虑。
 *
 * 注意：本服务只有在用户在系统设置里勾选了「自动记账」通知使用权之后才会收到回调，
 * 代码里无法自行申请，必须在设置页引导用户跳转。
 */
class PaymentNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        val pkg = notification.packageName ?: return
        val extras = notification.notification?.extras ?: return

        // 提取标题与正文（正文可能分散在 text / bigText / textLines 里）
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val body = buildString {
            extras.getCharSequence(Notification.EXTRA_TEXT)?.let { append(it) }
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let {
                if (it.isNotEmpty() && !contains(it)) append(' ').append(it)
            }
            // EXTRA_TEXT_LINES 是 CharSequence 数组，必须用 getCharSequenceArray
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach { line ->
                val s = line?.toString().orEmpty()
                if (s.isNotEmpty() && !contains(s)) append(' ').append(s)
            }
        }.trim()

        if (title.isBlank() && body.isBlank()) return

        val postedAt = notification.postTime.takeIf { it > 0 } ?: System.currentTimeMillis()

        scope.launch {
            try {
                val app = AutoLedgerApp.instance
                // 只处理用户勾选过的 App，避免把无关通知也解析一遍
                if (!app.settings.isWatched(pkg)) return@launch

                val result = app.repository.ingest(pkg, title, body, postedAt)
                Log.d(TAG, "[$pkg] $title | $body -> $result")
            } catch (t: Throwable) {
                Log.w(TAG, "解析通知失败", t)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AutoLedger"
    }
}
