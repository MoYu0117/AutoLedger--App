package com.xtgxiso.autoledger.ocr

import com.xtgxiso.autoledger.util.Money
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

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
    /** 具体时分，可能为 null（图片里没写时间就默认 12:00） */
    val time: LocalTime?,
    /** 命中的原始文字行，排查用 */
    val rawLine: String,
)

/**
 * 把 OCR 得到的文字行，解析成一批候选账目。
 *
 * 针对微信账单流水列表的典型结构（每笔记录占 2~3 行）：
 *   第 1 行：标题（如「扫二维码付款-给cyk 622」「微信红包-来自堆花酒」「界埠佰川便民超市」）
 *   第 2 行：日期时间（如「11月10日 11:33」）
 *   第 3 行：金额（如「-0.02」「+1.80」）
 *
 * 也兼容单行凭证卡片（「商户名 ¥25.80」）。
 *
 * 解析原则：
 *  - 金额是硬指标（没有金额的算不出账）。
 *  - 方向：优先看金额行的正负号；其次看标题关键词（红包/收款/转入 → 收入；付款/转账/消费 → 支出）。
 *  - 商户名：从「标题」行提取；「转账-转给XX / 扫二维码付款-给XX」取「转给/给」后的部分；否则整行当商户名。
 *  - 日期时间：独立成行或与标题同行都会解析，随后面最近的账目。
 */
object OcrBillParser {

    // 金额：带小数点的数，可带 ¥/￥ 或正负号。用「非贪婪+边界」避免把「11:33」里的数字当金额。
    private val moneySymbolRe = Regex("""[¥￥]\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""")
    private val moneySignRe = Regex("""([-+])\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""")

    private val dateRe = Regex("""(\d{1,2})月(\d{1,2})日""")
    private val fullDateRe = Regex("""(\d{4})年(\d{1,2})月(\d{1,2})日""")
    private val timeRe = Regex("""(\d{1,2}):(\d{2})""")

    // 收入关键词（优先于支出）
    private val incomeWords = listOf("红包", "收款", "收入", "转入", "退款", "入账", "到账", "存入", "来自")
    // 支出关键词
    private val expenseWords = listOf("付款", "支付", "消费", "支出", "扣款", "转账", "商户", "商家", "转给", "给")

    fun parse(lines: List<String>): List<OcrBill> {
        val bills = mutableListOf<OcrBill>()

        // 遍历时记住「最近一个还没配到金额的标题」，以及最近的日期时间。
        var pendingTitle: String? = null
        var pendingDate: LocalDate? = null
        var pendingTime: LocalTime? = null

        for (raw in lines) {
            val t = raw.trim()
            if (t.isEmpty()) continue

            // 1) 日期 / 时间：记下来，等金额行出现时绑定
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
            timeRe.find(t)?.let { m ->
                pendingTime = runCatching {
                    LocalTime.of(m.groupValues[1].toInt(), m.groupValues[2].toInt())
                }.getOrNull()
            }

            // 2) 金额：这行有金额才算一条账；没有金额就只当标题候选
            val amount = extractAmount(t)
            if (amount == null) {
                if (looksLikeTitle(t)) pendingTitle = t
                continue
            }

            // 3) 标题：优先用「本行残留文字」当商户；若本行只剩金额，就用之前记住的标题
            var payee = extractPayeeFromLine(t, amount.raw)
            if (payee == null || payee.length < 2) {
                payee = pendingTitle?.let { extractPayeeFromLine(it, "") }
            }
            if (payee.isNullOrBlank()) payee = "未识别商户"

            // 4) 方向：金额符号优先，其次标题关键词
            val directionFromLine = judgeDirection(t, amount.hasMinus)
            val directionFromTitle = pendingTitle?.let { judgeDirection(it, false) } ?: directionFromLine
            // 若标题明确说了「红包/收款」，即便金额行没符号也按收入
            val isExpense = when {
                amount.hasMinus -> true
                amount.hasPlus -> false
                pendingTitle != null && hasIncomeWord(pendingTitle!!) -> false
                pendingTitle != null && hasExpenseWord(pendingTitle!!) -> true
                else -> directionFromLine
            }

            bills.add(
                OcrBill(
                    payee = payee,
                    amountCents = amount.cents,
                    isExpense = isExpense,
                    date = pendingDate,
                    time = pendingTime,
                    rawLine = t,
                )
            )

            // 这一笔配完了，清掉标题（金额和日期时间可以保留给下一行没有日期的情况）
            pendingTitle = null
        }

        return bills
    }

