package com.xtgxiso.autoledger.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一条账目。
 * 注意 amountCents 是「分」，不是「元」。
 */
@Entity(
    tableName = "transactions",
    indices = [Index("time"), Index("status")]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** 金额，单位：分 */
    val amountCents: Long,
    /** true = 支出，false = 收入 */
    val isExpense: Boolean,
    /** 商户/对方名称 */
    val payee: String,
    /** 分类，取自 Categories */
    val category: String,
    /** 发生时间，毫秒时间戳 */
    val time: Long,
    /** 来源：包名（com.tencent.mm / com.eg.android.AlipayGphone）或 "manual" */
    val source: String,
    /** 原始通知文本，留着排查"为什么识别错了" */
    val rawText: String = "",
    val note: String = "",
    /** 0 = 待确认，1 = 已入账 */
    val status: Int = CONFIRMED,
) {
    companion object {
        const val PENDING = 0
        const val CONFIRMED = 1
        const val SOURCE_MANUAL = "manual"
    }
}
