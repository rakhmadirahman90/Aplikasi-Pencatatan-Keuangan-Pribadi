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
        
        // OAuth2 Storage Keys
        val OAUTH_CLIENT_ID = stringPreferencesKey("oauth_client_id")
        val OAUTH_CLIENT_SECRET = stringPreferencesKey("oauth_client_secret")
        val OAUTH_ACCESS_TOKEN = stringPreferencesKey("oauth_access_token")
        val OAUTH_REFRESH_TOKEN = stringPreferencesKey("oauth_refresh_token")
        val OAUTH_TOKEN_EXPIRY = longPreferencesKey("oauth_token_expiry")
    }

    val spreadsheetUrlFlow: Flow<String> = context.dataStore.data.map { it[SPREADSHEET_URL] ?: "" }
    val spreadsheetIdFlow: Flow<String> = context.dataStore.data.map { it[SPREADSHEET_ID] ?: "" }
    val syncModeFlow: Flow<String> = context.dataStore.data.map { it[SYNC_MODE] ?: "AppsScript" }
    val lastSyncTimeFlow: Flow<Long> = context.dataStore.data.map { it[LAST_SYNC_TIME] ?: 0L }
    val initialBalanceFlow: Flow<Double> = context.dataStore.data.map { it[INITIAL_BALANCE] ?: 0.0 }
    val isDarkThemeFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_DARK_THEME] ?: false }

    // OAuth2 Flows
    val oauthClientIdFlow: Flow<String> = context.dataStore.data.map { it[OAUTH_CLIENT_ID] ?: "" }
    val oauthClientSecretFlow: Flow<String> = context.dataStore.data.map { it[OAUTH_CLIENT_SECRET] ?: "" }
    val oauthAccessTokenFlow: Flow<String> = context.dataStore.data.map { it[OAUTH_ACCESS_TOKEN] ?: "" }
    val oauthRefreshTokenFlow: Flow<String> = context.dataStore.data.map { it[OAUTH_REFRESH_TOKEN] ?: "" }
    val oauthTokenExpiryFlow: Flow<Long> = context.dataStore.data.map { it[OAUTH_TOKEN_EXPIRY] ?: 0L }

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

    suspend fun saveOAuthCredentials(clientId: String, clientSecret: String) {
        context.dataStore.edit {
            it[OAUTH_CLIENT_ID] = clientId
            it[OAUTH_CLIENT_SECRET] = clientSecret
        }
    }

    suspend fun saveOAuthTokens(accessToken: String, refreshToken: String, expiresIn: Long) {
        context.dataStore.edit {
            it[OAUTH_ACCESS_TOKEN] = accessToken
            if (refreshToken.isNotEmpty()) {
                it[OAUTH_REFRESH_TOKEN] = refreshToken
            }
            it[OAUTH_TOKEN_EXPIRY] = System.currentTimeMillis() + (expiresIn * 1000)
        }
    }

    suspend fun clearOAuthTokens() {
        context.dataStore.edit {
            it.remove(OAUTH_ACCESS_TOKEN)
            it.remove(OAUTH_REFRESH_TOKEN)
            it.remove(OAUTH_TOKEN_EXPIRY)
        }
    }
}
