package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.model.BudgetEntity
import com.example.data.model.RecurringEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = FinanceRepository(database.transactionDao())
    private val preferencesManager = PreferencesManager(application)

    // Current month & year filter
    private val _currentMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH)) // 0-11
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()

    private val _currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()

    // Settings flows
    val spreadsheetUrl: StateFlow<String> = preferencesManager.spreadsheetUrlFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val syncMode: StateFlow<String> = preferencesManager.syncModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "AppsScript")

    val lastSyncTime: StateFlow<Long> = preferencesManager.lastSyncTimeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val initialBalance: StateFlow<Double> = preferencesManager.initialBalanceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 200000000.0) // default 200 million Rupiah to make it look rich!

    val isDarkTheme: StateFlow<Boolean> = preferencesManager.isDarkThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Raw transactions, budgets, recurring
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBudgets: StateFlow<List<BudgetEntity>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecurring: StateFlow<List<RecurringEntity>> = repository.allRecurring
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state for search/filter in Transaksi tab
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _categoryFilter = MutableStateFlow("Semua") // "Semua", "Pengeluaran", "Pemasukan"
    val categoryFilter: StateFlow<String> = _categoryFilter.asStateFlow()

    // Sync status messages
    private val _syncStatus = MutableStateFlow("Selesai") // "Selesai", "Sinkronisasi...", "Gagal"
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _syncMessage = MutableStateFlow("")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    init {
        // Seed initial mock data if database is empty so user gets a fully functional and rich experience immediately
        viewModelScope.launch {
            repository.allTransactions.first().let { list ->
                if (list.isEmpty()) {
                    seedMockData()
                }
            }
        }
    }

    /**
     * Set mock transaction, budgets, and recurring items to mimic the screenshots perfectly on first launch
     */
    private suspend fun seedMockData() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, Calendar.MAY)
        cal.set(Calendar.YEAR, 2026)

        // Reset to first week of May
        val txs = listOf(
            TransactionEntity(title = "Gaji bulanan", amount = 10650000.0, isIncome = true, category = "Gaji", date = getMockDate(cal, 25), note = "Gaji - 25/05"),
            TransactionEntity(title = "Freelance UI Design", amount = 3900000.0, isIncome = true, category = "Freelance", date = getMockDate(cal, 10), note = "Proyek Mobile App"),
            TransactionEntity(title = "Beli sepatu", amount = 240000.0, isIncome = false, category = "Belanja", date = getMockDate(cal, 23), note = "Belanja - 23/05"),
            TransactionEntity(title = "Makan warkop", amount = 10000.0, isIncome = false, category = "Makanan", date = getMockDate(cal, 31), note = "Makanan - 31/05"),
            TransactionEntity(title = "Gorengan", amount = 46000.0, isIncome = false, category = "Makanan", date = getMockDate(cal, 24), note = "Makanan - 24/05"),
            TransactionEntity(title = "Grab ke kantor", amount = 61000.0, isIncome = false, category = "Transport", date = getMockDate(cal, 24), note = "Transport - 24/05"),
            TransactionEntity(title = "Bayar kos", amount = 1650000.0, isIncome = false, category = "Rumah", date = getMockDate(cal, 1), note = "Sewa bulanan"),
            TransactionEntity(title = "Tagihan Wifi", amount = 350000.0, isIncome = false, category = "Tagihan", date = getMockDate(cal, 3), note = "Indihome"),
            TransactionEntity(title = "Listrik Token", amount = 505000.0, isIncome = false, category = "Tagihan", date = getMockDate(cal, 6), note = "Token Prabayar"),
            TransactionEntity(title = "Belanja Bulanan", amount = 1140000.0, isIncome = false, category = "Belanja", date = getMockDate(cal, 8), note = "Supermarket"),
            TransactionEntity(title = "Bioskop & popcorn", amount = 180000.0, isIncome = false, category = "Hiburan", date = getMockDate(cal, 15), note = "CGV Weekend"),
            TransactionEntity(title = "Obat flu", amount = 150000.0, isIncome = false, category = "Kesehatan", date = getMockDate(cal, 18), note = "Apotek K-24"),
            TransactionEntity(title = "Buku Pemrograman Kotlin", amount = 240000.0, isIncome = false, category = "Pendidikan", date = getMockDate(cal, 20), note = "Gramedia")
        )

        txs.forEach { repository.insertTransaction(it) }

        // Seed Budgets
        val budgets = listOf(
            BudgetEntity("Makanan", 1000000.0),
            BudgetEntity("Belanja", 1500000.0),
            BudgetEntity("Transport", 600000.0),
            BudgetEntity("Tagihan", 1000000.0)
        )
        budgets.forEach { repository.insertBudget(it) }

        // Seed Recurring
        val recurring = listOf(
            RecurringEntity(title = "Sewa Kos", amount = 1650000.0, isIncome = false, category = "Rumah", interval = "Bulanan", nextDueDate = getMockDate(cal, 1)),
            RecurringEntity(title = "Langganan Spotify", amount = 55000.0, isIncome = false, category = "Hiburan", interval = "Bulanan", nextDueDate = getMockDate(cal, 5))
        )
        recurring.forEach { repository.insertRecurring(it) }
    }

    private fun getMockDate(baseCal: Calendar, day: Int): Long {
        val cal = baseCal.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, day)
        return cal.timeInMillis
    }

    // Navigation and Calendar actions
    fun nextMonth() {
        if (_currentMonth.value == 11) {
            _currentMonth.value = 0
            _currentYear.value += 1
        } else {
            _currentMonth.value += 1
        }
    }

    fun prevMonth() {
        if (_currentMonth.value == 0) {
            _currentMonth.value = 11
            _currentYear.value -= 1
        } else {
            _currentMonth.value -= 1
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(filter: String) {
        _categoryFilter.value = filter
    }

    // Actions
    fun addTransaction(title: String, amount: Double, isIncome: Boolean, category: String, dateMillis: Long, note: String) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                title = title,
                amount = amount,
                isIncome = isIncome,
                category = category,
                date = dateMillis,
                note = note,
                synced = false
            )
            repository.insertTransaction(tx)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun saveSpreadsheetUrl(url: String) {
        viewModelScope.launch {
            preferencesManager.saveSpreadsheetUrl(url)
        }
    }

    fun saveInitialBalance(balance: Double) {
        viewModelScope.launch {
            preferencesManager.saveInitialBalance(balance)
        }
    }

    fun saveDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            preferencesManager.saveDarkTheme(isDark)
        }
    }

    fun addBudget(category: String, amount: Double) {
        viewModelScope.launch {
            repository.insertBudget(BudgetEntity(category, amount))
        }
    }

    fun deleteBudget(category: String) {
        viewModelScope.launch {
            repository.deleteBudget(category)
        }
    }

    fun addRecurring(title: String, amount: Double, isIncome: Boolean, category: String, interval: String, dateMillis: Long) {
        viewModelScope.launch {
            repository.insertRecurring(RecurringEntity(
                title = title,
                amount = amount,
                isIncome = isIncome,
                category = category,
                interval = interval,
                nextDueDate = dateMillis
            ))
        }
    }

    fun deleteRecurring(id: Long) {
        viewModelScope.launch {
            repository.deleteRecurring(id)
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            _syncStatus.value = "Sinkronisasi..."
            _syncMessage.value = "Menghubungkan ke Google Spreadsheet..."
            val url = spreadsheetUrl.value
            if (url.isEmpty()) {
                _syncStatus.value = "Gagal"
                _syncMessage.value = "Masukkan URL Google Apps Script di Pengaturan terlebih dahulu."
                return@launch
            }

            val result = repository.syncWithGoogleSheets(url)
            if (result.isSuccess) {
                _syncStatus.value = "Selesai"
                _syncMessage.value = result.getOrNull() ?: "Berhasil sinkronisasi!"
                preferencesManager.saveLastSyncTime(System.currentTimeMillis())
            } else {
                _syncStatus.value = "Gagal"
                _syncMessage.value = result.exceptionOrNull()?.message ?: "Gagal terhubung dengan Spreadsheet."
            }
        }
    }

    // State Calculations for selected Month & Year
    val filteredTransactions: Flow<List<TransactionEntity>> = combine(
        allTransactions, currentMonth, currentYear
    ) { txs, month, year ->
        txs.filter { tx ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = tx.date
            cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
        }
    }

    // Filtered transaction list based on Search & Category Filters in Transaksi tab
    val searchedTransactions: Flow<List<TransactionEntity>> = combine(
        filteredTransactions, searchQuery, categoryFilter
    ) { txs, query, filter ->
        txs.filter { tx ->
            val matchesSearch = tx.title.contains(query, ignoreCase = true) || 
                                tx.category.contains(query, ignoreCase = true) ||
                                tx.note.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                "Semua" -> true
                "Pemasukan" -> tx.isIncome
                "Pengeluaran" -> !tx.isIncome
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    // Financial summaries computed on the fly
    val monthlySummary = combine(filteredTransactions, initialBalance, allTransactions) { monthTxs, initBal, allTxs ->
        val income = monthTxs.filter { it.isIncome }.sumOf { it.amount }
        val expense = monthTxs.filter { !it.isIncome }.sumOf { it.amount }
        val surplus = income - expense

        // Total balance = Initial balance + All time incomes - All time expenses
        val allIncome = allTxs.filter { it.isIncome }.sumOf { it.amount }
        val allExpense = allTxs.filter { !it.isIncome }.sumOf { it.amount }
        val totalBalance = initBal + allIncome - allExpense

        val largestExpense = monthTxs.filter { !it.isIncome }.maxOfOrNull { it.amount } ?: 0.0
        val count = monthTxs.size

        // Calculate average spending per day in active days
        val cal = Calendar.getInstance()
        val currentDay = if (cal.get(Calendar.MONTH) == _currentMonth.value) cal.get(Calendar.DAY_OF_MONTH) else cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dailyAvg = if (currentDay > 0) expense / currentDay else 0.0

        // Saving ratio (Rasio Nabung)
        val savingsRate = if (income > 0) {
            ((income - expense) / income) * 100
        } else {
            0.0
        }

        MonthlySummary(
            totalBalance = totalBalance,
            monthlyIncome = income,
            monthlyExpense = expense,
            surplus = surplus,
            dailyAverage = dailyAvg,
            largestExpense = largestExpense,
            transactionCount = count,
            savingsRate = savingsRate.coerceIn(0.0, 100.0)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlySummary())

    // Group expenses by category for pie charts
    val categoryExpenses = filteredTransactions.map { txs ->
        val expensesOnly = txs.filter { !it.isIncome }
        val total = expensesOnly.sumOf { it.amount }
        if (total == 0.0) return@map emptyList<CategoryExpenseSummary>()

        expensesOnly.groupBy { it.category }
            .map { (cat, list) ->
                val amount = list.sumOf { it.amount }
                CategoryExpenseSummary(
                    category = cat,
                    amount = amount,
                    percentage = (amount / total).toFloat()
                )
            }.sortedByDescending { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Heatmap data: map dayOfMonth to spending amount
    val calendarHeatmap = filteredTransactions.map { txs ->
        txs.filter { !it.isIncome }
            .groupBy {
                val cal = Calendar.getInstance()
                cal.timeInMillis = it.date
                cal.get(Calendar.DAY_OF_MONTH)
            }.mapValues { (_, list) ->
                list.sumOf { it.amount }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
}

data class MonthlySummary(
    val totalBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val surplus: Double = 0.0,
    val dailyAverage: Double = 0.0,
    val largestExpense: Double = 0.0,
    val transactionCount: Int = 0,
    val savingsRate: Double = 0.0
)

data class CategoryExpenseSummary(
    val category: String,
    val amount: Double,
    val percentage: Float
)
