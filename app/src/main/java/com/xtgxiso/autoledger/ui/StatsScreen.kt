package com.xtgxiso.autoledger.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xtgxiso.autoledger.data.Transaction
import com.xtgxiso.autoledger.ui.theme.ExpenseColor
import com.xtgxiso.autoledger.ui.theme.IncomeColor
import com.xtgxiso.autoledger.ui.theme.Mint
import com.xtgxiso.autoledger.ui.theme.MintSoft
import com.xtgxiso.autoledger.ui.theme.PageBg
import com.xtgxiso.autoledger.ui.theme.TextMain
import com.xtgxiso.autoledger.ui.theme.TextSub
import com.xtgxiso.autoledger.ui.theme.colorForCategory
import com.xtgxiso.autoledger.util.Categories
import com.xtgxiso.autoledger.util.Dates
import com.xtgxiso.autoledger.util.Money
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * 统计页，两种视图：
 * - 报表：环形占比 + 分类明细进度条，圆环左右滑动切月
 * - 日历：月历网格上直接看每天花了多少
 * 顶部月份：点击弹月份选择弹窗（去掉左右箭头），两视图共用。
 */
@Composable
fun StatsScreen(
    vm: LedgerViewModel,
    onAdd: () -> Unit,
    onOpen: (Transaction) -> Unit,
) {
    val vmMonth by vm.month.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { MAX_MONTHS })
    val scope = rememberCoroutineScope()
    var showMonthPicker by rememberSaveable { mutableStateOf(false) }

    val pagerMonth = remember(pagerState.currentPage) {
        YearMonth.now().minusMonths(pagerState.currentPage.toLong())
    }
    val displayMonth = if (mode == 0) pagerMonth else vmMonth

    Column(
        Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        MintHeader(bottomPadding = 14) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                SegmentedTabs(
                    options = listOf("报表", "日历"),
                    selected = mode,
                    onSelect = { mode = it },
                    onBand = true,
                )
            }
            Spacer(Modifier.height(10.dp))
            MonthSelector(
                month = displayMonth,
                onClick = { showMonthPicker = true },
            )
        }

        if (mode == 0) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                val m = remember(page) { YearMonth.now().minusMonths(page.toLong()) }
                val list by remember(m) { vm.observeMonthOf(m) }
                    .collectAsStateWithLifecycle(emptyList())
                ReportBody(month = m, list = list)
            }
        } else {
            val list by vm.transactions.collectAsStateWithLifecycle()
            CalendarBody(month = vmMonth, list = list, onAdd = onAdd, onOpen = onOpen)
        }
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            initial = displayMonth,
            onDismiss = { showMonthPicker = false },
            onConfirm = { ym ->
                if (mode == 0) {
                    val target = Dates.monthsBetween(ym, YearMonth.now()).toInt()
                        .coerceIn(0, MAX_MONTHS - 1)
                    scope.launch { pagerState.scrollToPage(target) }
                } else {
                    vm.setMonth(ym)
                }
                showMonthPicker = false
            },
        )
    }
}

// ——————————————————————— 报表 ———————————————————————

private data class Slice(
    val name: String,
    val iconCategory: String,
    val cents: Long,
    val count: Int,
    val color: Color,
)

