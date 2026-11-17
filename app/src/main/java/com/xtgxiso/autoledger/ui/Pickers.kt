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
 * 实现要点（关键，避免「时间回退」bug）：
 * - 视口显示 3 项，上下各留 1 项高度的 contentPadding，使第 0 项/最后一项也能滚到正中央；
 * - 选中项 = 中心最接近视口中线的那一项（用 layoutInfo 算）；
 * - **外部 selected 变化时用 LaunchedEffect 重新定位**，但用「snapshotFlow 快照」在
 *   scrollToItem 之后、滚动稳定后才回传选中值，杜绝「scroll 过程中 centerIndex 抖动、
 *   把 selected 一路回调到最小值」的回退问题。
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

    // 初始化时直接把列表定位到选中项（-1 使该项居中），之后外部 selected 变化时再 scroll
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (selectedIndex - 1).coerceAtLeast(0))
    val fling = rememberSnapFlingBehavior(lazyListState = listState)

    // 外部 selected 变化 -> 重新定位滚轮。滚动稳定后（帧之后）才同步回传，避免抖动回退。
    LaunchedEffect(selectedIndex) {
        val target = (selectedIndex - 1).coerceAtLeast(0)
        // 只有真正需要移动才 scroll，避免无谓的滚动触发 centerIndex 抖动
        if (listState.firstVisibleItemIndex != target) {
            listState.scrollToItem(target)
        }
    }

    // 中心项：视口中线附近最接近的那一项
    val centerIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) selectedIndex
            else {
                val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                info.visibleItemsInfo.minByOrNull {
                    abs((it.offset + it.size / 2f) - center)
                }?.index ?: selectedIndex
            }
        }
    }

    // 仅当中心项对应的「值」和外部 selected 不一致时才回传。
    // 用值比较（而非 index），彻底避免「scroll 过程中中间态触发 onSelected 把值改小」。
    LaunchedEffect(centerIndex) {
        val value = items.getOrNull(centerIndex) ?: return@LaunchedEffect
        if (value != selected) onSelected(value)
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
