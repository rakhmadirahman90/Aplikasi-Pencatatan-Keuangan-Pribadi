package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.ui.components.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransaksiScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val searchedTransactions by viewModel.searchedTransactions.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categoryFilter by viewModel.categoryFilter.collectAsState()

    // Calculate sum of filtered Transactions
    val filteredIncomeSum = searchedTransactions.filter { it.isIncome }.sumOf { it.amount }
    val filteredExpenseSum = searchedTransactions.filter { !it.isIncome }.sumOf { it.amount }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Space under safe margins
        Spacer(modifier = Modifier.height(12.dp))

        // 1. SEARCH BAR
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Cari transaksi...", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_bar_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TealPrimary,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.6f)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. SEGMENTED FILTER BUTTONS: Semua, Pengeluaran, Pemasukan
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Semua", "Pengeluaran", "Pemasukan").forEach { label ->
                val isActive = categoryFilter == label
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isActive) TealPrimary else Color.Transparent)
                        .clickable { viewModel.setCategoryFilter(label) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. INFLOW / OUTFLOW SUMMARY CARDS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Masuk Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "↙ MASUK",
                        style = MaterialTheme.typography.labelSmall,
                        color = IncomeGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        formatRupiah(filteredIncomeSum).replace(",00", ""),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                }
            }

            // Keluar Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "↗ KELUAR",
                        style = MaterialTheme.typography.labelSmall,
                        color = ExpenseRed,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        formatRupiah(filteredExpenseSum).replace(",00", ""),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. GROUPED TRANSACTIONS LIST BY DATE
        if (searchedTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Tidak ada transaksi ditemukan",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Coba masukkan filter pencarian atau rekam transaksi baru.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            // Perform grouping by date
            val grouped = remember(searchedTransactions) {
                groupTransactionsByDate(searchedTransactions)
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                grouped.forEach { (dateGroup, list) ->
                    // Date group header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateGroup.headerLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            // Show net sum for this day
                            val netSum = dateGroup.netAmount
                            Text(
                                text = (if (netSum >= 0) "+" else "-") + " " + formatRupiah(Math.abs(netSum)).replace(",00", ""),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (netSum >= 0) IncomeGreen else ExpenseRed
                            )
                        }
                    }

                    // Transactions on this day
                    items(list) { tx ->
                        TransactionItemRow(tx = tx, onDelete = { viewModel.deleteTransaction(tx.id) })
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) } // navigation cushioning
            }
        }
    }
}

/**
 * Helper to group transactions by calendar days
 */
private fun groupTransactionsByDate(transactions: List<TransactionEntity>): List<Pair<DateGroupHeader, List<TransactionEntity>>> {
    val cal = Calendar.getInstance()
    val today = cal.clone() as Calendar
    val yesterday = cal.clone() as Calendar
    yesterday.add(Calendar.DAY_OF_YEAR, -1)

    val dateFormat = SimpleDateFormat("E, dd MMM yyyy", Locale("id", "ID"))

    // Grouping by standard key
    val rawGrouped = transactions.groupBy { tx ->
        val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
        
        val isToday = txCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                      txCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
                      
        val isYesterday = txCal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                          txCal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)

        when {
            isToday -> "Hari Ini"
            isYesterday -> "Kemarin"
            else -> dateFormat.format(Date(tx.date))
        }
    }

    // Convert to ordered list of DateGroupHeader with net amount
    return rawGrouped.map { (label, list) ->
        val net = list.sumOf { if (it.isIncome) it.amount else -it.amount }
        // We find the date timestamp from the first element of list to help sorting
        val dateRep = list.firstOrNull()?.date ?: 0L
        DateGroupHeader(label, net, dateRep) to list
    }.sortedByDescending { it.first.timestamp }
}

data class DateGroupHeader(
    val headerLabel: String,
    val netAmount: Double,
    val timestamp: Long
)
