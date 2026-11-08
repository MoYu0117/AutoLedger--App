package com.xtgxiso.autoledger.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 自绘图标。
 * material-icons-core 里没有图表类图标，而为了一个图标引入 material-icons-extended
 * 会多打进 60MB 的类（本项目没开 R8 混淆），不划算，这里手绘几个简单几何图形代替。
 */

/** 报表：三根柱子 */
val BarChartIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "BarChart",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(4.5f, 13f); lineTo(8.5f, 13f); lineTo(8.5f, 20f); lineTo(4.5f, 20f); close()
            moveTo(10f, 5f); lineTo(14f, 5f); lineTo(14f, 20f); lineTo(10f, 20f); close()
            moveTo(15.5f, 9f); lineTo(19.5f, 9f); lineTo(19.5f, 20f); lineTo(15.5f, 20f); close()
        }
    }.build()
}

/** 账单：纸 + 两行字 */
val ReceiptIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Receipt",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // 左侧竖条 + 三条横线，视觉上像一张小票
        path(fill = SolidColor(Color.Black)) {
            moveTo(3f, 3f); lineTo(10f, 3f); lineTo(10f, 21f); lineTo(3f, 21f); close()
            moveTo(12f, 5f); lineTo(21f, 5f); lineTo(21f, 8f); lineTo(12f, 8f); close()
            moveTo(12f, 10.5f); lineTo(21f, 10.5f); lineTo(21f, 13.5f); lineTo(12f, 13.5f); close()
            moveTo(12f, 16f); lineTo(17f, 16f); lineTo(17f, 19f); lineTo(12f, 19f); close()
        }
    }.build()
}
