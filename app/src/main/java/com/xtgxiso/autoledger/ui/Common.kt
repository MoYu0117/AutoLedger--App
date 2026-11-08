package com.xtgxiso.autoledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.xtgxiso.autoledger.data.Transaction
import com.xtgxiso.autoledger.ui.theme.BandBottom
import com.xtgxiso.autoledger.ui.theme.BandTop
import com.xtgxiso.autoledger.ui.theme.ExpenseColor
import com.xtgxiso.autoledger.ui.theme.IncomeColor
import com.xtgxiso.autoledger.ui.theme.Mint
import com.xtgxiso.autoledger.ui.theme.MintSoft
import com.xtgxiso.autoledger.ui.theme.NavyPill
import com.xtgxiso.autoledger.ui.theme.TextMain
import com.xtgxiso.autoledger.ui.theme.TextSub
import com.xtgxiso.autoledger.ui.theme.colorForCategory
import com.xtgxiso.autoledger.util.Categories
import com.xtgxiso.autoledger.util.Dates
import com.xtgxiso.autoledger.util.Money
import com.xtgxiso.autoledger.util.isNotificationAccessGranted

/**
 * 「通知使用权」实时状态。
 * 用生命周期监听而不是 LaunchedEffect：用户去系统设置勾完权限返回时，这里会自动刷新。
 */
@Composable
fun rememberNotificationAccess(): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(isNotificationAccessGranted(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = isNotificationAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return granted
}

/**
 * 顶部青绿色带。
 * 会一直画到状态栏下面（界面开了 edge-to-edge），内部内容自动避开状态栏。
 */
@Composable
fun MintHeader(
    modifier: Modifier = Modifier,
    bottomPadding: Int = 18,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(BandTop, BandBottom))),
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 6.dp, bottom = bottomPadding.dp),
            content = content,
        )
    }
}

/** 白卡片 */
@Composable
fun WhiteCard(
    modifier: Modifier = Modifier,
    padding: Int = 16,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(padding.dp), content = content)
    }
}

/** 月份切换行（放在色带上，深色字） */
@Composable
fun MonthNavRow(
    month: java.time.YearMonth,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onBackToNow: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "上个月", tint = TextMain)
        }
        Text(
            Dates.monthLabel(month),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextMain,
            modifier = Modifier
                .clickable(enabled = onBackToNow != null) { onBackToNow?.invoke() }
                .padding(horizontal = 4.dp),
        )
        IconButton(onClick = onNext, enabled = canGoNext, modifier = Modifier.size(30.dp)) {
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = "下个月",
                tint = if (canGoNext) TextMain else TextMain.copy(alpha = 0.3f),
            )
        }
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** 从 2020-01 到现在的最大可翻页月数（账单页/报表页的 HorizontalPager 用） */
const val MAX_MONTHS = 240

/** 月份选择器：点击弹月份选择弹窗，替代原来的左右箭头 */
@Composable
fun MonthSelector(
    month: java.time.YearMonth,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                Dates.monthLabel(month),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextMain,
            )
            Spacer(Modifier.width(4.dp))
            Text("▾", style = MaterialTheme.typography.bodyMedium, color = TextMain)
        }
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** 小胶囊按钮（资产 / 筛选 / 按时间 …） */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    background: Color = MintSoft,
    contentColor: Color = Mint,
) {
    Surface(
        color = background,
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** 分段控件：选中项是深墨蓝胶囊，和截图一致 */
@Composable
fun SegmentedTabs(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onBand: Boolean = false,
) {
    Surface(
        color = if (onBand) Color.White.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50),
        modifier = modifier,
    ) {
        Row(Modifier.padding(3.dp)) {
            options.forEachIndexed { index, label ->
                val active = index == selected
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (active) NavyPill else Color.Transparent)
                        .clickable { onSelect(index) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                        color = if (active) Color.White else TextSub,
                    )
                }
            }
        }
    }
}

/** 分类图标：圆形底 + emoji（不额外引入图片资源） */
@Composable
fun CategoryBadge(category: String, size: Int = 38) {
    val color = colorForCategory(category)
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(Categories.emoji(category), fontSize = (size * 0.45f).sp)
    }
}

/** 一条账目（卡片内的一行，行间用发丝线分隔） */
@Composable
fun BillRow(
    item: Transaction,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(item.category)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                item.payee,
                style = MaterialTheme.typography.bodyLarge,
                color = TextMain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildString {
                    append(item.category)
                    append(" · ")
                    append(Dates.hm(item.time))
                    if (item.note.isNotBlank()) {
                        append(" · ")
                        append(item.note)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                Money.signed(item.amountCents, item.isExpense),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (item.isExpense) ExpenseColor else IncomeColor,
            )
            if (item.source != Transaction.SOURCE_MANUAL) {
                AutoBadge()
            }
            trailing?.invoke()
        }
    }
}

/** 「自动记账」小标签：只给通知监听自动入账的记录打上，手动记的没有 */
@Composable
private fun AutoBadge() {
    Surface(color = MintSoft, shape = RoundedCornerShape(50)) {
        Text(
            "自动记账",
            style = MaterialTheme.typography.labelSmall,
            color = Mint,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

/** 日期分组头：11月5日 今天 ………… 支:569.00 */
@Composable
fun DayHeader(dateMillis: Long, dayExpense: Long, dayIncome: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            Dates.monthDay(dateMillis),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextMain,
        )
        Spacer(Modifier.width(6.dp))
        Text(Dates.dayHint(dateMillis), style = MaterialTheme.typography.labelSmall, color = TextSub)
        Spacer(Modifier.weight(1f))
        if (dayExpense > 0) {
            Text("支:${Money.centsToYuan(dayExpense)}", style = MaterialTheme.typography.labelMedium, color = TextSub)
        }
        if (dayIncome > 0) {
            Spacer(Modifier.width(8.dp))
            Text("收:${Money.centsToYuan(dayIncome)}", style = MaterialTheme.typography.labelMedium, color = TextSub)
        }
    }
}

/** 细进度条 */
@Composable
fun ThinProgress(percent: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(50))
            .background(MintSoft),
    ) {
        Box(
            Modifier
                .fillMaxWidth(percent.coerceIn(0f, 1f))
                .height(7.dp)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = TextMain,
        modifier = modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
    )
}

/** 顶部权限未开时的醒目提示 */
@Composable
fun PermissionBanner(onClick: () -> Unit) {
    Surface(
        color = Color(0xFFFFF3E2),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "还没开通知读取权限",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8A5300),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "不开权限，App 收不到支付通知，也就不会自动记账。点这里去开启 →",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8A5300),
            )
        }
    }
}

/** 空列表占位 */
@Composable
fun EmptyState(
    granted: Boolean,
    padding: PaddingValues = PaddingValues(vertical = 40.dp),
    title: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            title ?: if (granted) "还没有记录" else "还没有任何记录",
            style = MaterialTheme.typography.titleSmall,
            color = TextSub,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (granted) "用微信 / 支付宝付一笔钱，几秒后这里会自动出现一条" else "先开启通知读取权限，或者手动记一笔",
            style = MaterialTheme.typography.bodySmall,
            color = TextSub,
        )
    }
}

/** 按日期分组，返回 (当天0点, 该天账目)；已按时间倒序 */
fun groupByDay(list: List<Transaction>): List<Pair<Long, List<Transaction>>> =
    list.groupBy { Dates.startOfDay(it.time) }.toList().sortedByDescending { it.first }
