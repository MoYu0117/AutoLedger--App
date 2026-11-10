package com.xtgxiso.autoledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xtgxiso.autoledger.data.Transaction
import com.xtgxiso.autoledger.ui.theme.ExpenseColor
import com.xtgxiso.autoledger.ui.theme.IncomeColor
import com.xtgxiso.autoledger.ui.theme.Mint
import com.xtgxiso.autoledger.ui.theme.MintChip
import com.xtgxiso.autoledger.ui.theme.MintSoft
import com.xtgxiso.autoledger.ui.theme.PageBg
import com.xtgxiso.autoledger.ui.theme.TextMain
import com.xtgxiso.autoledger.ui.theme.TextSub
import com.xtgxiso.autoledger.util.Money
import com.xtgxiso.autoledger.util.openNotificationListenerSettings
import kotlin.math.abs

/**
 * 主页：顶部大卡片（本月支出 / 收入 / 结余 三态可切），下面只放近三日账单。
 * 完整明细在「账单」页，这里刻意不做全量列表。
 */
@Composable
fun HomeScreen(
    vm: LedgerViewModel,
    onAdd: () -> Unit,
    onOpen: (Transaction) -> Unit,
    onOpenBills: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenOcr: () -> Unit,
) {
    val monthTx by vm.currentMonthTx.collectAsStateWithLifecycle()
    val recent by vm.recentTx.collectAsStateWithLifecycle()
    val pending by vm.pending.collectAsStateWithLifecycle()
    val granted = rememberNotificationAccess()
    val context = LocalContext.current

    val expense = remember(monthTx) { monthTx.filter { it.isExpense }.sumOf { it.amountCents } }
    val income = remember(monthTx) { monthTx.filterNot { it.isExpense }.sumOf { it.amountCents } }
    val days = remember(recent) { groupByDay(recent) }

    var page by rememberSaveable { mutableIntStateOf(0) }
    var hidden by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        MintHeader(bottomPadding = 34) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "本月账单",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                    modifier = Modifier.weight(1f),
                )
                PillButton(
                    text = "识图",
                    onClick = onOpenOcr,
                    background = Color.White.copy(alpha = 0.5f),
                    contentColor = TextMain,
                )
                Spacer(Modifier.width(8.dp))
                PillButton(
                    text = "设置",
                    onClick = onOpenSettings,
                    background = Color.White.copy(alpha = 0.5f),
                    contentColor = TextMain,
                )
            }
            Spacer(Modifier.height(12.dp))
            HeroCard(
                expense = expense,
                income = income,
                page = page,
                onCyclePage = { page = (page + 1) % 3 },
                hidden = hidden,
                onToggleHidden = { hidden = !hidden },
                onAdd = onAdd,
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (!granted) {
                item { PermissionBanner(onClick = { openNotificationListenerSettings(context) }) }
                item { Spacer(Modifier.height(8.dp)) }
            }

            if (pending.isNotEmpty()) {
                item {
                    Surface(
                        color = Color(0xFFFFF3E2),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "有 ${pending.size} 条等待确认",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF8A5300),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "去账单页处理",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF8A5300),
                                modifier = Modifier.clickable { onOpenBills() },
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "近三日账单",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMain,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onOpenBills) {
                        Text("全部明细 ›", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            if (days.isEmpty()) {
                item { EmptyState(granted = granted, title = "近三日还没有记录") }
            } else {
                days.forEach { (dayStart, items) ->
                    item(key = "h$dayStart") {
                        DayHeader(
                            dateMillis = dayStart,
                            dayExpense = items.filter { it.isExpense }.sumOf { it.amountCents },
                            dayIncome = items.filterNot { it.isExpense }.sumOf { it.amountCents },
                        )
                    }
                    item(key = "c$dayStart") {
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
}

/** 顶部大卡片：三个状态（支出 / 收入 / 结余），点箭头循环切换 */
@Composable
private fun HeroCard(
    expense: Long,
    income: Long,
    page: Int,
    onCyclePage: () -> Unit,
    hidden: Boolean,
    onToggleHidden: () -> Unit,
    onAdd: () -> Unit,
) {
    val balance = income - expense

    // 三个口径「支出 / 收入 / 结余」轮流占据主位，其余两个固定排在下方两栏。
    // 点箭头只是调换顺序，金额一律用黑色。
    val mainLabel: String
    val mainValue: Long
    val leftLabel: String
    val leftValue: Long
    val rightLabel: String
    val rightValue: Long
    when (page) {
        1 -> {
            mainLabel = "本月收入"; mainValue = income
            leftLabel = "月结余"; leftValue = balance
            rightLabel = "本月支出"; rightValue = expense
        }
        2 -> {
            mainLabel = "月结余"; mainValue = balance
            leftLabel = "本月支出"; leftValue = expense
            rightLabel = "本月收入"; rightValue = income
        }
        else -> {
            mainLabel = "本月支出"; mainValue = expense
            leftLabel = "本月收入"; leftValue = income
            rightLabel = "月结余"; rightValue = balance
        }
    }

    // 标签用语义色，金额数字统一黑色
    val mainLabelColor = when (mainLabel) {
        "本月支出" -> ExpenseColor
        "本月收入" -> IncomeColor
        else -> if (balance < 0) ExpenseColor else if (balance > 0) IncomeColor else TextSub
    }

    fun fmt(v: Long): String {
        if (hidden) return "••••"
        val a = Money.centsToYuan(abs(v))
        return if (v < 0) "-$a" else a
    }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    mainLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = mainLabelColor,
                )
                Spacer(Modifier.width(8.dp))
                // 切换按钮：点一下把三个口径轮流调换顺序
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(ExpenseColor.copy(alpha = 0.12f))
                        .clickable(onClick = onCyclePage),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "切换支出/收入/结余",
                        tint = ExpenseColor,
                        modifier = Modifier.size(15.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                PillButton(
                    text = if (hidden) "显示" else "隐藏",
                    onClick = onToggleHidden,
                    background = MintSoft,
                    contentColor = Mint,
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "¥",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMain,
                    modifier = Modifier.padding(bottom = 5.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (hidden) "••••" else {
                        val v = Money.centsToYuan(abs(mainValue))
                        if (mainValue < 0) "-$v" else v
                    },
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        leftLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSub,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        fmt(leftValue),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMain,
                    )
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        rightLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSub,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        fmt(rightValue),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMain,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MintChip,
                    contentColor = TextMain,
                ),
            ) {
                Text("记一笔", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
