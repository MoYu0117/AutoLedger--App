package com.xtgxiso.autoledger.ocr

import com.xtgxiso.autoledger.util.Money
import java.time.LocalDate

/**
 * 一条从图片识别出的候选账目，等待用户勾选确认。
 */
data class OcrBill(
    /** 商户名 */
    val payee: String,
    /** 金额，单位：分 */
    val amountCents: Long,
    /** true = 支出，false = 收入 */
    val isExpense: Boolean,
    /** 日期，可能为 null（图片里没写日期就默认今天） */
    val date: LocalDate?,
    /** 命中的原始文字行，排查用 */
    val rawLine: String,
)

/**
 * 把 OCR 得到的文字行，解析成一批候选账目。
 *
 * 针对两类常见图片：
 * 1. 微信支付凭证卡片：一行「商户名」+ 一行「¥ 金额」+ 可能「支付成功/对方已收款」；
 * 2. 微信账单流水列表：每行「标题 + 日期时间 + 金额」，标题常是「扫二维码付款-给XX」。
 *
 * 解析原则：金额是硬指标（没有金额的算不出账），方向看正负号或关键词，商户从同行/相邻行抠。
 */
object OcrBillParser {

    // 金额：¥ 或 ￥ 开头，或带正负号，或纯数字带小数
    private val moneyRe = Regex("""([¥￥]|[-+])?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""")
    // 日期：11月9日 / 2026年11月9日 / 昨天 / 今天
    private val dateRe = Regex("""(\d{1,2})月(\d{1,2})日""")
    private val fullDateRe = Regex("""(\d{4})年(\d{1,2})月(\d{1,2})日""")

    // 收入关键词
    private val incomeWords = listOf("红包", "收款", "收入", "转入", "退款", "入账", "到账", "存入")
    // 支出关键词
    private val expenseWords = listOf("付款", "支付", "消费", "支出", "扣款", "转账", "商户", "商家")

    fun parse(lines: List<String>): List<OcrBill> {
        val bills = mutableListOf<OcrBill>()
        var pendingDate: LocalDate? = null

        for (line in lines) {
            val t = line.trim()
            if (t.isEmpty()) continue

            // 先看这行有没有日期，有则记住（流水列表里日期单独一行或和标题同行）
            fullDateRe.find(t)?.let { m ->
                pendingDate = runCatching {
                    LocalDate.of(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
                }.getOrNull()
            }
            dateRe.find(t)?.let { m ->
                val now = LocalDate.now()
                pendingDate = runCatching {
                    LocalDate.of(now.year, m.groupValues[1].toInt(), m.groupValues[2].toInt())
                }.getOrNull()
            }

            val amount = extractAmount(t) ?: continue
            // 金额必须是「明确的账目行」，避免把「11月9日 08:59」里的 11、9 误当金额。
            if (amount.raw.isEmpty()) continue

            val isExpense = judgeDirection(t, amount.hasMinus)
            // 商户：同行去掉金额和日期后的剩余文字；流水标题「扫二维码付款-给XX」取「给」后面的部分
            val payee = extractPayee(t, amount.raw) ?: "未识别商户"

            bills.add(
                OcrBill(
                    payee = payee,
                    amountCents = amount.cents,
                    isExpense = isExpense,
                    date = pendingDate,
                    rawLine = t,
                )
            )
        }

        return bills
    }

    private data class Amount(val cents: Long, val raw: String, val hasMinus: Boolean)

    private fun extractAmount(line: String): Amount? {
        // 找金额：优先「¥/￥」符号或带小数点的数，避免把纯日期数字当金额
        val symbolMatch = Regex("""[¥￥]\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""").find(line)
        if (symbolMatch != null) {
            val raw = symbolMatch.groupValues[1]
            val cents = Money.yuanToCents(raw) ?: return null
            if (cents <= 0L) return null
            return Amount(cents, raw, hasMinus = false)
        }

        // 带小数点的数（如 -8.00、+1.80）
        val decimalMatch = Regex("""([-+])?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)\b""").find(line)
            ?: return null
        val sign = decimalMatch.groupValues[1]
        val raw = decimalMatch.groupValues[2]
        // 只有带小数点，或符号+金额才可靠；纯整数行（如「共5笔」）跳过
        if (!raw.contains('.') && sign.isEmpty()) return null
        val cents = Money.yuanToCents(raw) ?: return null
        if (cents <= 0L) return null
        return Amount(cents, raw, hasMinus = sign == "-")
    }

    private fun judgeDirection(line: String, hasMinus: Boolean): Boolean {
        if (hasMinus) return true
        val hasIncome = incomeWords.any { line.contains(it) }
        val hasExpense = expenseWords.any { line.contains(it) }
        return when {
            hasIncome && !hasExpense -> false
            hasExpense && !hasIncome -> true
            else -> true // 默认支出
        }
    }

    private fun extractPayee(line: String, amountRaw: String): String? {
        // 去掉金额部分
        var s = line.replace(amountRaw, "").trim()
        s = s.replace(Regex("""[¥￥]"""), "").trim()
        s = s.replace(Regex("""[-+]"""), "").trim()
        // 去掉日期
        s = s.replace(dateRe, "").replace(fullDateRe, "").trim()
        // 去掉常见标签词
        val stripWords = listOf(
            "使用零钱通支付", "使用零钱支付", "使用银行卡支付", "使用信用卡支付",
            "支付成功", "对方已收款", "交易状态", "账单详情", "查看交易详情", "留言",
            "昨天", "今天", "前天",
        )
        for (w in stripWords) s = s.replace(w, "")
        s = s.trim().trim('·', '—', '-', '：', ':', '>', ' ')

        // 流水标题「扫二维码付款-给XX」→ 取「给」后面
        if (s.contains("给")) {
            s = s.substringAfterLast("给").trim()
        } else {
            s = s.substringAfterLast("-").trim()
        }
        return s.takeIf { it.isNotBlank() && it.length in 1..20 }
    }
}
