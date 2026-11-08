package com.xtgxiso.autoledger.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// DataStore 实例必须放在文件顶层（扩展属性），否则会重复创建导致崩溃
private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "autoledger_settings")

/**
 * App 设置项，全部落在 DataStore（本地文件），不联网。
 */
class AppSettings(private val context: Context) {

    val watchedPackages: Flow<Set<String>> = context.settingsStore.data
        .map { it[KEY_WATCHED] ?: DEFAULT_WATCHED }

    val autoConfirm: Flow<Boolean> = context.settingsStore.data
        .map { it[KEY_AUTO_CONFIRM] ?: true }

    val dedupeSeconds: Flow<Int> = context.settingsStore.data
        .map { it[KEY_DEDUPE] ?: 60 }

    /** 监听服务里同步调用的小工具方法 */
    suspend fun isWatched(pkg: String): Boolean = watchedPackages.first().contains(pkg)

    suspend fun shouldAutoConfirm(): Boolean = autoConfirm.first()

    suspend fun dedupeWindowMillis(): Long = dedupeSeconds.first() * 1000L

    suspend fun setWatched(packages: Set<String>) {
        context.settingsStore.edit { it[KEY_WATCHED] = packages }
    }

    suspend fun toggleWatched(pkg: String, on: Boolean) {
        context.settingsStore.edit { prefs ->
            val current = (prefs[KEY_WATCHED] ?: DEFAULT_WATCHED).toMutableSet()
            if (on) current.add(pkg) else current.remove(pkg)
            prefs[KEY_WATCHED] = current
        }
    }

    suspend fun setAutoConfirm(value: Boolean) {
        context.settingsStore.edit { it[KEY_AUTO_CONFIRM] = value }
    }

    suspend fun setDedupeSeconds(value: Int) {
        context.settingsStore.edit { it[KEY_DEDUPE] = value }
    }

    companion object {
        const val PKG_WECHAT = "com.tencent.mm"
        const val PKG_ALIPAY = "com.eg.android.AlipayGphone"

        private val DEFAULT_WATCHED = setOf(PKG_WECHAT, PKG_ALIPAY)

        private val KEY_WATCHED = stringSetPreferencesKey("watched_packages")
        private val KEY_AUTO_CONFIRM = booleanPreferencesKey("auto_confirm")
        private val KEY_DEDUPE = intPreferencesKey("dedupe_seconds")
    }
}
