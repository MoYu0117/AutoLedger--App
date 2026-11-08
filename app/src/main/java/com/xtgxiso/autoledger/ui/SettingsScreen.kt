package com.xtgxiso.autoledger.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xtgxiso.autoledger.BuildConfig
import com.xtgxiso.autoledger.data.AppSettings
import com.xtgxiso.autoledger.data.RuleEntity
import com.xtgxiso.autoledger.ui.theme.ExpenseColor
import com.xtgxiso.autoledger.ui.theme.IncomeColor
import com.xtgxiso.autoledger.ui.theme.Mint
import com.xtgxiso.autoledger.ui.theme.MintSoft
import com.xtgxiso.autoledger.ui.theme.PageBg
import com.xtgxiso.autoledger.ui.theme.TextMain
import com.xtgxiso.autoledger.ui.theme.TextSub
import com.xtgxiso.autoledger.util.AppInfo
import com.xtgxiso.autoledger.util.Categories
import com.xtgxiso.autoledger.util.Dates
import com.xtgxiso.autoledger.util.Money
import com.xtgxiso.autoledger.util.loadLaunchableApps
import com.xtgxiso.autoledger.util.openAppDetailSettings
import com.xtgxiso.autoledger.util.openBatteryOptimizationSettings
import com.xtgxiso.autoledger.util.openNotificationListenerSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth

/**
 * 「我的」页面。
 *
 * 性能说明（上一版卡顿的原因与修法）：
 * 1. 上一版整页是一个 verticalScroll 的 Column，38 条识别规则 + 可能的 200 多个已安装应用
 *    会被一次性组合布局 → 进页面就卡。这里改成 LazyColumn，只组合屏幕内的条目。
 * 2. 识别规则默认折叠，展开后放进一个固定最大高度的内层 LazyColumn，
 *    这样规则再多也不会把整页撑爆。
 * 3. 已安装应用列表以前是进页面就扫描（PackageManager 遍历全机应用），
 *    现在推迟到用户真的展开「监听其他应用」时才扫，且只扫一次。
 */
