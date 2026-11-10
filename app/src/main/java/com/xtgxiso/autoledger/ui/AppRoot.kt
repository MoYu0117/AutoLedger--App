package com.xtgxiso.autoledger.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xtgxiso.autoledger.BuildConfig
import com.xtgxiso.autoledger.data.Transaction
import com.xtgxiso.autoledger.ui.theme.Mint
import com.xtgxiso.autoledger.ui.theme.MintSoft
import com.xtgxiso.autoledger.ui.theme.PageBg
import com.xtgxiso.autoledger.ui.theme.TextSub
import com.xtgxiso.autoledger.update.ReleaseInfo
import com.xtgxiso.autoledger.update.UpdateChecker

/**
 * 应用骨架：底部 4 个 Tab（主页 / 账单 / 报表 / 我的）。
 * 编辑页以「整页盖上来」的方式显示，页面少，不引入 Navigation 组件。
 */
@Composable
fun AppRoot() {
    val vm: LedgerViewModel = viewModel(factory = LedgerViewModel.Factory)
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Transaction?>(null) }
    var creating by remember { mutableStateOf(false) }
    var ocrOpen by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<ReleaseInfo?>(null) }

    // 启动后联网检查一次：云端 versionCode 比本地大才弹「是否更新」
    LaunchedEffect(Unit) {
        val latest = UpdateChecker.fetchLatest()
        if (latest != null && latest.versionCode > BuildConfig.VERSION_CODE) {
            updateInfo = latest
        }
    }

    val update = updateInfo
    if (update != null) {
        UpdateDialog(
            info = update,
            onUpdate = { updateInfo = null },
            onLater = { updateInfo = null },
        )
    }

    if (ocrOpen) {
        OcrImportScreen(
            onDismiss = { ocrOpen = false },
            onSave = { txs ->
                vm.saveAll(txs)
                ocrOpen = false
            },
        )
        return
    }

    if (creating || editing != null) {
        EditScreen(
            initial = editing,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { item ->
                vm.save(item)
                creating = false
                editing = null
            },
            onDelete = { item ->
                vm.delete(item)
                creating = false
                editing = null
            },
        )
        return
    }

    Scaffold(
        containerColor = PageBg,
        // 自己处理状态栏内边距：顶部色带要画到状态栏下面去
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { BottomNav(selected = tab, onSelect = { tab = it }) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (tab) {
                0 -> HomeScreen(
                    vm = vm,
                    onAdd = { creating = true },
                    onOpen = { editing = it },
                    onOpenBills = { tab = 1 },
                    onOpenSettings = { tab = 3 },
                    onOpenOcr = { ocrOpen = true },
                )
                1 -> BillsScreen(vm = vm, onOpen = { editing = it })
                2 -> StatsScreen(vm = vm, onAdd = { creating = true }, onOpen = { editing = it })
                else -> SettingsScreen(vm = vm)
            }
        }
    }
}

@Composable
private fun BottomNav(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf(
        "主页" to Icons.Default.Home,
        "账单" to ReceiptIcon,
        "报表" to BarChartIcon,
        "我的" to Icons.Default.Person,
    )
    NavigationBar(
        containerColor = androidx.compose.ui.graphics.Color.White,
        tonalElevation = 0.dp,
    ) {
        tabs.forEachIndexed { index, (label, icon) ->
            NavigationBarItem(
                selected = selected == index,
                onClick = { onSelect(index) },
                icon = {
                    Icon(
                        icon,
                        contentDescription = label,
                        modifier = Modifier.size(22.dp),
                    )
                },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Mint,
                    selectedTextColor = Mint,
                    indicatorColor = MintSoft,
                    unselectedIconColor = TextSub,
                    unselectedTextColor = TextSub,
                ),
            )
        }
    }
}

/** 发现新版本弹窗：去更新（跳浏览器下载）/ 暂不 */
@Composable
private fun UpdateDialog(
    info: ReleaseInfo,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text("发现新版本 v${info.versionName}") },
        text = {
            Text(
                buildString {
                    append("当前版本 v${BuildConfig.VERSION_NAME}\n\n")
                    if (info.note.isNotBlank()) append(info.note)
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val url = info.downloadUrl.ifBlank { UpdateChecker.REPO_URL }
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                    onUpdate()
                },
            ) { Text("去更新", color = Mint) }
        },
        dismissButton = {
            TextButton(onClick = onLater) { Text("暂不") }
        },
    )
}