    private data class Amount(
        val cents: Long,
        val raw: String,
        val hasMinus: Boolean,
        val hasPlus: Boolean,
    )

    private fun extractAmount(line: String): Amount? {
        // 优先 ¥/￥ 符号
        moneySymbolRe.find(line)?.let { m ->
            val raw = m.groupValues[1]
            val cents = Money.yuanToCents(raw) ?: return null
            if (cents <= 0L) return null
            return Amount(cents, raw, hasMinus = false, hasPlus = false)
        }
        // 带正负号的金额
        moneySignRe.find(line)?.let { m ->
            val sign = m.groupValues[1]
            val raw = m.groupValues[2]
            val cents = Money.yuanToCents(raw) ?: return null
            if (cents <= 0L) return null
            return Amount(cents, raw, hasMinus = sign == "-", hasPlus = sign == "+")
        }
        // 纯数字带小数（如「25.80」），且不能是「11:33」这种时间或日期
        val plain = Regex("""(?:^|[^0-9])([0-9]+(?:\.[0-9]{2}))(?:$|[^0-9])""").find(line) ?: return null
        val raw = plain.groupValues[1]
        val cents = Money.yuanToCents(raw) ?: return null
        if (cents <= 0L) return null
        return Amount(cents, raw, hasMinus = false, hasPlus = false)
    }

    private fun looksLikeTitle(line: String): Boolean {
        // 含商户/方向关键词、且没有金额的，当标题
        return hasIncomeWord(line) || hasExpenseWord(line) ||
            line.contains("转账") || line.contains("红包") || line.contains("支付")
    }

    private fun hasIncomeWord(s: String) = incomeWords.any { s.contains(it) }
    private fun hasExpenseWord(s: String) = expenseWords.any { s.contains(it) }

    private fun judgeDirection(line: String, hasMinus: Boolean): Boolean {
        if (hasMinus) return true
        val hasIncome = hasIncomeWord(line)
        val hasExpense = hasExpenseWord(line)
        return when {
            hasIncome && !hasExpense -> false
            hasExpense && !hasIncome -> true
            else -> true
        }
    }

    /**
     * 从一行里抠商户名。金额 raw 为空时表示这行本身是纯标题。
     */
    private fun extractPayeeFromLine(line: String, amountRaw: String): String? {
        var s = if (amountRaw.isNotEmpty()) line.replace(amountRaw, "") else line
        s = s.replace(Regex("""[¥￥]"""), "").trim()
        s = s.replace(Regex("""[-+]"""), "").trim()
        s = s.replace(dateRe, "").replace(fullDateRe, "").trim()
        s = s.replace(timeRe, "").trim()

        val stripWords = listOf(
            "使用零钱通支付", "使用零钱支付", "使用银行卡支付", "使用信用卡支付",
            "支付成功", "对方已收款", "交易状态", "账单详情", "查看交易详情", "留言",
            "昨天", "今天", "前天",
        )
        for (w in stripWords) s = s.replace(w, "")
        s = s.trim().trim('·', '—', '-', '：', ':', '>', ' ')

        // 微信流水标题三种格式：
        //   「转账-转给XX」 / 「扫二维码付款-给XX」 / 「XX」（直接商户名）
        for (kw in listOf("转给", "给")) {
            if (s.contains(kw)) {
                return s.substringAfterLast(kw).trim().takeIf { it.isNotBlank() && it.length in 1..30 }
            }
        }
        // 「扫二维码付款」「转账」「微信红包-来自」等前缀去掉
        val prefixes = listOf("扫二维码付款", "二维码付款", "扫一扫付款", "微信红包", "转账")
        for (p in prefixes) s = s.replace(p, "")
        s = s.trim().trim('-', '·', '—', ' ', ':')

        return s.takeIf { it.isNotBlank() && it.length in 1..30 }
    }
}
