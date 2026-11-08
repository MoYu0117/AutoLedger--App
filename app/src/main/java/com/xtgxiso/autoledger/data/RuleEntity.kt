package com.xtgxiso.autoledger.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 识别规则。三类：
 * - PAYEE   ：商户关键词 -> 分类（如 "沃尔玛" -> "日用"）
 * - INCOME  ：额外收入关键词（如 "红包" 也算收入）
 * - EXPENSE ：额外支出关键词
 *
 * 微信/支付宝改文案时，不用重新打包，改这里的规则即可。
 */
@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val kind: String,
    val keyword: String,
    val category: String = "",
    val enabled: Boolean = true,
) {
    companion object {
        const val KIND_PAYEE = "PAYEE"
        const val KIND_INCOME = "INCOME"
        const val KIND_EXPENSE = "EXPENSE"
    }
}

@Dao
interface RuleDao {

    @Query("SELECT * FROM rules ORDER BY kind ASC, id ASC")
    fun observeAll(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE enabled = 1")
    suspend fun enabledOnce(): List<RuleEntity>

    @Query("SELECT COUNT(*) FROM rules")
    suspend fun count(): Int

    @Insert
    suspend fun insert(rule: RuleEntity): Long

    @Insert
    suspend fun insertAll(rules: List<RuleEntity>)

    @Update
    suspend fun update(rule: RuleEntity)

    @Delete
    suspend fun delete(rule: RuleEntity)

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** v1.1 分类改名，启动时跑一次，幂等 */
    @Query("UPDATE rules SET category = :to WHERE category = :from")
    suspend fun renameCategory(from: String, to: String)
}

/** 首次安装自动写入的默认规则，可删可改 */
object DefaultRules {
    val list: List<RuleEntity> = listOf(
        // —— 商户 → 分类 ——
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "美团", category = "餐饮"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "饿了么", category = "餐饮"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "肯德基", category = "餐饮"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "麦当劳", category = "餐饮"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "星巴克", category = "餐饮"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "瑞幸", category = "餐饮"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "餐", category = "餐饮"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "饭", category = "餐饮"),

        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "滴滴", category = "交通"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "地铁", category = "交通"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "公交", category = "交通"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "加油", category = "交通"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "停车", category = "交通"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "高铁", category = "交通"),

        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "淘宝", category = "购物"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "天猫", category = "购物"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "京东", category = "购物"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "拼多多", category = "购物"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "唯品会", category = "购物"),

        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "超市", category = "日常"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "便利店", category = "日常"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "沃尔玛", category = "日常"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "永辉", category = "日常"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "盒马", category = "日常"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "药房", category = "医疗"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "医院", category = "医疗"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "药店", category = "医疗"),

        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "电影", category = "娱乐"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "影院", category = "娱乐"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "游戏", category = "娱乐"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "KTV", category = "娱乐"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "会员", category = "娱乐"),

        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "房租", category = "住宿"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "物业", category = "住宿"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "水费", category = "住宿"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "电费", category = "住宿"),
        RuleEntity(kind = RuleEntity.KIND_PAYEE, keyword = "燃气", category = "住宿"),

        // —— 额外关键词 ——
        RuleEntity(kind = RuleEntity.KIND_INCOME, keyword = "红包"),
        RuleEntity(kind = RuleEntity.KIND_INCOME, keyword = "转账给你"),
        RuleEntity(kind = RuleEntity.KIND_EXPENSE, keyword = "自动续费"),
    )
}
