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
import androidx.compose.foundation.lazy.items
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
 * 用在 时/分、年/月、删除月数 等地方，替代一下一下点箭头的步进器。
 *
 * 实现要点：
 * - LazyColumn 每项高度固定，视口显示 3 项，上下各留 1 项高度的 padding，
 *   这样第 0 项和最后一项也能滚到视口正中央；
 * - 选中项 = 中心最接近视口中线的那一项（用 layoutInfo 算，不依赖 firstVisibleIndex 的边界细节）；
 * - rememberSnapFlingBehavior 负责松手后的"吸附"手感。
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
    val listState = rememberLazyListState()
    val fling = rememberSnapFlingBehavior(lazyListState = listState)
    val selectedIndex = items.indexOf(selected).coerceIn(0, items.lastIndex)

    // 初始定位：滚动偏移 = selectedIndex * itemHeight 时该项正好居中
    LaunchedEffect(Unit) {
        if (selectedIndex > 0) listState.scrollToItem(selectedIndex - 1)
    }

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

    LaunchedEffect(centerIndex) {
        items.getOrNull(centerIndex)?.let { if (it != selected) onSelected(it) }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // 中心高亮条（底层，让选中项有"框住"的感觉）
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
            items(items.size) { i ->
                val isCenter = i == centerIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeightDp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        format(items[i]),
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
