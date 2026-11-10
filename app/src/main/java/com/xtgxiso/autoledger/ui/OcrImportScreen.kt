package com.xtgxiso.autoledger.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

/**
 * 识图记账：选一张账单截图 → 本地 OCR 识别 → 列出候选账单 → 勾选后批量入账。
 */
@Composable
fun OcrImportScreen(
    onDismiss: () -> Unit,
    onSave: (List<Transaction>) -> Unit,
) {
    var pickedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var imagePath by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var bills by remember { mutableStateOf<List<OcrBill>>(emptyList()) }
    var checked by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var error by remember { mutableStateOf<String?>(null) }

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
            bills = emptyList()
            checked = emptySet()
        }
    }

    // 拿到路径后跑 OCR
    LaunchedEffect(imagePath) {
        val path = imagePath ?: return@LaunchedEffect
        val lines = OcrEngine.recognize(path)
        bills = OcrBillParser.parse(lines)
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
                    "App 会在你手机本地识别文字，不上传图片。识别后可勾选要记入的账单。",
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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        "识别出 ${bills.size} 条，勾选要记入的：",
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
                                Transaction(
                                    amountCents = b.amountCents,
                                    isExpense = b.isExpense,
                                    payee = b.payee,
                                    category = if (b.isExpense) "其他" else "其他收入",
                                    time = b.date?.let { d ->
                                        // 日期默认当天 12:00，避免 0 点
                                        d.atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                                    } ?: now,
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
}

@Composable
private fun OcrBillRow(
    bill: OcrBill,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onToggle),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (checked) Mint else MintSoft),
                contentAlignment = Alignment.Center,
            ) {
                if (checked) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    bill.payee,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMain,
                    maxLines = 1,
                )
                if (bill.date != null) {
                    Text(
                        "${bill.date.monthValue}月${bill.date.dayOfMonth}日",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSub,
                    )
                }
            }
            Text(
                (if (bill.isExpense) "-" else "+") + Money.centsToYuan(bill.amountCents),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (bill.isExpense) ExpenseColor else IncomeColor,
            )
        }
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