@Composable
private fun ReportBody(month: YearMonth, list: List<Transaction>) {
    val expense = remember(list) { list.filter { it.isExpense }.sumOf { it.amountCents } }
    val income = remember(list) { list.filterNot { it.isExpense }.sumOf { it.amountCents } }
    val total = expense

    // 按分类聚合；超过 6 类就把尾巴合成「其他」，避免环形图太碎
    val slices = remember(list) {
        val grouped = list.filter { it.isExpense }
            .groupBy { it.category }
            .map { (cat, items) ->
                Slice(cat, cat, items.sumOf { it.amountCents }, items.size, colorForCategory(cat))
            }
            .sortedByDescending { it.cents }
        val head = grouped.take(6)
        val tail = grouped.drop(6)
        if (tail.isEmpty()) head else head + Slice(
            name = "其他",
            iconCategory = "其他",
            cents = tail.sumOf { it.cents },
            count = tail.sumOf { it.count },
            color = Color(0xFF9AA6A0),
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (total == 0L && income == 0L) {
            item { EmptyState(granted = true, title = "这个月还没有数据") }
            return@LazyColumn
        }

        item {
            WhiteCard {
                Row {
                    TopStat("月支出", "¥" + Money.centsToYuan(expense), ExpenseColor, Modifier.weight(1f))
                    TopStat("月收入", "¥" + Money.centsToYuan(income), IncomeColor, Modifier.weight(1f))
                    TopStat(
                        "结余",
                        (if (income - expense < 0) "-¥" + Money.centsToYuan(expense - income) else "¥" + Money.centsToYuan(income - expense)),
                        TextMain,
                        Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(18.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Donut(
                        slices = slices,
                        total = total,
                        centerLabel = "总支出",
                        centerValue = "¥" + Money.centsToYuan(total),
                    )
                }
            }
        }

        item {
            WhiteCard(padding = 0) {
                slices.forEachIndexed { index, slice ->
                    SliceRow(slice = slice, total = total)
                    if (index != slices.lastIndex) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 62.dp, end = 16.dp)
                                .height(1.dp)
                                .background(Color(0xFFF0F3F0))
                        )
                    }
                }
                if (slices.isEmpty()) {
                    Text(
                        "本月还没有支出",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSub,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TopStat(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(valueColor.copy(alpha = 0.5f))
            )
            Spacer(Modifier.width(5.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSub)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
    }
}

/**
 * 环形占比图。
 * 每个扇区从弧的中点先「垂直」（沿径向）伸出一小段，再弯折到左右两侧标分类名；
 * 左右两列的标签按顺序做垂直去重叠，避免分类一多文字叠在一起。
 */
@Composable
private fun Donut(slices: List<Slice>, total: Long, centerLabel: String, centerValue: String) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, color = TextMain, fontWeight = FontWeight.Medium)
    val centerLabelStyle = TextStyle(fontSize = 12.sp, color = TextSub)
    val centerValueStyle = TextStyle(fontSize = 18.sp, color = TextMain, fontWeight = FontWeight.Bold)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.18f),
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val stroke = 22.dp.toPx()
        val ringOuter = size.minDimension * 0.30f
        val arcTopLeft = Offset(cx - ringOuter, cy - ringOuter)
        val arcSize = Size(ringOuter * 2f, ringOuter * 2f)

        val gap = 8.dp.toPx()          // 垂直（径向）伸出的长度
        val leaderLen = 16.dp.toPx()   // 弯折后到标签的水平距离
        val textGap = 4.dp.toPx()
        val minLabelGap = 18.dp.toPx() // 同列标签的最小垂直间距

        val n = slices.size
        val dxArr = FloatArray(n)
        val dyArr = FloatArray(n)
        val rawY = FloatArray(n)
        val sweepArr = FloatArray(n)

        var start = -90f
        slices.forEachIndexed { i, slice ->
            val sweep = if (total > 0) slice.cents * 360f / total else 0f
            sweepArr[i] = sweep
            if (sweep > 0f) {
                drawArc(
                    color = slice.color,
                    startAngle = start,
                    sweepAngle = (sweep - 1.2f).coerceAtLeast(0.4f),
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
                val midDeg = start + sweep / 2f
                val rad = Math.toRadians(midDeg.toDouble())
                val dx = cos(rad).toFloat()
                val dy = sin(rad).toFloat()
                dxArr[i] = dx
                dyArr[i] = dy
                rawY[i] = cy + dy * ringOuter
            }
            start += sweep
        }

        // 标签垂直去重叠：左列、右列各自按原始 Y 排序后向下推
        val labelY = FloatArray(n)
        fun placeColumn(indices: List<Int>) {
            var lastY = Float.NEGATIVE_INFINITY
            for (i in indices) {
                val y = maxOf(rawY[i], lastY + minLabelGap).coerceIn(0f, size.height)
                labelY[i] = y
                lastY = y
            }
        }
        placeColumn(slices.indices.filter { sweepArr[it] > 0f && dxArr[it] < 0f }.sortedBy { rawY[it] })
        placeColumn(slices.indices.filter { sweepArr[it] > 0f && dxArr[it] >= 0f }.sortedBy { rawY[it] })

        // 画引线 + 标签
        slices.forEachIndexed { i, slice ->
            if (sweepArr[i] <= 0f) return@forEachIndexed
            val dx = dxArr[i]
            val dy = dyArr[i]
            val isLeft = dx < 0f

            val edgeX = cx + dx * ringOuter
            val edgeY = cy + dy * ringOuter
            val elbowX = cx + dx * (ringOuter + gap)
            val elbowY = cy + dy * (ringOuter + gap)
            val y = labelY[i]
            val labelColumnX = if (isLeft) cx - ringOuter - gap - leaderLen
                               else cx + ringOuter + gap + leaderLen

            val lineColor = slice.color.copy(alpha = 0.55f)
            // ① 垂直出来：沿径向伸出一小段
            drawLine(lineColor, Offset(edgeX, edgeY), Offset(elbowX, elbowY), 1.2.dp.toPx())
            // ② 弯折：折向标签所在列
            drawLine(lineColor, Offset(elbowX, elbowY), Offset(labelColumnX, y), 1.2.dp.toPx())

            val layout = textMeasurer.measure(slice.name, labelStyle)
            val textX = if (isLeft) labelColumnX - textGap - layout.size.width else labelColumnX + textGap
            val textY = y - layout.size.height / 2f
            drawText(layout, topLeft = Offset(textX, textY))
        }

        // 环中心：总支出
        val cl = textMeasurer.measure(centerLabel, centerLabelStyle)
        val cv = textMeasurer.measure(centerValue, centerValueStyle)
        val gapH = 4.dp.toPx()
        val blockH = cl.size.height + gapH + cv.size.height
        var topY = cy - blockH / 2f
        drawText(cl, topLeft = Offset(cx - cl.size.width / 2f, topY))
        topY += cl.size.height + gapH
        drawText(cv, topLeft = Offset(cx - cv.size.width / 2f, topY))
    }
}

@Composable
private fun SliceRow(slice: Slice, total: Long) {
    val percent = if (total > 0) slice.cents.toFloat() / total else 0f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(slice.iconCategory, size = 36)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    slice.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "${(percent * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSub,
                )
            }
            Spacer(Modifier.height(6.dp))
            ThinProgress(percent = percent, color = slice.color)
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "¥" + Money.centsToYuan(slice.cents),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = ExpenseColor,
            )
            Text("${slice.count}笔", style = MaterialTheme.typography.labelSmall, color = TextSub)
        }
    }
}

