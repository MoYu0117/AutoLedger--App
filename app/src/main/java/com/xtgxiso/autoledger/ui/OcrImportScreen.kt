package com.xtgxiso.autoledger.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xtgxiso.autoledger.data.Transaction
import com.xtgxiso.autoledger.ocr.OcrBill
import com.xtgxiso.autoledger.ocr.OcrBillParser
import com.xtgxiso.autoledger.ocr.OcrEngine
import com.xtgxiso.autoledger.ui.theme.ExpenseColor
import com.xtgxiso.autoledger.ui.theme.IncomeColor
import com.xtgxiso.autoledger.ui.theme.Mint
import com.xtgxiso.autoledger.ui.theme.MintSoft
import com.xtgxiso.autoledger.ui.theme.PageBg
import com.xtgxiso.autoledger.ui.theme.TextMain
import com.xtgxiso.autoledger.ui.theme.TextSub
import com.xtgxiso.autoledger.util.Money
import java.time.LocalDate
import java.time.LocalTime

/**
 * 识图记账：选一张账单截图 → 本地 OCR 识别 → 列出候选账单 → 可编辑每条 → 勾选后批量入账。
 */
@Composable
fun OcrImportScreen(
    onDismiss: () -> Unit,
    onSave: (List<Transaction>) -> Unit,
) {
    var pickedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var imagePath by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    // 用可编辑的列表状态，每条都能单独改
    val bills = remember { mutableStateListOf<EditableBill>() }
    var checked by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var error by remember { mutableStateOf<String?>(null) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    BackHandler(onBack = onDismiss)

    // 选择图片
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pickedUri = uri
            imagePath = copyToCache(uri)
            loading = true
            error = null
            bills.clear()
            checked = emptySet()
        }
    }

    // 拿到路径后跑 OCR
    LaunchedEffect(imagePath) {
        val path = imagePath ?: return@LaunchedEffect
        val lines = OcrEngine.recognize(path)
        val parsed = OcrBillParser.parse(lines)
        bills.clear()
        bills.addAll(parsed.map { EditableBill.from(it) })
        checked = bills.indices.toSet() // 默认全选
        loading = false
        if (bills.isEmpty()) {
            error = "没识别出账单。请换一张更清晰的截图，或手动记一笔。"
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        // 顶部栏
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
            Text(
                "识图记账",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextMain,
                modifier = Modifier.weight(1f),
            )
        }

        if (bills.isEmpty() && !loading && error == null) {
            // 未选图：引导选图
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("导入微信/支付宝的账单截图", style = MaterialTheme.typography.titleMedium, color = TextMain)
                Spacer(Modifier.height(8.dp))
                Text(
                    "App 会在你手机本地识别文字，不上传图片。识别后可勾选、可逐条编辑，再确认入账。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSub,
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { launcher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Mint),
                    shape = RoundedCornerShape(26.dp),
                ) {
                    Text("选择图片")
                }
            }
        } else if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("正在识别…", style = MaterialTheme.typography.bodyLarge, color = TextSub)
            }
        } else if (error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error!!, style = MaterialTheme.typography.bodyLarge, color = TextSub)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { launcher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Mint),
                        shape = RoundedCornerShape(26.dp),
                    ) { Text("换一张图") }
                }
            }
        } else {
            // 列出候选账单
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        "识别出 ${bills.size} 条，勾选要记入的（点条目可编辑）：",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextMain,
                    )
                }
                itemsIndexed(bills) { index, bill ->
                    OcrBillRow(
                        bill = bill,
                        checked = index in checked,
                        onToggle = {
                            checked = if (index in checked) checked - index else checked + index
                        },
                        onEdit = { editingIndex = index },
                    )
                }
            }

            // 底部确认栏
            Surface(color = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val selectedCount = checked.size
                    Text(
                        if (selectedCount == 0) "未勾选" else "已选 $selectedCount 条",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSub,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            val picked = bills.filterIndexed { i, _ -> i in checked }
                            val now = System.currentTimeMillis()
                            val txs = picked.map { b ->
                                val zone = java.time.ZoneId.systemDefault()
                                val dt = java.time.LocalDateTime.of(
                                    b.date ?: java.time.LocalDate.now(),
                                    b.time ?: LocalTime.of(12, 0),
                                )
                                Transaction(
                                    amountCents = b.amountCents,
                                    isExpense = b.isExpense,
                                    payee = b.payee,
                                    category = if (b.isExpense) "其他" else "其他收入",
                                    time = dt.atZone(zone).toInstant().toEpochMilli(),
                                    source = Transaction.SOURCE_MANUAL,
                                    rawText = b.rawLine,
                                )
                            }
                            onSave(txs)
                        },
                        enabled = checked.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Mint),
                        shape = RoundedCornerShape(26.dp),
                    ) {
                        Text("确认导入")
                    }
                }
            }
        }
    }

    // 编辑弹窗
    editingIndex?.let { index ->
        val bill = bills.getOrNull(index) ?: return@let
        EditOcrBillDialog(
            bill = bill,
            onDismiss = { editingIndex = null },
            onConfirm = { newPayee, newCents, newExpense, newDate, newTime ->
                bill.payee = newPayee
                bill.amountCents = newCents
                bill.isExpense = newExpense
                bill.date = newDate
                bill.time = newTime
                editingIndex = null
            },
        )
    }
}

