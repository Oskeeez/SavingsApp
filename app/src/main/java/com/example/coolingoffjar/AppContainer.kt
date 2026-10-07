package com.example.coolingoffjar

import android.content.Context
import com.example.coolingoffjar.data.db.AppDatabase
import com.example.coolingoffjar.data.repo.CoolingOffRepository
import com.example.coolingoffjar.data.repo.ShelfRepository
import com.example.coolingoffjar.data.settings.SettingsRepository

/** Hand-rolled dependency container: simple and readable beats a DI framework for an app this size. */
class AppContainer(context: Context) {
    private val database = AppDatabase.create(context)
    val settingsRepository = SettingsRepository(context)
    val repository = CoolingOffRepository(database, settingsRepository)
    val shelfRepository = ShelfRepository(database)
}
