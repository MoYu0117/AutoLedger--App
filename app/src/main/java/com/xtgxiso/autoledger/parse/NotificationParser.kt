package com.xtgxiso.autoledger.parse

import com.xtgxiso.autoledger.data.RuleEntity
import com.xtgxiso.autoledger.util.Money

data class ParseResult(
    val amountCents: Long,
    val isExpense: Boolean,
    val payee: String,
    val category: String,
    /** 命中的金额写法，排查用 */
    val matchedPattern: String,
)

/**
 * 通知文本解析器：把一条通知的「标题 + 正文」变成一条账目。
 *
 * 设计原则：
 * 1. 纯函数，无 Android 依赖 —— 设置页的「测试一句话」和后台服务调用的是同一份逻辑，不会出现"测试能过、真机不灵"。
 * 2. 规则可外部注入 —— 微信/支付宝改文案时改规则表即可。
 * 3. 宁可漏，不可错得太离谱 —— 解析不出金额就返回 null，交给用户手动补记。
 */
object NotificationParser {

    /** 这些标题太泛，不能当商户名 */
    private val genericTitles = setOf("支付宝", "微信支付", "微信", "WeChat", "支付通知", "服务通知")

    /** 金额写法，按优先级匹配（¥ 符号最可靠，其次 "xx元"，最后是"支出 25.8" 这种） */
    private val amountPatterns: List<Pair<String, Regex>> = listOf(
        "符号金额" to Regex("""[¥￥]\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)"""),
        "元单位金额" to Regex("""([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*元"""),
        "动作+金额" to Regex(
            """(?:支出|收入|消费|付款|支付|扣款|收款|退款|入账|到账)\s*(?:金额)?\s*[:：]?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)"""
        ),
    )

    /** 商户名写法 */
    private val merchantPatterns: List<Regex> = listOf(
        Regex("""(?:收款方|商户全称|商户名称|商户|付款给|付款方|交易对方|收款人)\s*[:：]\s*([^\s,，。;；]{1,24})"""),
        Regex("""在\s*([^\s,，。;；]{2,20})\s*(?:消费|支付|付款)"""),
    )

    private val baseIncomeWords = listOf("收款", "收到", "收入", "入账", "到账", "退款", "转入", "存入", "红包")
    private val baseExpenseWords = listOf(
        "支付成功", "已支付", "付款成功", "支出", "消费", "扣款",
        "转账给", "已付款", "付款金额", "自动续费", "账单"
    )

    fun parse(
        title: String,
        text: String,
        rules: List<RuleEntity> = emptyList(),
    ): ParseResult? {
        val full = "$title $text".trim()
        if (full.isBlank()) return null

        // 1) 金额：按优先级找第一个能解析出正数的
        var amountCents: Long? = null
        var patternName = ""
        for ((name, regex) in amountPatterns) {
            val raw = regex.find(full)?.groupValues?.getOrNull(1) ?: continue
            val cents = Money.yuanToCents(raw) ?: continue
            if (cents > 0L) {
                amountCents = cents
                patternName = name
                break
            }
        }
        val cents = amountCents ?: return null

        // 2) 收支方向：额外规则词优先叠加到基础词表上
        val incomeWords = baseIncomeWords +
            rules.filter { it.kind == RuleEntity.KIND_INCOME && it.enabled }.map { it.keyword }
        val expenseWords = baseExpenseWords +
            rules.filter { it.kind == RuleEntity.KIND_EXPENSE && it.enabled }.map { it.keyword }

        val incomeHit = incomeWords.any { it.isNotBlank() && full.contains(it) }
        val expenseHit = expenseWords.any { it.isNotBlank() && full.contains(it) }
        val isExpense = when {
            incomeHit && !expenseHit -> false
            expenseHit && !incomeHit -> true
            // 两个都命中：退款/收款类优先判为收入，其余判支出
            incomeHit && expenseHit -> !(full.contains("退款") || full.contains("收款"))
            else -> true
        }

        // 3) 商户：先找"商户：xxx"，再退回到标题
        var merchant = merchantPatterns.firstNotNullOfOrNull {
            it.find(full)?.groupValues?.getOrNull(1)
        }
        if (merchant.isNullOrBlank()) {
            val t = title.trim()
            merchant = if (t.isNotBlank() && genericTitles.none { g -> t.contains(g) }) t else null
        }
        val payee = merchant?.trim()?.trim(',', '，', '。', '.', '：', ':', '-', ' ')?.takeIf { it.isNotBlank() }
            ?: "未识别商户"

        // 4) 分类：按规则表顺序，命中第一个就用它
        val category = rules.firstOrNull { rule ->
            rule.kind == RuleEntity.KIND_PAYEE &&
                rule.enabled &&
                rule.keyword.isNotBlank() &&
                rule.category.isNotBlank() &&
                (payee.contains(rule.keyword) || full.contains(rule.keyword))
        }?.category ?: if (isExpense) "其他" else "其他收入"

        return ParseResult(
            amountCents = cents,
            isExpense = isExpense,
            payee = payee,
            category = category,
            matchedPattern = patternName,
        )
    }
}
