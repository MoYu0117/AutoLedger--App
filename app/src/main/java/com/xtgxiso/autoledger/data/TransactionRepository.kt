package com.xtgxiso.autoledger.data

import com.xtgxiso.autoledger.parse.NotificationParser
import com.xtgxiso.autoledger.parse.ParseResult
import kotlinx.coroutines.flow.Flow

sealed interface IngestResult {
    /** 成功入库 */
    data class Inserted(val id: Long, val parsed: ParseResult, val pending: Boolean) : IngestResult
    /** 判定为重复，丢弃 */
    data object Duplicate : IngestResult
    /** 解析不出金额，丢弃 */
    data object Unparsed : IngestResult
}

/**
 * 业务仓库：通知 -> 解析 -> 去重 -> 入库。
 * 所有写库逻辑都集中在这，UI 和 Service 都只调这里，保证口径一致。
 */
class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val ruleDao: RuleDao,
    private val settings: AppSettings,
) {

    fun observeMonth(from: Long, to: Long, status: Int): Flow<List<Transaction>> =
        transactionDao.observeRange(from, to, status)

    fun observeSince(from: Long, status: Int): Flow<List<Transaction>> =
        transactionDao.observeSince(from, status)

    fun observeCount(): Flow<Int> = transactionDao.observeCount()

    fun observeRules(): Flow<List<RuleEntity>> = ruleDao.observeAll()

    suspend fun enabledRules(): List<RuleEntity> = ruleDao.enabledOnce()

    suspend fun getById(id: Long): Transaction? = transactionDao.getById(id)

    /** 首次启动写入默认规则；顺带把 v1.0 的旧分类名改成 v1.1 的新名字 */
    suspend fun seedDefaultsIfNeeded() {
        if (ruleDao.count() == 0) ruleDao.insertAll(DefaultRules.list)
        // 幂等：老数据里的「日用 / 居住」统一归到新分类，避免图标和筛选对不上
        transactionDao.renameCategory("日用", "日常")
        transactionDao.renameCategory("居住", "住宿")
        ruleDao.renameCategory("日用", "日常")
        ruleDao.renameCategory("居住", "住宿")
    }

    /**
     * 收到一条通知后的完整处理。返回结果便于日志/调试。
     */
    suspend fun ingest(
        source: String,
        title: String,
        text: String,
        postedAt: Long,
    ): IngestResult {
        val rules = ruleDao.enabledOnce()
        val parsed = NotificationParser.parse(title, text, rules) ?: return IngestResult.Unparsed

        val window = settings.dedupeWindowMillis()
        val duplicate = transactionDao.findDuplicate(
            cents = parsed.amountCents,
            from = postedAt - window,
            to = postedAt + window,
            source = source,
        )
        if (duplicate != null) return IngestResult.Duplicate

        val auto = settings.shouldAutoConfirm()
        val id = transactionDao.insert(
            Transaction(
                amountCents = parsed.amountCents,
                isExpense = parsed.isExpense,
                payee = parsed.payee,
                category = parsed.category,
                time = postedAt,
                source = source,
                rawText = "$title | $text".trim(),
                status = if (auto) Transaction.CONFIRMED else Transaction.PENDING,
            )
        )
        return IngestResult.Inserted(id, parsed, pending = !auto)
    }

    /** 新增/修改一条账目（手动记账或编辑已识别记录） */
    suspend fun save(item: Transaction): Long {
        return if (item.id == 0L) {
            transactionDao.insert(item)
        } else {
            transactionDao.update(item)
            0L
        }
    }

    suspend fun confirm(item: Transaction) {
        transactionDao.update(item.copy(status = Transaction.CONFIRMED))
    }

    suspend fun delete(item: Transaction) = transactionDao.delete(item)

    suspend fun clearAll() = transactionDao.clearAll()

    /** 删除某时刻之前的记录，返回删除条数 */
    suspend fun deleteBefore(threshold: Long): Int = transactionDao.deleteBefore(threshold)

    /** 最早一笔记录的时间，没有则 null（「删除 X 月前数据」用来判断是否有那么老的数据） */
    suspend fun earliestTime(): Long? = transactionDao.earliestTime()

    suspend fun addRule(rule: RuleEntity) = ruleDao.insert(rule)

    suspend fun deleteRule(id: Long) = ruleDao.deleteById(id)

    /** 设置页「测试一句话」：用同一套解析逻辑跑一遍 */
    suspend fun testParse(title: String, text: String): ParseResult? =
        NotificationParser.parse(title, text, ruleDao.enabledOnce())
}
