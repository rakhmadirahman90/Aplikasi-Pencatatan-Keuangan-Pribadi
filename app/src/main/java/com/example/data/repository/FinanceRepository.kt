package com.example.data.repository

import android.util.Log
import com.example.data.local.TransactionDao
import com.example.data.model.BudgetEntity
import com.example.data.model.RecurringEntity
import com.example.data.model.TransactionEntity
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
}
