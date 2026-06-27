package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.PreferencesManager
import com.example.data.model.TransactionEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoogleSheetsService(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    
    companion object {
        private const val TAG = "GoogleSheetsService"
        private const val TOKEN_URL = "https://oauth2.googleapis.com/token"
        private const val SHEETS_API_BASE = "https://sheets.googleapis.com/v4/spreadsheets"
        const val REDIRECT_URI = "https://oauth2.googleapis.com/token" // Can use standard token URL as redirect loopback
    }

    /**
     * Builds the Google OAuth2 authorization URL
     */
    fun getAuthorizationUrl(clientId: String): String {
        return "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=$clientId" +
                "&redirect_uri=https://oauth2.googleapis.com/token" + // Redirect to token loopback for copy/paste code extraction
                "&response_type=code" +
                "&scope=https://www.googleapis.com/auth/spreadsheets" +
                "&access_type=offline" +
                "&prompt=consent"
    }

    /**
     * Exchanges OAuth2 authorization code for Access & Refresh tokens
     */
    suspend fun exchangeCodeForToken(clientId: String, clientSecret: String, code: String): Result<Boolean> {
        val formBody = FormBody.Builder()
            .add("code", code)
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("redirect_uri", REDIRECT_URI)
            .add("grant_type", "authorization_code")
            .build()

        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(formBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string()
                if (response.isSuccessful && bodyStr != null) {
                    val json = JSONObject(bodyStr)
                    val accessToken = json.getString("access_token")
                    val refreshToken = json.optString("refresh_token", "")
                    val expiresIn = json.getLong("expires_in")
                    
                    // Save tokens to storage
                    preferencesManager.saveOAuthCredentials(clientId, clientSecret)
                    preferencesManager.saveOAuthTokens(accessToken, refreshToken, expiresIn)
                    Result.success(true)
                } else {
                    Log.e(TAG, "Failed to exchange token: $bodyStr")
                    Result.failure(IOException("Server returned: ${response.code} ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during token exchange", e)
            Result.failure(e)
        }
    }

    /**
     * Checks if we have an active or refreshable OAuth session
     */
    suspend fun isAuthorized(): Boolean {
        val refreshToken = preferencesManager.oauthRefreshTokenFlow.first()
        return refreshToken.isNotEmpty()
    }

    /**
     * Retrieves valid access token, refreshing if necessary
     */
    suspend fun getValidAccessToken(): String {
        val expiry = preferencesManager.oauthTokenExpiryFlow.first()
        val accessToken = preferencesManager.oauthAccessTokenFlow.first()
        val refreshToken = preferencesManager.oauthRefreshTokenFlow.first()
        val clientId = preferencesManager.oauthClientIdFlow.first()
        val clientSecret = preferencesManager.oauthClientSecretFlow.first()

        // If token expires in less than 5 minutes, refresh it
        if (System.currentTimeMillis() + 300_000 >= expiry) {
            if (refreshToken.isEmpty()) {
                throw IllegalStateException("OAuth refresh token is missing. Please sign in again.")
            }
            return refreshAccessToken(clientId, clientSecret, refreshToken)
        }
        return accessToken
    }

    /**
     * Refreshes the Access Token using the Refresh Token
     */
    private suspend fun refreshAccessToken(clientId: String, clientSecret: String, refreshToken: String): String {
        val formBody = FormBody.Builder()
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("refresh_token", refreshToken)
            .add("grant_type", "refresh_token")
            .build()

        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(formBody)
            .build()

        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string()
            if (response.isSuccessful && bodyStr != null) {
                val json = JSONObject(bodyStr)
                val newAccessToken = json.getString("access_token")
                val expiresIn = json.getLong("expires_in")
                
                preferencesManager.saveOAuthTokens(newAccessToken, "", expiresIn)
                return newAccessToken
            } else {
                Log.e(TAG, "Failed to refresh token: $bodyStr")
                throw IOException("Token refresh failed: ${response.code} ${response.message}")
            }
        }
    }

    /**
     * Creates headers sheet if it doesn't exist
     */
    suspend fun initializeSpreadsheet(spreadsheetId: String): Result<Boolean> {
        return try {
            val token = getValidAccessToken()
            
            // Check if "Transaksi" sheet exists by fetching spreadsheet metadata
            val metadataRequest = Request.Builder()
                .url("$SHEETS_API_BASE/$spreadsheetId")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(metadataRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IOException("Gagal mengambil metadata spreadsheet: ${response.code}"))
                }
                
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                val sheets = json.getJSONArray("sheets")
                var transaksisheetExists = false
                
                for (i in 0 until sheets.length()) {
                    val sheet = sheets.getJSONObject(i)
                    val properties = sheet.getJSONObject("properties")
                    if (properties.getString("title") == "Transaksi") {
                        transaksisheetExists = true
                        break
                    }
                }

                // If Transaksi sheet doesn't exist, create it
                if (!transaksisheetExists) {
                    val addSheetPayload = """
                        {
                          "requests": [
                            {
                              "addSheet": {
                                "properties": {
                                  "title": "Transaksi"
                                }
                              }
                            }
                          ]
                        }
                    """.trimIndent()

                    val addSheetRequest = Request.Builder()
                        .url("$SHEETS_API_BASE/$spreadsheetId:batchUpdate")
                        .addHeader("Authorization", "Bearer $token")
                        .post(addSheetPayload.toRequestBody("application/json".toMediaType()))
                        .build()

                    client.newCall(addSheetRequest).execute().use { addResponse ->
                        if (!addResponse.isSuccessful) {
                            return Result.failure(IOException("Gagal membuat sheet 'Transaksi'"))
                        }
                    }
                }
            }

            // Write Headers
            val headers = listOf("ID", "Judul", "Jumlah", "Tipe", "Kategori", "Tanggal", "Catatan")
            val payload = JSONObject().apply {
                put("range", "Transaksi!A1:G1")
                put("majorDimension", "ROWS")
                put("values", JSONArray().apply {
                    put(JSONArray().apply {
                        headers.forEach { put(it) }
                    })
                })
            }

            val writeHeadersRequest = Request.Builder()
                .url("$SHEETS_API_BASE/$spreadsheetId/values/Transaksi!A1:G1?valueInputOption=USER_ENTERED")
                .addHeader("Authorization", "Bearer $token")
                .put(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(writeHeadersRequest).execute().use { headerResponse ->
                if (headerResponse.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(IOException("Gagal menulis header ke spreadsheet"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Appends list of transactions as rows to Transaksi sheet
     */
    suspend fun appendTransactions(spreadsheetId: String, transactions: List<TransactionEntity>): Result<Int> {
        if (transactions.isEmpty()) return Result.success(0)
        
        return try {
            val token = getValidAccessToken()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            
            val rowsArray = JSONArray().apply {
                transactions.forEach { tx ->
                    put(JSONArray().apply {
                        put(tx.id.toString())
                        put(tx.title)
                        put(tx.amount)
                        put(if (tx.isIncome) "Pemasukan" else "Pengeluaran")
                        put(tx.category)
                        put(dateFormat.format(Date(tx.date)))
                        put(tx.note)
                    })
                }
            }

            val payload = JSONObject().apply {
                put("range", "Transaksi!A:G")
                put("majorDimension", "ROWS")
                put("values", rowsArray)
            }

            val request = Request.Builder()
                .url("$SHEETS_API_BASE/$spreadsheetId/values/Transaksi!A:G:append?valueInputOption=USER_ENTERED")
                .addHeader("Authorization", "Bearer $token")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(transactions.size)
                } else {
                    val errStr = response.body?.string() ?: ""
                    Result.failure(IOException("Append failed: $errStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reads all rows from Transaksi sheet
     */
    suspend fun readTransactions(spreadsheetId: String): Result<List<TransactionEntity>> {
        return try {
            val token = getValidAccessToken()
            val request = Request.Builder()
                .url("$SHEETS_API_BASE/$spreadsheetId/values/Transaksi!A2:G")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    val values = json.optJSONArray("values")
                    val list = mutableListOf<TransactionEntity>()
                    
                    if (values != null) {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        for (i in 0 until values.length()) {
                            val row = values.getJSONArray(i)
                            if (row.length() < 5) continue
                            
                            val id = row.optString(0).toLongOrNull() ?: System.currentTimeMillis() + i
                            val title = row.optString(1)
                            val amount = row.optString(2).toDoubleOrNull() ?: 0.0
                            val type = row.optString(3)
                            val category = row.optString(4)
                            
                            val dateStr = row.optString(5)
                            val date = try {
                                dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                            } catch (e: Exception) {
                                System.currentTimeMillis()
                            }
                            
                            val note = row.optString(6, "")
                            
                            list.add(
                                TransactionEntity(
                                    id = id,
                                    title = title,
                                    amount = amount,
                                    isIncome = type == "Pemasukan",
                                    category = category,
                                    date = date,
                                    note = note,
                                    synced = true
                                )
                            )
                        }
                    }
                    Result.success(list)
                } else {
                    Result.failure(IOException("Server returned code: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