@Composable
fun SettingsScreen(vm: LedgerViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val watched by vm.watchedPackages.collectAsStateWithLifecycle()
    val autoConfirm by vm.autoConfirm.collectAsStateWithLifecycle()
    val rules by vm.rules.collectAsStateWithLifecycle()
    val totalCount by vm.totalCount.collectAsStateWithLifecycle()
    val granted = rememberNotificationAccess()

    var showOthers by rememberSaveable { mutableStateOf(false) }
    var rulesExpanded by rememberSaveable { mutableStateOf(false) }
    var showAddRule by rememberSaveable { mutableStateOf(false) }
    var showTest by rememberSaveable { mutableStateOf(false) }
    var showClear by rememberSaveable { mutableStateOf(false) }
    var showDeleteBefore by rememberSaveable { mutableStateOf(false) }
    var showDeleteBeforeConfirm by rememberSaveable { mutableStateOf(false) }
    var deleteMonths by rememberSaveable { mutableIntStateOf(3) }
    var deleteThreshold by remember { mutableStateOf(0L) }

    // 只在展开时扫描一次，之后复用
    var apps by remember { mutableStateOf<List<AppInfo>?>(null) }
    LaunchedEffect(showOthers) {
        if (showOthers && apps == null) {
            apps = withContext(Dispatchers.IO) { loadLaunchableApps(context) }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MintHeader(bottomPadding = 18) {
                Text(
                    "我的",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "所有数据只存在这台手机上，不联网、不上传",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMain.copy(alpha = 0.7f),
                )
            }
        }

        // ——— 自动记账 ———
        item {
            SettingsCard {
                SectionTitle("自动记账")
                ClickRow(
                    title = "通知读取权限",
                    subtitle = if (granted) "已开启，可以正常收通知" else "未开启，App 收不到任何支付通知",
                    value = if (granted) "已开启" else "去开启",
                    valueColor = if (granted) IncomeColor else ExpenseColor,
                    onClick = { openNotificationListenerSettings(context) },
                )
                Divider()
                SwitchRow(
                    title = "全自动入账",
                    subtitle = if (autoConfirm) "识别到就直接记账，事后可改" else "先进「待确认」，你点一下才入账",
                    checked = autoConfirm,
                    onCheckedChange = { vm.setAutoConfirm(it) },
                )
                Divider()
                SwitchRow(
                    title = "微信",
                    subtitle = "com.tencent.mm",
                    checked = watched.contains(AppSettings.PKG_WECHAT),
                    onCheckedChange = { vm.toggleWatch(AppSettings.PKG_WECHAT, it) },
                )
                SwitchRow(
                    title = "支付宝",
                    subtitle = "com.eg.android.AlipayGphone",
                    checked = watched.contains(AppSettings.PKG_ALIPAY),
                    onCheckedChange = { vm.toggleWatch(AppSettings.PKG_ALIPAY, it) },
                )
                TextButton(
                    onClick = { showOthers = !showOthers },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                ) {
                    Text(
                        if (showOthers) "收起其他应用" else "监听其他应用",
                        style = MaterialTheme.typography.labelLarge,
                        color = Mint,
                    )
                }
                if (showOthers) {
                    val list = apps
                    when {
                        list == null -> Text("正在读取应用列表…", style = MaterialTheme.typography.bodySmall, color = TextSub)
                        list.isEmpty() -> Text(
                            "没读到应用列表，可能需要在系统设置里允许「读取应用列表」权限。",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSub,
                        )
                        else -> {
                            val others = remember(list) {
                                list.filter {
                                    it.pkg != AppSettings.PKG_WECHAT && it.pkg != AppSettings.PKG_ALIPAY
                                }
                            }
                            // 同样用固定高度的内层 LazyColumn：装了几百个 App 也不会一次性全组合
                            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                                items(others, key = { it.pkg }) { app ->
                                    SwitchRow(
                                        title = app.label,
                                        subtitle = app.pkg,
                                        checked = watched.contains(app.pkg),
                                        onCheckedChange = { vm.toggleWatch(app.pkg, it) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ——— 识别规则（默认折叠，解决卡顿）———
        item {
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("识别规则", modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { rulesExpanded = !rulesExpanded },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(
                            "${rules.size} 条 · " + if (rulesExpanded) "收起" else "展开",
                            style = MaterialTheme.typography.labelLarge,
                            color = Mint,
                        )
                    }
                }
                Text(
                    "微信/支付宝改了通知文案时，在这里加关键词就能修好，不用重新打包。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSub,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallMintButton("＋ 新增规则") { showAddRule = true }
                    SmallMintButton("测试一句话") { showTest = true }
                }
                if (rulesExpanded) {
                    Spacer(Modifier.height(8.dp))
                    if (rules.isEmpty()) {
                        Text("还没有规则", style = MaterialTheme.typography.bodySmall, color = TextSub)
                    } else {
                        // 内层独立 LazyColumn + 最大高度：规则再多也只组合可见部分
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(rules, key = { it.id }) { rule ->
                                RuleRow(rule = rule, onDelete = { vm.deleteRule(rule.id) })
                            }
                        }
                    }
                }
            }
        }

        // ——— 保活 ———
        item {
            SettingsCard {
                SectionTitle("后台保活（重要）")
                Text(
                    "国产系统会自动杀后台。这三项不设置，会出现「用着用着就不记账了」。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSub,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                ClickRow("① 电池优化白名单", "设为「不限制 / 允许后台运行」", "去设置") {
                    openBatteryOptimizationSettings(context)
                }
                ClickRow("② 自启动", "应用详情里打开「自启动 / 关联启动」", "应用详情") {
                    openAppDetailSettings(context)
                }
                ClickRow("③ 通知权限", "确认系统没把本应用的通知关掉", "应用详情") {
                    openAppDetailSettings(context)
                }
            }
        }

        // ——— 数据 ———
        item {
            SettingsCard {
                SectionTitle("数据")
                ClickRow("本地记录总数", "全部存在这台手机上，不联网、不上传", "$totalCount 条") {}
                Divider()
                ClickRow("清空全部记录", "删除后不可恢复，请谨慎操作", "清空", valueColor = ExpenseColor) {
                    showClear = true
                }
                Divider()
                ClickRow("删除 N 月前数据", "清理很久以前的账单，月数可选", "去清理") {
                    showDeleteBefore = true
                }
            }
        }

        item {
            SettingsCard {
                SectionTitle("关于")
                ClickRow("版本", "个人自用版", "v" + BuildConfig.VERSION_NAME) {}
                Text(
                    "仅限个人自用，请勿分发给他人，也不可上架应用商店。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSub,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }

    if (showAddRule) {
        AddRuleDialog(
            onDismiss = { showAddRule = false },
            onConfirm = { kind, keyword, category ->
                vm.addRule(kind, keyword, category)
                showAddRule = false
            },
        )
    }

    if (showTest) {
        TestParseDialog(
            onDismiss = { showTest = false },
            onTest = { title, text -> vm.testParse(title, text) },
        )
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("清空全部记录？") },
            text = { Text("这会删除所有账目，无法恢复。建议先在相册里截个图留底。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearAll()
                    showClear = false
                }) { Text("确认清空", color = ExpenseColor) }
            },
            dismissButton = { TextButton(onClick = { showClear = false }) { Text("取消") } },
        )
    }

    if (showDeleteBefore) {
        DeleteBeforeDialog(
            months = deleteMonths,
            onMonthsChange = { deleteMonths = it },
            onDismiss = { showDeleteBefore = false },
            onConfirm = { months ->
                scope.launch {
                    val threshold = Dates.monthStart(YearMonth.now().minusMonths(months.toLong()))
                    val earliest = vm.earliestTime()
                    if (earliest == null || earliest >= threshold) {
                        android.widget.Toast.makeText(
                            context,
                            "还没有记录 ${months} 月前的数据，无法删除",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                        showDeleteBefore = false
                    } else {
                        deleteThreshold = threshold
                        showDeleteBefore = false
                        showDeleteBeforeConfirm = true
                    }
                }
            },
        )
    }

    if (showDeleteBeforeConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteBeforeConfirm = false },
            title = { Text("确认删除？") },
            text = {
                Text(
                    "将删除 ${deleteMonths} 个月前（即 ${Dates.monthLabel(YearMonth.now().minusMonths(deleteMonths.toLong()))} 之前）的所有记录，删除后不可恢复。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteBefore(deleteThreshold)
                    android.widget.Toast.makeText(context, "已删除", android.widget.Toast.LENGTH_SHORT).show()
                    showDeleteBeforeConfirm = false
                }) { Text("确认删除", color = ExpenseColor) }
            },
            dismissButton = { TextButton(onClick = { showDeleteBeforeConfirm = false }) { Text("取消") } },
        )
    }
}

// —————————————————————— 通用小部件 ——————————————————————

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = TextMain,
        modifier = modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun Divider() {
    HorizontalDivider(Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SmallMintButton(text: String, onClick: () -> Unit) {
    Surface(
        color = MintSoft,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = Mint,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun ClickRow(
    title: String,
    subtitle: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = Mint,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextMain)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSub)
        }
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextMain)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSub)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                checkedTrackColor = Mint,
            ),
        )
    }
}

