package com.xtgxiso.autoledger.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xtgxiso.autoledger.data.Transaction
import com.xtgxiso.autoledger.ui.theme.ExpenseColor
import com.xtgxiso.autoledger.ui.theme.IncomeColor
import com.xtgxiso.autoledger.ui.theme.Mint
import com.xtgxiso.autoledger.ui.theme.MintSoft
import com.xtgxiso.autoledger.ui.theme.PageBg
import com.xtgxiso.autoledger.ui.theme.TextMain
import com.xtgxiso.autoledger.ui.theme.TextSub
import com.xtgxiso.autoledger.util.Categories
import com.xtgxiso.autoledger.util.Dates
import com.xtgxiso.autoledger.util.Money
import java.time.YearMonth
import kotlinx.coroutines.launch

/**
 * 账单页：月度汇总（含按日柱状图）+ 按日期分组的明细。
 * 月份切换：点击顶部月份弹窗选，或左右滑动整页切换；两处联动、顶部文字同步。
 */
@Composable
fun BillsScreen(
    vm: LedgerViewModel,
    onOpen: (Transaction) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { MAX_MONTHS })
    val scope = rememberCoroutineScope()
    val currentMonth = remember(pagerState.currentPage) {
        YearMonth.now().minusMonths(pagerState.currentPage.toLong())
    }

    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var filterOpen by rememberSaveable { mutableStateOf(false) }
    var picked by remember { mutableStateOf(emptySet<String>()) }
    var showMonthPicker by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        MintHeader(bottomPadding = 16) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "账单",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    searchOpen = !searchOpen
                    if (!searchOpen) query = ""
                }) {
                    Icon(
                        if (searchOpen) Icons.Default.Clear else Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = TextMain,
                    )
                }
            }
            MonthSelector(
                month = currentMonth,
                onClick = { showMonthPicker = true },
                trailing = {
                    PillButton(
                        text = if (picked.isEmpty()) "筛选" else "筛选 ${picked.size}",
                        onClick = { filterOpen = !filterOpen },
                        background = if (picked.isEmpty()) Color.White.copy(alpha = 0.5f) else Mint,
                        contentColor = if (picked.isEmpty()) TextMain else Color.White,
                    )
                },
            )
            if (searchOpen) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("搜商户 / 分类 / 备注") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (filterOpen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(
                    selected = picked.isEmpty(),
                    onClick = { picked = emptySet() },
                    label = { Text("全部") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MintSoft,
                        selectedLabelColor = Mint,
                    ),
                )
                Categories.expense.forEach { cat ->
                    FilterChip(
                        selected = picked.contains(cat),
                        onClick = {
                            picked = if (picked.contains(cat)) picked - cat else picked + cat
                        },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MintSoft,
                            selectedLabelColor = Mint,
                        ),
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            reverseLayout = true,
            modifier = Modifier.weight(1f),
        ) { page ->
            val month = remember(page) { YearMonth.now().minusMonths(page.toLong()) }
            BillsMonthPage(
                vm = vm,
                month = month,
                query = query,
                picked = picked,
                onOpen = onOpen,
            )
        }
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            initial = currentMonth,
            onDismiss = { showMonthPicker = false },
            onConfirm = { ym ->
                val target = Dates.monthsBetween(ym, YearMonth.now()).toInt()
                    .coerceIn(0, MAX_MONTHS - 1)
                scope.launch { pagerState.scrollToPage(target) }
                showMonthPicker = false
            },
        )
    }
}

