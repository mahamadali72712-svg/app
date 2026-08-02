package com.example.di

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.repository.StoreRepository
import com.example.data.sync.SyncEngine

interface AppContainer {
    val storeRepository: StoreRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val database: AppDatabase by lazy { AppDatabase.getDatabase(context) }
    
    private val syncEngine: SyncEngine by lazy {
        SyncEngine(context, database)
    }

    override val storeRepository: StoreRepository by lazy {
        StoreRepository(
            productDao = database.productDao(),
            salesDao = database.salesDao(),
            financeDao = database.financeDao(),
            purchaseDao = database.purchaseDao(),
            partiesDao = database.partiesDao(),
            syncDao = database.syncDao(),
            syncEngine = syncEngine
        )
    }
}