/**
 * 可编辑的候选账目（内部状态，编辑时原地改字段触发重组）。
 */
private class EditableBill(
    var payee: String,
    var amountCents: Long,
    var isExpense: Boolean,
    var date: LocalDate?,
    var time: LocalTime?,
    val rawLine: String,
) {
    companion object {
        fun from(b: OcrBill) = EditableBill(b.payee, b.amountCents, b.isExpense, b.date, b.time, b.rawLine)
    }
}

@Composable
private fun OcrBillRow(
    bill: EditableBill,
    checked: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 勾选框
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (checked) Mint else MintSoft)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                if (checked) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.width(12.dp))

            // 中间：商户名 + 日期时间
            Column(Modifier.weight(1f)) {
                Text(
                    bill.payee,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMain,
                    maxLines = 1,
                )
                val dateStr = bill.date?.let { "${it.monthValue}月${it.dayOfMonth}日" } ?: ""
                val timeStr = bill.time?.let { String.format("%02d:%02d", it.hour, it.minute) } ?: ""
                Text(
                    if (timeStr.isNotEmpty()) "$dateStr $timeStr" else dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSub,
                )
            }

            // 金额
            Text(
                (if (bill.isExpense) "-" else "+") + Money.centsToYuan(bill.amountCents),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (bill.isExpense) ExpenseColor else IncomeColor,
            )

            Spacer(Modifier.width(8.dp))

            // 编辑按钮
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = TextSub, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun EditOcrBillDialog(
    bill: EditableBill,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Boolean, LocalDate?, LocalTime?) -> Unit,
) {
    var payee by remember { mutableStateOf(bill.payee) }
    var amountText by remember { mutableStateOf(Money.centsToYuan(bill.amountCents)) }
    var isExpense by remember { mutableStateOf(bill.isExpense) }
    var year by remember { mutableIntStateOf((bill.date ?: LocalDate.now()).year) }
    var month by remember { mutableIntStateOf((bill.date ?: LocalDate.now()).monthValue) }
    var day by remember { mutableIntStateOf((bill.date ?: LocalDate.now()).dayOfMonth) }
    var hour by remember { mutableIntStateOf((bill.time ?: LocalTime.of(12, 0)).hour) }
    var minute by remember { mutableIntStateOf((bill.time ?: LocalTime.of(12, 0)).minute) }
    var amountError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑这条账单") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = payee,
                    onValueChange = { payee = it },
                    label = { Text("商户 / 备注") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("金额（元）") },
                    singleLine = true,
                    isError = amountError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (amountError != null) {
                    Text(amountError!!, style = MaterialTheme.typography.bodySmall, color = ExpenseColor)
                }
                // 收支切换
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DirectionChip("支出", !isExpense, ExpenseColor) { isExpense = true }
                    DirectionChip("收入", isExpense, IncomeColor) { isExpense = false }
                }
                // 日期
                Text("日期", style = MaterialTheme.typography.labelMedium, color = TextSub)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WheelPicker(items = (2000..2100).toList(), selected = year, onSelected = { year = it }, modifier = Modifier.weight(1f), format = { "$it" })
                    Text("年", color = TextSub)
                    WheelPicker(items = (1..12).toList(), selected = month, onSelected = { month = it }, modifier = Modifier.weight(0.8f))
                    Text("月", color = TextSub)
                    WheelPicker(
                        items = (1..31).toList(), selected = day, onSelected = { day = it },
                        modifier = Modifier.weight(0.8f),
                        format = { it.toString() },
                    )
                    Text("日", color = TextSub)
                }
                // 时间
                Text("时间", style = MaterialTheme.typography.labelMedium, color = TextSub)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WheelPicker(items = (0..23).toList(), selected = hour, onSelected = { hour = it }, modifier = Modifier.weight(1f))
                    Text("时", color = TextSub)
                    WheelPicker(items = (0..59).toList(), selected = minute, onSelected = { minute = it }, modifier = Modifier.weight(1f))
                    Text("分", color = TextSub)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cents = Money.yuanToCents(amountText)
                if (cents == null || cents <= 0L) {
                    amountError = "请输入正确的金额"
                } else {
                    val d = runCatching { LocalDate.of(year, month, day.coerceIn(1, 28)) }.getOrNull() ?: LocalDate.now()
                    val t = LocalTime.of(hour, minute)
                    onConfirm(payee.ifBlank { "未识别商户" }, cents, isExpense, d, t)
                }
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun DirectionChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Surface(
        color = if (selected) color.copy(alpha = 0.15f) else MintSoft,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) color else TextSub,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

/** 把 content:// 的 Uri 拷到应用缓存目录，拿到文件绝对路径给 BitmapFactory 用 */
private fun copyToCache(uri: android.net.Uri): String? {
    return try {
        val context = com.xtgxiso.autoledger.AutoLedgerApp.instance
        val resolver = context.contentResolver
        val input = resolver.openInputStream(uri) ?: return null
        val file = java.io.File(context.cacheDir, "ocr_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { out -> input.copyTo(out) }
        input.close()
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}