// ——————————————————————— 日历 ———————————————————————

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.CalendarBody(
    month: YearMonth,
    list: List<Transaction>,
    onAdd: () -> Unit,
    onOpen: (Transaction) -> Unit,
) {
    var metric by rememberSaveable { mutableIntStateOf(0) } // 0 支出 1 收入 2 结余
    val defaultDay = remember(month) {
        if (Dates.isThisMonth(month)) LocalDate.now().dayOfMonth else 1
    }
    var selectedDay by rememberSaveable(month) { mutableIntStateOf(defaultDay) }

    val expenseByDay = remember(list) {
        list.filter { it.isExpense }
            .groupBy { Dates.toLocalDate(it.time).dayOfMonth }
            .mapValues { entry -> entry.value.sumOf { it.amountCents } }
    }
    val incomeByDay = remember(list) {
        list.filterNot { it.isExpense }
            .groupBy { Dates.toLocalDate(it.time).dayOfMonth }
            .mapValues { entry -> entry.value.sumOf { it.amountCents } }
    }
    val monthExpense = remember(list) { list.filter { it.isExpense }.sumOf { it.amountCents } }
    val monthIncome = remember(list) { list.filterNot { it.isExpense }.sumOf { it.amountCents } }
    val dayCount = remember(month) { Dates.dayCountForAverage(month) }
    val avg = if (dayCount > 0) monthExpense / dayCount else 0L

    val length = month.lengthOfMonth()
    val lead = Dates.mondayIndex(month.atDay(1).dayOfWeek)
    val cells: List<Int?> = List(lead) { null } + (1..length).toList()

    val selectedItems = remember(list, selectedDay) {
        list.filter { Dates.toLocalDate(it.time).dayOfMonth == selectedDay }
    }
    val selectedExpense = selectedItems.filter { it.isExpense }.sumOf { it.amountCents }

    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            WhiteCard(padding = 12) {
                SegmentedTabs(
                    options = listOf("支出", "收入", "结余"),
                    selected = metric,
                    onSelect = { metric = it },
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth()) {
                    Dates.weekTitles.forEach {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSub,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            Box(Modifier.weight(1f)) {
                                if (day == null) {
                                    Spacer(Modifier.height(52.dp))
                                } else {
                                    DayCell(
                                        day = day,
                                        label = cellLabel(day, metric, expenseByDay, incomeByDay),
                                        labelColor = when (metric) {
                                            1 -> IncomeColor
                                            2 -> if ((incomeByDay[day] ?: 0L) - (expenseByDay[day] ?: 0L) < 0) ExpenseColor else IncomeColor
                                            else -> ExpenseColor
                                        },
                                        selected = day == selectedDay,
                                        onClick = { selectedDay = day },
                                    )
                                }
                            }
                        }
                        // 最后一行补空，保证高度一致
                        repeat(7 - week.size) { Box(Modifier.weight(1f)) {} }
                    }
                }
            }
        }

        item {
            WhiteCard {
                Row {
                    Text(
                        "月支出：¥" + Money.centsToYuan(monthExpense),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ExpenseColor,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "日均支出：¥" + Money.centsToYuan(avg),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMain,
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${month.monthValue}月${selectedDay}日账单",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "支出 ¥" + Money.centsToYuan(selectedExpense),
                    style = MaterialTheme.typography.bodySmall,
                    color = ExpenseColor,
                )
                Spacer(Modifier.weight(1f))
                PillButton(
                    text = "记一笔",
                    onClick = onAdd,
                    background = Mint,
                    contentColor = Color.White,
                )
            }
        }

        if (selectedItems.isEmpty()) {
            item {
                Text(
                    "这天没有记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSub,
                    modifier = Modifier.padding(vertical = 20.dp, horizontal = 4.dp),
                )
            }
        } else {
            item {
                WhiteCard(padding = 0) {
                    selectedItems.forEachIndexed { index, item ->
                        BillRow(item = item, onClick = { onOpen(item) })
                        if (index != selectedItems.lastIndex) {
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

private fun cellLabel(
    day: Int,
    metric: Int,
    expenseByDay: Map<Int, Long>,
    incomeByDay: Map<Int, Long>,
): String {
    val exp = expenseByDay[day] ?: 0L
    val inc = incomeByDay[day] ?: 0L
    return when (metric) {
        1 -> if (inc > 0) Money.centsToYuanShort(inc) else ""
        2 -> {
            val bal = inc - exp
            if (bal == 0L) "" else (if (bal < 0) "-" else "") + Money.centsToYuanShort(abs(bal))
        }
        else -> if (exp > 0) Money.centsToYuanShort(exp) else ""
    }
}

@Composable
private fun DayCell(
    day: Int,
    label: String,
    labelColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) Mint else MintSoft.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .padding(2.dp)
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) Color.White else TextMain,
            )
            if (label.isNotEmpty()) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) Color.White else labelColor,
                    maxLines = 1,
                )
            }
        }
    }
}
