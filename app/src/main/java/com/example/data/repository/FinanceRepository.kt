package com.example.data.repository

import android.util.Log
import com.example.data.local.TransactionDao
import com.example.data.model.BudgetEntity
import com.example.data.model.RecurringEntity
import com.example.data.model.TransactionEntity
import com.example.data.remote.GoogleSheetsService
import com.example.data.remote.RetrofitClient
import com.example.data.remote.SyncRequest
import com.example.data.remote.SyncTransactionDto
import kotlinx.coroutines.flow.Flow
import java.lang.Exception

class FinanceRepository(private val dao: TransactionDao) {

    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val allBudgets: Flow<List<BudgetEntity>> = dao.getAllBudgets()
    val allRecurring: Flow<List<RecurringEntity>> = dao.getAllRecurring()

    fun getTransactionsInRange(start: Long, end: Long): Flow<List<TransactionEntity>> {
        return dao.getTransactionsInRange(start, end)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return dao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(id: Long) {
        dao.deleteTransaction(id)
    }

    suspend fun clearAllTransactions() {
        dao.clearAllTransactions()
    }

    suspend fun insertBudget(budget: BudgetEntity) {
        dao.insertBudget(budget)
    }

    suspend fun deleteBudget(category: String) {
        dao.deleteBudget(category)
    }

    suspend fun insertRecurring(recurring: RecurringEntity) {
        dao.insertRecurring(recurring)
    }

    suspend fun deleteRecurring(id: Long) {
        dao.deleteRecurring(id)
    }

    /**
     * Bi-directional synchronization with Google Sheets Apps Script Web App
     */
    suspend fun syncWithGoogleSheets(url: String): Result<String> {
        if (url.isEmpty()) {
            return Result.failure(Exception("URL Google Apps Script belum diatur"))
        }

        try {
            // 1. Get unsynced local transactions
            val unsyncedLocal = dao.getUnsyncedTransactions()
            val dtos = unsyncedLocal.map {
                SyncTransactionDto(
                    id = it.id,
                    title = it.title,
                    amount = it.amount,
                    isIncome = it.isIncome,
                    category = it.category,
                    date = it.date,
                    note = it.note
                )
            }

            // 2. Network Sync request
            val request = SyncRequest(action = "sync", transactions = dtos)
            val response = RetrofitClient.api.syncWithAppsScript(url, request)

            if (response.isSuccessful && response.body() != null) {
                val syncResult = response.body()!!
                if (syncResult.success) {
                    // 3. Mark unsynced local transactions as synced
                    if (unsyncedLocal.isNotEmpty()) {
                        val ids = unsyncedLocal.map { it.id }
                        dao.markAsSynced(ids)
                    }

                    // 4. Merge server transactions to local Room
                    val remoteTransactions = syncResult.transactions.map {
                        TransactionEntity(
                            id = it.id,
                            title = it.title,
                            amount = it.amount,
                            isIncome = it.isIncome,
                            category = it.category,
                            date = it.date,
                            note = it.note,
                            synced = true // Remote ones are already synced
                        )
                    }

                    if (remoteTransactions.isNotEmpty()) {
                        dao.insertTransactions(remoteTransactions)
                    }

                    return Result.success("Sinkronisasi Berhasil! ${remoteTransactions.size} transaksi diperbarui.")
                } else {
                    return Result.failure(Exception(syncResult.message.ifEmpty { "Gagal melakukan sinkronisasi." }))
                }
            } else {
                return Result.failure(Exception("Gagal menghubungi server Google Sheets: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("FinanceRepository", "Sync failed", e)
            return Result.failure(e)
        }
    }

    /**
     * Bi-directional synchronization using Google Sheets API (OAuth2)
     */
    suspend fun syncWithGoogleSheetsAPI(spreadsheetId: String, service: GoogleSheetsService): Result<String> {
        if (spreadsheetId.isEmpty()) {
            return Result.failure(Exception("Spreadsheet ID belum diatur di Pengaturan"))
        }

        try {
            // 1. Initialize spreadsheet headers if needed
            val initRes = service.initializeSpreadsheet(spreadsheetId)
            if (initRes.isFailure) {
                return Result.failure(initRes.exceptionOrNull() ?: Exception("Gagal menginisialisasi spreadsheet"))
            }

            // 2. Fetch remote transactions from Google Sheets API
            val remoteRes = service.readTransactions(spreadsheetId)
            if (remoteRes.isFailure) {
                return Result.failure(remoteRes.exceptionOrNull() ?: Exception("Gagal membaca dari spreadsheet"))
            }
            val remoteTransactions = remoteRes.getOrDefault(emptyList())

            // 3. Get unsynced local transactions
            val unsyncedLocal = dao.getUnsyncedTransactions()

            // 4. Send unsynced local transactions to Google Sheets
            if (unsyncedLocal.isNotEmpty()) {
                val appendRes = service.appendTransactions(spreadsheetId, unsyncedLocal)
                if (appendRes.isFailure) {
                    return Result.failure(appendRes.exceptionOrNull() ?: Exception("Gagal mengunggah transaksi baru"))
                }
                
                // Mark locally unsynced transactions as synced
                val ids = unsyncedLocal.map { it.id }
                dao.markAsSynced(ids)
            }

            // 5. Merge remote transactions that do not exist locally into the local DB
            if (remoteTransactions.isNotEmpty()) {
                dao.insertTransactions(remoteTransactions)
            }

            val totalUpdated = unsyncedLocal.size + remoteTransactions.size
            return Result.success("Sinkronisasi Sheets API Berhasil! $totalUpdated transaksi disinkronkan.")
        } catch (e: Exception) {
            Log.e("FinanceRepository", "Sheets API Sync failed", e)
            return Result.failure(e)
        }
    }
}