@Composable
private fun BillsMonthPage(
    vm: LedgerViewModel,
    month: YearMonth,
    query: String,
    picked: Set<String>,
    onOpen: (Transaction) -> Unit,
) {
    val list by remember(month) { vm.observeMonthOf(month) }
        .collectAsStateWithLifecycle(emptyList())
    val pending by remember(month) { vm.observeMonthOf(month, Transaction.PENDING) }
        .collectAsStateWithLifecycle(emptyList())

    val expense = remember(list) { list.filter { it.isExpense }.sumOf { it.amountCents } }
    val income = remember(list) { list.filterNot { it.isExpense }.sumOf { it.amountCents } }
    val balance = income - expense
    val dayCount = remember(month) { Dates.dayCountForAverage(month) }
    val avg = if (dayCount > 0) expense / dayCount else 0L

    val dayValues = remember(list, month) {
        val len = month.lengthOfMonth()
        LongArray(len).also { arr ->
            list.filter { it.isExpense }.forEach { item ->
                val d = Dates.toLocalDate(item.time).dayOfMonth
                if (d in 1..len) arr[d - 1] += item.amountCents
            }
        }
    }

    val filtered = remember(list, query, picked) {
        list.filter { item ->
            val okCat = picked.isEmpty() || picked.contains(item.category)
            val okQuery = query.isBlank() ||
                item.payee.contains(query, true) ||
                item.category.contains(query, true) ||
                item.note.contains(query, true)
            okCat && okQuery
        }
    }
    val days = remember(filtered) { groupByDay(filtered) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            WhiteCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DotStat(
                        dotColor = ExpenseColor,
                        label = "月支出",
                        value = "¥" + Money.centsToYuan(expense),
                        valueColor = ExpenseColor,
                        modifier = Modifier.weight(1f),
                    )
                    DotStat(
                        dotColor = IncomeColor,
                        label = "月收入",
                        value = "¥" + Money.centsToYuan(income),
                        valueColor = IncomeColor,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(14.dp))
                DailyBars(days = month.lengthOfMonth(), values = dayValues)
            }
        }

        item { Spacer(Modifier.height(10.dp)) }

        item {
            WhiteCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "月结余：" + (if (balance < 0) "-¥" + Money.centsToYuan(-balance) else "¥" + Money.centsToYuan(balance)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (balance < 0) ExpenseColor else IncomeColor,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "日均支出：¥" + Money.centsToYuan(avg),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextMain,
                    )
                }
            }
        }

        if (pending.isNotEmpty()) {
            item {
                Column(Modifier.padding(top = 12.dp)) {
                    Text(
                        "待确认 · ${pending.size} 条",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMain,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                    )
                    WhiteCard(padding = 0) {
                        pending.forEachIndexed { index, item ->
                            BillRow(
                                item = item,
                                onClick = { onOpen(item) },
                                trailing = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { vm.confirm(item) },
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Mint,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Text(
                                            " 入账",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Mint,
                                        )
                                    }
                                },
                            )
                            if (index != pending.lastIndex) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(start = 64.dp)
                                        .height(1.dp)
                                        .background(Color(0xFFF0F3F0))
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "账单明细",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (days.isEmpty()) {
            item {
                EmptyState(
                    granted = true,
                    title = if (query.isNotBlank() || picked.isNotEmpty()) "没有符合条件的记录" else "这个月还没有记录",
                )
            }
        } else {
            days.forEach { (dayStart, items) ->
                item(key = "bh$dayStart") {
                    DayHeader(
                        dateMillis = dayStart,
                        dayExpense = items.filter { it.isExpense }.sumOf { it.amountCents },
                        dayIncome = items.filterNot { it.isExpense }.sumOf { it.amountCents },
                    )
                }
                item(key = "bc$dayStart") {
                    WhiteCard(padding = 0) {
                        items.forEachIndexed { index, item ->
                            BillRow(item = item, onClick = { onOpen(item) })
                            if (index != items.lastIndex) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(start = 64.dp)
                                        .height(1.dp)
                                        .background(Color(0xFFF0F3F0))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DotStat(
    dotColor: Color,
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = 0.55f))
            )
            Spacer(Modifier.width(5.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSub)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
    }
}

/** 按日柱状图：一个月每天一根柱子，空数据不画 */
@Composable
private fun DailyBars(days: Int, values: LongArray) {
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
        ) {
            if (days <= 0) return@Canvas
            val slot = size.width / days
            val barWidth = (slot * 0.45f).coerceAtLeast(1.5f)
            values.forEachIndexed { index, value ->
                if (value <= 0L) return@forEachIndexed
                val ratio = value.toFloat() / max
                val h = (ratio * (size.height - 4.dp.toPx())).coerceAtLeast(2.dp.toPx())
                drawRoundRect(
                    color = Mint,
                    topLeft = Offset(index * slot + (slot - barWidth) / 2f, size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("1日", "8日", "15日", "22日", "29日").forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = TextSub)
            }
        }
    }
}
