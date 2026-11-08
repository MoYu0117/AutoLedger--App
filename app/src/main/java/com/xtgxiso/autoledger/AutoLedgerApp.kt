package com.xtgxiso.autoledger

import android.app.Application
import com.xtgxiso.autoledger.data.AppDatabase
import com.xtgxiso.autoledger.data.AppSettings
import com.xtgxiso.autoledger.data.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AutoLedgerApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val settings: AppSettings by lazy { AppSettings(this) }
    val repository: TransactionRepository by lazy {
        TransactionRepository(database.transactionDao(), database.ruleDao(), settings)
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        // 首次安装写入默认识别规则
        appScope.launch { repository.seedDefaultsIfNeeded() }
    }

    companion object {
        /** 通知监听服务没有 Activity，只能通过这里拿到仓库 */
        lateinit var instance: AutoLedgerApp
            private set
    }
}
