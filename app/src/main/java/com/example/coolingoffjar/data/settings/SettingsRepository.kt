package com.example.coolingoffjar.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.coolingoffjar.domain.Settings
import com.example.coolingoffjar.domain.SettingsRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.settingsDataStore)

    /** Values are clamped on read as well as on write, so a bad stored value can never leak out. */
    val settings: Flow<Settings> = store.data.map { prefs ->
        Settings(
            coolOffDays = SettingsRules.clampCoolOffDays(prefs[COOL_OFF_DAYS] ?: SettingsRules.DEFAULT_COOL_OFF_DAYS),
            notBuysPerJar = SettingsRules.clampNotBuysPerJar(prefs[NOT_BUYS_PER_JAR] ?: SettingsRules.DEFAULT_NOT_BUYS_PER_JAR),
        )
    }

    suspend fun setCoolOffDays(days: Int) {
        store.edit { it[COOL_OFF_DAYS] = SettingsRules.clampCoolOffDays(days) }
    }

    suspend fun setNotBuysPerJar(n: Int) {
        store.edit { it[NOT_BUYS_PER_JAR] = SettingsRules.clampNotBuysPerJar(n) }
    }

    private companion object {
        val COOL_OFF_DAYS = intPreferencesKey("cool_off_days")
        val NOT_BUYS_PER_JAR = intPreferencesKey("not_buys_per_jar")
    }
}
