package com.xtgxiso.autoledger.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xtgxiso.autoledger.data.Transaction
import com.xtgxiso.autoledger.ui.theme.Mint
import com.xtgxiso.autoledger.ui.theme.MintSoft
import com.xtgxiso.autoledger.ui.theme.PageBg
import com.xtgxiso.autoledger.ui.theme.TextSub

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
