package com.xtgxiso.autoledger.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xtgxiso.autoledger.AutoLedgerApp
import com.xtgxiso.autoledger.data.RuleEntity
import com.xtgxiso.autoledger.data.Transaction
import com.xtgxiso.autoledger.parse.ParseResult
import com.xtgxiso.autoledger.util.Dates
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

/**
 * 全部界面共用的一个 ViewModel。
 * 一个人用的小 App，不搞多 ViewModel 拆分，减少样板代码。
 */
class LedgerViewModel(private val app: AutoLedgerApp) : ViewModel() {

    private val repo = app.repository
    private val settings = app.settings

    /** 账单 / 报表页当前查看的月份（两页联动） */
    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    /** 首页固定看「本月」，不受账单页翻月影响，避免数字和标题对不上 */
    private val currentMonth: YearMonth = YearMonth.now()

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<Transaction>> = _month
        .flatMapLatest { m ->
            repo.observeMonth(
                from = Dates.monthStart(m),
                to = Dates.monthEnd(m),
                status = Transaction.CONFIRMED,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val pending: StateFlow<List<Transaction>> = _month
        .flatMapLatest { m ->
            repo.observeMonth(
                from = Dates.monthStart(m),
                to = Dates.monthEnd(m),
                status = Transaction.PENDING,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 首页大卡片用的本月数据 */
    val currentMonthTx: StateFlow<List<Transaction>> = repo.observeMonth(
        from = Dates.monthStart(currentMonth),
        to = Dates.monthEnd(currentMonth),
        status = Transaction.CONFIRMED,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 首页「近三日」：今天 + 昨天 + 前天，可跨月 */
    val recentTx: StateFlow<List<Transaction>> = repo.observeSince(
        from = Dates.daysAgoStart(2),
        status = Transaction.CONFIRMED,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 账单页 / 报表页的分页用：按需拿某个月的数据（不缓存成 StateFlow） */
    fun observeMonthOf(month: YearMonth, status: Int = Transaction.CONFIRMED): Flow<List<Transaction>> =
        repo.observeMonth(Dates.monthStart(month), Dates.monthEnd(month), status)

    val rules: StateFlow<List<RuleEntity>> = repo.observeRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val watchedPackages: StateFlow<Set<String>> = settings.watchedPackages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val autoConfirm: StateFlow<Boolean> = settings.autoConfirm
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val totalCount: StateFlow<Int> = repo.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // ——— 月份切换 ———
    fun shiftMonth(delta: Int) {
        val next = _month.value.plusMonths(delta.toLong())
        // 不允许翻到未来
        if (next <= YearMonth.now()) _month.value = next
    }

    fun setMonth(m: YearMonth) {
        if (m <= YearMonth.now()) _month.value = m
    }

    fun backToCurrentMonth() {
        _month.value = YearMonth.now()
    }

    /** 不允许翻到未来的月份（记账没意义） */
    fun canGoNext(): Boolean = _month.value < YearMonth.now()

    // ——— 账目操作 ———
    fun save(item: Transaction) = viewModelScope.launch { repo.save(item) }

    fun confirm(item: Transaction) = viewModelScope.launch { repo.confirm(item) }

    fun delete(item: Transaction) = viewModelScope.launch { repo.delete(item) }

    fun clearAll() = viewModelScope.launch { repo.clearAll() }

    fun deleteBefore(threshold: Long) = viewModelScope.launch { repo.deleteBefore(threshold) }

    suspend fun earliestTime(): Long? = repo.earliestTime()

    // ——— 设置 ———
    fun toggleWatch(pkg: String, on: Boolean) = viewModelScope.launch {
        settings.toggleWatched(pkg, on)
    }

    fun setAutoConfirm(value: Boolean) = viewModelScope.launch {
        settings.setAutoConfirm(value)
    }

    fun addRule(kind: String, keyword: String, category: String) = viewModelScope.launch {
        if (keyword.isBlank()) return@launch
        repo.addRule(RuleEntity(kind = kind, keyword = keyword.trim(), category = category))
    }

    fun deleteRule(id: Long) = viewModelScope.launch { repo.deleteRule(id) }

    /** 设置页「测试一句话」*/
    suspend fun testParse(title: String, text: String): ParseResult? = repo.testParse(title, text)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AutoLedgerApp
                LedgerViewModel(app)
            }
        }
    }
}
