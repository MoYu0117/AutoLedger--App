package com.xtgxiso.autoledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
 * 竖直滚轮选择器：手指滑动选值，松手自动吸附到最近的一项。
 *
 * 定位与回传设计（避免「时间回退」bug 的关键）：
 * - 视口显示 3 项，上下各留 1 项高度的 contentPadding，使第 0 项 / 最后一项也能滚到正中央；
 * - 用 `rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex - 1)` 初始化，
 *   让选中项一开始就居中（第 selectedIndex-1 项在视口顶部 -> 第 selectedIndex 项居中）；
 * - 中心项用 **viewport 相对坐标** 计算：`item.offset` 本来就是相对视口顶部的偏移，
 *   视口中心 = (viewportEndOffset - viewportStartOffset) / 2，两者相减才是正确距离。
 *   之前误用 (viewportStartOffset + viewportEndOffset)/2（content 坐标）和 viewport 坐标的
 *   item.offset 相减，导致中心项永远算错，选中值一路回退。
 * - 只在「滚动停止」时回传一次，杜绝滚动过程中的中间项被误回传。
 *
 * 这里做成「非受控」：外部 `selected` 仅作初始值，之后完全由滚轮自身滚动驱动。
 * 年月/时分各滚轮之间没有联动，非受控最简单也最不容易出错。
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
    val selectedIndex = items.indexOf(selected).coerceIn(0, items.lastIndex)

    // 初始化定位：让选中项居中（-1 项在视口顶部）
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedIndex - 1).coerceAtLeast(0)
    )
    val fling = rememberSnapFlingBehavior(lazyListState = listState)

    // 中心项：视口相对坐标正确计算
    val centerIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) selectedIndex
            else {
                val viewportCenter = (info.viewportEndOffset - info.viewportStartOffset) / 2f
                info.visibleItemsInfo.minByOrNull {
                    abs((it.offset + it.size / 2f) - viewportCenter)
                }?.index ?: selectedIndex
            }
        }
    }

    // 只在滚动停止后回传一次；程序不主动 scrollToItem，因此 isScrollInProgress 只由用户手势触发
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { inProgress ->
                if (!inProgress) {
                    items.getOrNull(centerIndex)?.let { v ->
                        if (v != selected) onSelected(v)
                    }
                }
            }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // 中心高亮条
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
                .height(itemHeightDp * 3),
            contentPadding = PaddingValues(vertical = itemHeightDp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemsIndexed(items) { i, value ->
                val isCenter = i == centerIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeightDp),
                    contentAlignment = Alignment.Center,
                ) {
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
                    items = (minYear..max.year).toList(),
                    selected = year,
                    onSelected = { year = it },
                    modifier = Modifier.weight(1f),
                    format = { it.toString() },
                )
                Spacer(Modifier.width(6.dp))
                Text("年", style = MaterialTheme.typography.titleMedium, color = TextMain)
                Spacer(Modifier.width(12.dp))
                WheelPicker(
                    items = (1..12).toList(),
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
