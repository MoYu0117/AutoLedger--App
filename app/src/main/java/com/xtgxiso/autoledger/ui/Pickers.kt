package com.xtgxiso.autoledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xtgxiso.autoledger.ui.theme.MintSoft
import com.xtgxiso.autoledger.ui.theme.TextMain
import com.xtgxiso.autoledger.ui.theme.TextSub
import java.time.YearMonth
import kotlin.math.abs

/**
 * 竖直滚轮选择器。
 *
 * 实现要点（修复「选中项与高亮条错位一行」与「时间回退」的关键）：
 * - 不在 LazyColumn 上使用 contentPadding，改为在数据首尾各塞一个透明占位项（null）。
 *   这样首尾的真实项也能滚到正中央，且 offset 坐标系简单清晰、无隐式偏移。
 * - 初始定位：`rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)`，
 *   让第 selectedIndex 个 display 项顶部对齐视口第 1 行，于是真实选中项（display 下标
 *   = selectedIndex + half）正好落在第 2 行（视口中心）。
 * - 中心项判定：视口中心 = viewportSize.height / 2（相对视口顶部），item.offset 同样
 *   相对视口顶部，两者相减即可正确找到中心项（之前误用 viewportStartOffset 导致偏一行）。
 * - 非受控：外部 selected 仅作初始值；滚动停止后（含慢速拖动）精确吸附并回传一次。
 */
@Composable
fun WheelPicker(
    items: List<Int>,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Int = 44,
    format: (Int) -> String = { it.toString().padStart(2, '0') },
) {
    if (items.isEmpty()) return
    val itemHeightDp = itemHeight.dp
    val viewportItemCount = 3
    val half = viewportItemCount / 2

    val selectedIndex = items.indexOf(selected).coerceIn(0, items.lastIndex)

    // 首尾各加 half 个占位项（null），保证首尾真实项也能居中
    val displayItems: List<Int?> = remember(items) {
        buildList {
            repeat(half) { add(null) }
            items.forEach { add(it) }
            repeat(half) { add(null) }
        }
    }

    // 初始定位：displayItems[selectedIndex] 顶部在第 1 行，
    // 真实选中项在 display 下标 selectedIndex + half（第 2 行，视口中心）
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val fling = rememberSnapFlingBehavior(listState)

    // 中心项（display 下标）
    val centerDisplayIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) selectedIndex + half
            else {
                val center = info.viewportSize.height / 2f
                info.visibleItemsInfo.minByOrNull {
                    abs(it.offset + it.size / 2f - center)
                }?.index ?: (selectedIndex + half)
            }
        }
    }

    // 滚动停止后：精确吸附到中心项，并回传真实值
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { inProgress ->
                if (!inProgress) {
                    val realIndex = centerDisplayIndex - half
                    if (realIndex in items.indices) {
                        // 慢速拖动松手时可能未对齐，手动吸附一次
                        if (listState.firstVisibleItemIndex != realIndex ||
                            listState.firstVisibleItemScrollOffset != 0
                        ) {
                            listState.scrollToItem(realIndex, 0)
                        }
                        items.getOrNull(realIndex)?.let { v ->
                            if (v != selected) onSelected(v)
                        }
                    }
                }
            }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .height(itemHeightDp)
                .clip(RoundedCornerShape(12.dp))
                .background(MintSoft.copy(alpha = 0.6f)),
        )
        LazyColumn(
            state = listState,
            flingBehavior = fling,
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeightDp * viewportItemCount),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemsIndexed(displayItems) { i, value ->
                val isCenter = i == centerDisplayIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeightDp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (value != null) {
                        Text(
                            format(value),
                            fontSize = if (isCenter) 22.sp else 16.sp,
                            fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCenter) TextMain else TextSub,
                        )
                    }
                }
            }
        }
    }
}

/** 月份选择弹窗：年 + 月两个滚轮 */
@Composable
fun MonthPickerDialog(
    initial: YearMonth,
    minYear: Int = 2020,
    onDismiss: () -> Unit,
    onConfirm: (YearMonth) -> Unit,
) {
    val max = YearMonth.now()
    var year by remember { mutableIntStateOf(initial.year.coerceIn(minYear, max.year)) }
    var month by remember { mutableIntStateOf(initial.monthValue) }

    val years = remember(minYear, max) { (minYear..max.year).toList() }
    val months = remember { (1..12).toList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择月份") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WheelPicker(
                    items = years,
                    selected = year,
                    onSelected = { year = it },
                    modifier = Modifier.weight(1f),
                    format = { it.toString() },
                )
                Spacer(Modifier.width(6.dp))
                Text("年", style = MaterialTheme.typography.titleMedium, color = TextMain)
                Spacer(Modifier.width(12.dp))
                WheelPicker(
                    items = months,
                    selected = month,
                    onSelected = { month = it },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
                Text("月", style = MaterialTheme.typography.titleMedium, color = TextMain)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(YearMonth.of(year, month).coerceAtMost(max)) }) {
                Text("确定")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
