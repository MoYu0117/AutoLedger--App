package com.xtgxiso.autoledger.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    /** 某个月、某个状态的账目，最新在前 */
    @Query(
        "SELECT * FROM transactions WHERE status = :status AND time >= :from AND time < :to " +
            "ORDER BY time DESC"
    )
    fun observeRange(from: Long, to: Long, status: Int): Flow<List<Transaction>>

    /** 从某时刻起的账目（首页「近三日」用，可跨月） */
    @Query(
        "SELECT * FROM transactions WHERE status = :status AND time >= :from " +
            "ORDER BY time DESC LIMIT 300"
    )
    fun observeSince(from: Long, status: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    /** 全部条数，设置页展示用 */
    @Query("SELECT COUNT(*) FROM transactions")
    fun observeCount(): Flow<Int>

    /**
     * 去重查询：同一来源、同一金额、在 [from, to] 时间窗内已有记录 -> 判定为重复。
     * 用来挡住「同一条通知被系统重复投递」和「同一笔支付连发两条通知」。
     */
    @Query(
        "SELECT * FROM transactions WHERE source = :source AND amountCents = :cents " +
            "AND time BETWEEN :from AND :to LIMIT 1"
    )
    suspend fun findDuplicate(cents: Long, from: Long, to: Long, source: String): Transaction?

    @Insert
    suspend fun insert(item: Transaction): Long

    @Update
    suspend fun update(item: Transaction)

    @Delete
    suspend fun delete(item: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()

    /** v1.1 分类改名（日用→日常、居住→住宿），启动时跑一次，幂等 */
    @Query("UPDATE transactions SET category = :to WHERE category = :from")
    suspend fun renameCategory(from: String, to: String)

    /** 删除某时刻之前的全部记录，返回删除条数 */
    @Query("DELETE FROM transactions WHERE time < :time")
    suspend fun deleteBefore(time: Long): Int

    /** 最早一笔记录的时间，没有则返回 null */
    @Query("SELECT MIN(time) FROM transactions")
    suspend fun earliestTime(): Long?
}
