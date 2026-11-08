package com.xtgxiso.autoledger.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.xtgxiso.autoledger.util.Categories

/**
 * 配色：青绿 / 薄荷。
 *
 * 上一版主色是墨绿 #1F6F5C，整块铺在首页卡片上偏重、久看压抑，
 * 这一版整体提亮：顶部色带是浅青绿，主色降饱和，正文用近黑绿，保证对比度。
 */

// ——— 顶部色带（会延伸到状态栏下面）———
val BandTop = Color(0xFFB5D1BE)
val BandBottom = Color(0xFF9EC6AD)

// ——— 主色 ———
val Mint = Color(0xFF5FA98C)        // 主色：选中态、图标、强调
val MintSoft = Color(0xFFDDEDE4)    // 浅底：chip、进度条底
val MintChip = Color(0xFFBED8C6)    // 键帽、浅按钮

// ——— 收支 ——
val ExpenseColor = Color(0xFFE4574C) // 支出红
val IncomeColor = Color(0xFF3FA47C)  // 收入绿

// ——— 文字与背景 ———
val TextMain = Color(0xFF1E2A24)
val TextSub = Color(0xFF7C8C83)
val PageBg = Color(0xFFF3F7F4)
val HairLine = Color(0xFFEDF1EE)
/** 分段控件选中态（截图里是深墨蓝，不是黑） */
val NavyPill = Color(0xFF2F3B44)

private val LightColors = lightColorScheme(
    primary = Mint,
    onPrimary = Color.White,
    primaryContainer = MintSoft,
    onPrimaryContainer = Color(0xFF123326),
    secondary = Color(0xFF6E8B7C),
    secondaryContainer = MintSoft,
    onSecondaryContainer = Color(0xFF123326),
    background = PageBg,
    onBackground = TextMain,
    surface = Color.White,
    onSurface = TextMain,
    surfaceVariant = Color(0xFFF0F5F1),
    onSurfaceVariant = TextSub,
    outline = Color(0xFFD8E2DB),
    outlineVariant = HairLine,
    error = ExpenseColor,
)

/** 分类配色：低饱和，和青绿主色不打架 */
val CategoryPalette = listOf(
    Color(0xFF6FAF92),
    Color(0xFFE0A45C),
    Color(0xFF6D9BD1),
    Color(0xFFC97F92),
    Color(0xFF8E85C4),
    Color(0xFF58AFAF),
    Color(0xFFB0A46A),
    Color(0xFF9AA6A0),
)

fun colorForCategory(category: String): Color {
    val idx = Categories.expense.indexOf(category).takeIf { it >= 0 } ?: 0
    return CategoryPalette[idx % CategoryPalette.size]
}

@Composable
fun AutoLedgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography(),
        content = content,
    )
}
