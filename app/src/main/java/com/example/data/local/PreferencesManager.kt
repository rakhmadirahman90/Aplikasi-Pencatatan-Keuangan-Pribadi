package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        val SPREADSHEET_URL = stringPreferencesKey("spreadsheet_url")
        val SPREADSHEET_ID = stringPreferencesKey("spreadsheet_id")
        val SYNC_MODE = stringPreferencesKey("sync_mode") // "AppsScript" or "SheetsAPI"
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        val INITIAL_BALANCE = doublePreferencesKey("initial_balance")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
    }

    val spreadsheetUrlFlow: Flow<String> = context.dataStore.data.map { it[SPREADSHEET_URL] ?: "" }
    val spreadsheetIdFlow: Flow<String> = context.dataStore.data.map { it[SPREADSHEET_ID] ?: "" }
    val syncModeFlow: Flow<String> = context.dataStore.data.map { it[SYNC_MODE] ?: "AppsScript" }
    val lastSyncTimeFlow: Flow<Long> = context.dataStore.data.map { it[LAST_SYNC_TIME] ?: 0L }
    val initialBalanceFlow: Flow<Double> = context.dataStore.data.map { it[INITIAL_BALANCE] ?: 0.0 }
    val isDarkThemeFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_DARK_THEME] ?: false }

    suspend fun saveSpreadsheetUrl(url: String) {
        context.dataStore.edit { it[SPREADSHEET_URL] = url }
    }

    suspend fun saveSpreadsheetId(id: String) {
        context.dataStore.edit { it[SPREADSHEET_ID] = id }
    }

    suspend fun saveSyncMode(mode: String) {
        context.dataStore.edit { it[SYNC_MODE] = mode }
    }

    suspend fun saveLastSyncTime(time: Long) {
        context.dataStore.edit { it[LAST_SYNC_TIME] = time }
    }

    suspend fun saveInitialBalance(balance: Double) {
        context.dataStore.edit { it[INITIAL_BALANCE] = balance }
    }

    suspend fun saveDarkTheme(isDark: Boolean) {
        context.dataStore.edit { it[IS_DARK_THEME] = isDark }
    }
}
