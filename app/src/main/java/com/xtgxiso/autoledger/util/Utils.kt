package com.xtgxiso.autoledger.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * 金额工具。
 * 全部用「分」(Long) 存储和计算，绝不用 Float/Double —— 浮点数做金额会累加出 0.0000001 的误差。
 */
object Money {

    /** "25.80" / "¥1,286.40" -> 2580。解析失败返回 null */
    fun yuanToCents(input: String): Long? {
        val t = input.trim()
            .replace(",", "")
            .replace("¥", "")
            .replace("￥", "")
            .replace(" ", "")
        if (t.isEmpty()) return null
        return runCatching {
            BigDecimal(t).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
        }.getOrNull()
    }

    /** 2580 -> "25.80" */
    fun centsToYuan(cents: Long): String =
        BigDecimal(cents).movePointLeft(2).setScale(2, RoundingMode.HALF_UP).toPlainString()

    /** 2580 -> "25.8"（去掉无意义的尾零，日历格子这种窄地方用） */
    fun centsToYuanShort(cents: Long): String {
        val s = centsToYuan(cents)
        return s.removeSuffix("0").removeSuffix("0").removeSuffix(".")
    }

    /** 2580 -> "+25.80" / "-25.80" */
    fun signed(cents: Long, isExpense: Boolean): String =
        (if (isExpense) "-" else "+") + centsToYuan(cents)
}

/**
 * 时间工具。
 * 月份范围统一用 [月起, 下月起) 半开区间，避免月末边界重复/漏算。
 */
object Dates {

    private val hmFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val monthDayFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日")
    private val fullFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")

    fun zone(): ZoneId = ZoneId.systemDefault()

    fun toLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone()).toLocalDate()

    fun startOfDay(millis: Long): Long =
        toLocalDate(millis).atStartOfDay(zone()).toInstant().toEpochMilli()

    fun todayStart(): Long = LocalDate.now().atStartOfDay(zone()).toInstant().toEpochMilli()

    /** N 天前那天的 0 点（0 = 今天） */
    fun daysAgoStart(days: Long): Long =
        LocalDate.now().minusDays(days).atStartOfDay(zone()).toInstant().toEpochMilli()

    fun hm(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zone()).format(hmFmt)

    /** "11月5日" */
    fun monthDay(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zone()).format(monthDayFmt)

    /** "2026年11月5日 18:02" */
    fun formatFull(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zone()).format(fullFmt)

    /** 兼容旧调用：MM-dd HH:mm */
    fun formatTime(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zone())
            .format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))

    /** 今天 / 昨天 / 前天 / 周三 */
    fun dayHint(millis: Long): String {
        val days = java.time.temporal.ChronoUnit.DAYS.between(toLocalDate(millis), LocalDate.now())
        return when (days) {
            0L -> "今天"
            1L -> "昨天"
            2L -> "前天"
            else -> toLocalDate(millis).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)
        }
    }

    fun monthLabel(month: YearMonth): String = "${month.year}年${month.monthValue}月"

    fun monthStart(month: YearMonth): Long =
        month.atDay(1).atStartOfDay(zone()).toInstant().toEpochMilli()

    fun monthEnd(month: YearMonth): Long =
        month.plusMonths(1).atDay(1).atStartOfDay(zone()).toInstant().toEpochMilli()

    fun isThisMonth(month: YearMonth): Boolean = month == YearMonth.now()

    fun monthsBetween(from: YearMonth, to: YearMonth): Long =
        java.time.temporal.ChronoUnit.MONTHS.between(from, to)

    /** 日均支出用的分母：本月按已过天数，历史月份按整月天数 */
    fun dayCountForAverage(month: YearMonth): Int =
        if (isThisMonth(month)) LocalDate.now().dayOfMonth else month.lengthOfMonth()

    /** 周一起算的星期标题 */
    val weekTitles = listOf("一", "二", "三", "四", "五", "六", "日")

    /** DayOfWeek(周一=1) -> 0..6 */
    fun mondayIndex(day: DayOfWeek): Int = day.value - 1
}

/**
 * 记账分类。
 * V1.1 换成了更细的一套（对应记一笔页面的图标网格），同时保留旧名字的图标映射，
 * 保证老数据里的「日用 / 居住」也能正常显示。
 */
object Categories {

    val expense = listOf(
        "餐饮", "购物", "交通", "住宿", "日常",
        "学习", "人情", "娱乐", "美妆", "旅游",
        "医疗", "通讯", "基金", "其他",
    )

    val income = listOf("工资", "奖金", "理财", "红包", "退款", "其他收入")

    /** 兼容旧代码里 Categories.all */
    val all: List<String> get() = expense

    fun emoji(category: String): String = when (category) {
        "餐饮" -> "🍽"
        "购物" -> "🛍"
        "交通" -> "🚕"
        "住宿", "居住" -> "🏠"
        "日常", "日用" -> "🧻"
        "学习" -> "📚"
        "人情" -> "💗"
        "娱乐" -> "🎡"
        "美妆" -> "💄"
        "旅游" -> "🧳"
        "医疗" -> "🏥"
        "通讯" -> "📱"
        "基金" -> "💰"
        "工资" -> "💵"
        "奖金" -> "🎁"
        "理财" -> "📈"
        "红包" -> "🧧"
        "退款" -> "↩"
        "其他收入" -> "➕"
        else -> "⚙"
    }
}
