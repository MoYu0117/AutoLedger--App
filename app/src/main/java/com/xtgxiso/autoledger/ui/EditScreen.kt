package com.xtgxiso.autoledger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * 记一笔 / 编辑一条。
 *
 * 布局照着常用记账 App 来：上方分类图标网格，底部自绘数字键盘（不弹系统输入法）。
 * 键盘上的「-」「+」是切换支出/收入，和主流记账 App 的手感一致。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    initial: Transaction?,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
) {
    var amount by remember {
        mutableStateOf(initial?.let { Money.centsToYuan(it.amountCents) }?.takeIf { it != "0.00" } ?: "")
    }
    var isExpense by remember { mutableStateOf(initial?.isExpense ?: true) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var category by remember {
        mutableStateOf(initial?.category ?: Categories.expense.first())
    }
    var timeMillis by remember { mutableStateOf(initial?.time ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    BackHandler(onBack = onDismiss)

    val cents = Money.yuanToCents(amount) ?: 0L
    val valid = cents > 0L
    val categories = if (isExpense) Categories.expense else Categories.income
    val accent = if (isExpense) ExpenseColor else IncomeColor

    fun pressKey(key: String) {
        when (key) {
            "⌫" -> if (amount.isNotEmpty()) amount = amount.dropLast(1)
            "." -> if (!amount.contains('.')) amount = if (amount.isEmpty()) "0." else "$amount."
            "-" -> {
                isExpense = true
                if (category !in Categories.expense) category = Categories.expense.first()
            }
            "+" -> {
                isExpense = false
                if (category !in Categories.income) category = Categories.income.first()
            }
            else -> {
                if (amount.contains('.')) {
                    if (amount.substringAfter('.').length < 2) amount += key
                } else if (amount == "0") {
                    amount = key
                } else if (amount.length < 8) {
                    amount += key
                }
            }
        }
    }

    fun doSave(keepOpen: Boolean) {
        if (!valid) return
        // 不允许记未来账：时间超出当前时刻就压回到现在
        val safeTime = minOf(timeMillis, System.currentTimeMillis())
        onSave(
            Transaction(
                id = initial?.id ?: 0L,
                amountCents = cents,
                isExpense = isExpense,
                payee = initial?.payee ?: note.ifBlank { category },
                category = category,
                time = safeTime,
                source = initial?.source ?: Transaction.SOURCE_MANUAL,
                rawText = initial?.rawText.orEmpty(),
                note = note,
                status = Transaction.CONFIRMED,
            )
        )
        if (keepOpen) {
            amount = ""
            note = ""
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        // ——— 顶部：只保留 支出 / 收入 ———
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = TextMain)
            }
            SegmentedTabs(
                options = listOf("支出", "收入"),
                selected = if (isExpense) 0 else 1,
                onSelect = { index ->
                    isExpense = index == 0
                    category = if (isExpense) Categories.expense.first() else Categories.income.first()
                },
            )
            Spacer(Modifier.weight(1f))
            if (initial != null) {
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = TextSub)
                }
            }
        }

        // ——— 分类网格 ———
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
        ) {
            Spacer(Modifier.height(6.dp))
            categories.chunked(5).forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    rowItems.forEach { cat ->
                        CategoryCell(
                            category = cat,
                            selected = category == cat,
                            onClick = { category = cat },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(5 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // ——— 备注 + 日期 + 金额 ———
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (note.isEmpty()) {
                            Text(
                                initial?.payee?.takeIf { it.isNotBlank() && it != "未识别商户" }
                                    ?: "请输入备注信息（最多150字）",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSub,
                            )
                        }
                        BasicTextField(
                            value = note,
                            onValueChange = { if (it.length <= 150) note = it },
                            textStyle = TextStyle(
                                color = TextMain,
                                fontSize = 15.sp,
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MintSoft,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { showDatePicker = true },
                    ) {
                        Text(
                            Dates.monthDay(timeMillis),
                            style = MaterialTheme.typography.labelLarge,
                            color = Mint,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = MintSoft,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { showTimePicker = true },
                    ) {
                        Text(
                            Dates.hm(timeMillis),
                            style = MaterialTheme.typography.labelLarge,
                            color = Mint,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "¥ " + if (amount.isEmpty()) "0.00" else amount,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                }
            }
        }

        // ——— 数字键盘 ———
        Surface(
            color = MintSoft,
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Column(
                Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                KeyRow {
                    Key("1", Modifier.weight(1f)) { pressKey("1") }
                    Key("2", Modifier.weight(1f)) { pressKey("2") }
                    Key("3", Modifier.weight(1f)) { pressKey("3") }
                    Key("⌫", Modifier.weight(1f), soft = true) { pressKey("⌫") }
                }
                KeyRow {
                    Key("4", Modifier.weight(1f)) { pressKey("4") }
                    Key("5", Modifier.weight(1f)) { pressKey("5") }
                    Key("6", Modifier.weight(1f)) { pressKey("6") }
                    Key("−", Modifier.weight(1f), soft = true) { pressKey("-") }
                }
                KeyRow {
                    Key("7", Modifier.weight(1f)) { pressKey("7") }
                    Key("8", Modifier.weight(1f)) { pressKey("8") }
                    Key("9", Modifier.weight(1f)) { pressKey("9") }
                    Key("+", Modifier.weight(1f), soft = true) { pressKey("+") }
                }
                KeyRow {
                    Key("再记", Modifier.weight(1f), small = true) { doSave(keepOpen = true) }
                    Key("0", Modifier.weight(1f)) { pressKey("0") }
                    Key(".", Modifier.weight(1f)) { pressKey(".") }
                    Key(
                        "保存",
                        Modifier.weight(1f),
                        primary = true,
                        enabled = valid,
                    ) { doSave(keepOpen = false) }
                }
            }
        }
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = timeMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return !date.isAfter(LocalDate.now())
                }
                override fun isSelectableYear(year: Int): Boolean = true
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { picked ->
                        timeMillis = mergeDate(timeMillis, picked)
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        val current = remember(timeMillis) {
            Instant.ofEpochMilli(timeMillis).atZone(ZoneId.systemDefault())
        }
        var hour by remember(current) { mutableIntStateOf(current.hour) }
        var minute by remember(current) { mutableIntStateOf(current.minute) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("选择时间") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WheelPicker(
                        items = (0..23).toList(),
                        selected = hour,
                        onSelected = { hour = it },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        ":",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    WheelPicker(
                        items = (0..59).toList(),
                        selected = minute,
                        onSelected = { minute = it },
                        modifier = Modifier.weight(1f),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val date = Instant.ofEpochMilli(timeMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                    timeMillis = date.atTime(hour, minute)
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("取消") } },
        )
    }

    if (showDeleteConfirm && initial != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除这条记录？") },
            text = { Text("删除后不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(initial)
                }) { Text("删除", color = ExpenseColor) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") } },
        )
    }
}

/** DatePicker 回传的是 UTC 零点，这里只取日期、保留原来的时分 */
private fun mergeDate(oldMillis: Long, pickedUtcMillis: Long): Long {
    val date = Instant.ofEpochMilli(pickedUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    val oldTime = Instant.ofEpochMilli(oldMillis).atZone(ZoneId.systemDefault()).toLocalTime()
    return date.atTime(oldTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

@Composable
private fun CategoryCell(
    category: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) Mint.copy(alpha = 0.22f) else Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text(Categories.emoji(category), fontSize = 22.sp)
        }
        Spacer(Modifier.height(5.dp))
        Text(
            category,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Mint else TextMain,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun KeyRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        content = content,
    )
}

@Composable
private fun Key(
    label: String,
    modifier: Modifier,
    soft: Boolean = false,
    primary: Boolean = false,
    small: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val bg = when {
        primary -> if (enabled) Mint else Mint.copy(alpha = 0.35f)
        soft -> Color.White.copy(alpha = 0.38f)
        else -> Color.White.copy(alpha = 0.72f)
    }
    val fg = if (primary) Color.White else TextMain
    Surface(
        color = bg,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                fontSize = if (small) 15.sp else 22.sp,
                fontWeight = if (primary || small) FontWeight.Medium else FontWeight.Normal,
                color = fg,
            )
        }
    }
}
