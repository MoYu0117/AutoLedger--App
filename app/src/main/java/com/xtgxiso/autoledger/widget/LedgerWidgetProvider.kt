package com.xtgxiso.autoledger.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.xtgxiso.autoledger.AutoLedgerApp
import com.xtgxiso.autoledger.MainActivity
import com.xtgxiso.autoledger.R
import com.xtgxiso.autoledger.util.Dates
import com.xtgxiso.autoledger.util.Money
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 2×2 桌面小组件：显示「今日支出 / 今日收入」，点「查看账单」进主页，点「+」直接记一笔。
 * 数据来自本地 Room（今日已入账记录），离线、不联网。
 */
class LedgerWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_ledger)

        // 点卡片主体 / 查看账单 -> 打开 App 主页
        views.setOnClickPendingIntent(R.id.widget_root, appPendingIntent(context, false))
        views.setOnClickPendingIntent(R.id.widget_view_bills, appPendingIntent(context, false))
        // 点 + -> 直接进记一笔
        views.setOnClickPendingIntent(R.id.widget_add, appPendingIntent(context, true))

        // 先放占位，再异步读库填充
        scope.launch {
            val todayStart = Dates.todayStart()
            val todayEnd = todayStart + 24L * 60 * 60 * 1000
            val list = runCatching {
                AutoLedgerApp.instance.repository.todayRange(todayStart, todayEnd)
            }.getOrDefault(emptyList())

            val expense = list.filter { it.isExpense }.sumOf { it.amountCents }
            val income = list.filterNot { it.isExpense }.sumOf { it.amountCents }

            views.setTextViewText(R.id.widget_expense_amount, "¥" + Money.centsToYuan(expense))
            views.setTextViewText(R.id.widget_income_amount, "¥" + Money.centsToYuan(income))
            manager.updateAppWidget(widgetId, views)
        }
    }

    private fun appPendingIntent(context: Context, openAdd: Boolean): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_ADD, openAdd)
        }
        return PendingIntent.getActivity(
            context,
            if (openAdd) 1 else 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        /** 数据变化后刷新所有小组件（在账目保存后调用） */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, LedgerWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) {
                val intent = Intent(context, LedgerWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