@Composable
private fun RuleRow(rule: RuleEntity, onDelete: () -> Unit) {
    val kindLabel = when (rule.kind) {
        RuleEntity.KIND_PAYEE -> "商户"
        RuleEntity.KIND_INCOME -> "收入"
        else -> "支出"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(color = MintSoft, shape = RoundedCornerShape(6.dp)) {
            Text(
                kindLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Mint,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            rule.keyword + if (rule.category.isNotBlank()) " → ${rule.category}" else "",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMain,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "删除规则",
                tint = TextSub,
                modifier = Modifier.height(18.dp),
            )
        }
    }
}

@Composable
private fun AddRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (kind: String, keyword: String, category: String) -> Unit,
) {
    var kind by remember { mutableStateOf(RuleEntity.KIND_PAYEE) }
    var keyword by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Categories.expense.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增识别规则") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        RuleEntity.KIND_PAYEE to "商户→分类",
                        RuleEntity.KIND_INCOME to "收入词",
                        RuleEntity.KIND_EXPENSE to "支出词",
                    ).forEach { (k, label) ->
                        FilterChip(
                            selected = kind == k,
                            onClick = { kind = k },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MintSoft,
                                selectedLabelColor = Mint,
                            ),
                        )
                    }
                }
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("关键词") },
                    placeholder = { Text("如：星巴克 / 退款") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (kind == RuleEntity.KIND_PAYEE) {
                    Text("命中后归到哪个分类", style = MaterialTheme.typography.bodySmall)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Categories.expense.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MintSoft,
                                    selectedLabelColor = Mint,
                                ),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(kind, keyword, if (kind == RuleEntity.KIND_PAYEE) category else "") },
                enabled = keyword.isNotBlank(),
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun TestParseDialog(
    onDismiss: () -> Unit,
    onTest: suspend (String, String) -> com.xtgxiso.autoledger.parse.ParseResult?,
) {
    var title by remember { mutableStateOf("微信支付") }
    var text by remember { mutableStateOf("微信支付凭证 已支付¥25.80 收款方：星巴克咖啡") }
    var result by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("测试一条通知") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "把真机上收到的通知文字原样粘进来，看看能不能正确识别。",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("通知标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("通知正文") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (loading) Text("解析中…", style = MaterialTheme.typography.bodySmall)
                result?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                loading = true
                scope.launch {
                    val r = onTest(title, text)
                    result = if (r == null) {
                        "❌ 没识别出金额，这条会被忽略。可以试着补一条规则。"
                    } else {
                        "✅ 金额 ¥${Money.centsToYuan(r.amountCents)}\n" +
                            "方向 ${if (r.isExpense) "支出" else "收入"}\n" +
                            "商户 ${r.payee}\n" +
                            "分类 ${r.category}\n" +
                            "命中写法 ${r.matchedPattern}"
                    }
                    loading = false
                }
            }) { Text("解析") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

@Composable
private fun DeleteBeforeDialog(
    months: Int,
    onMonthsChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除多久前的数据？") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "删除多少个月之前的全部账单",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSub,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WheelPicker(
                        items = (1..36).toList(),
                        selected = months,
                        onSelected = onMonthsChange,
                        modifier = Modifier.width(130.dp),
                    )
                    Text(
                        " 个月前",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextMain,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(months) }) { Text("删除") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
