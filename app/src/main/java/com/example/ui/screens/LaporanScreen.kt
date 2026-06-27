package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.util.*

@Composable
fun LaporanScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val filteredTransactions by viewModel.filteredTransactions.collectAsState(initial = emptyList())
    val categoryExpenses by viewModel.categoryExpenses.collectAsState(initial = emptyList())

    // Convert categoryExpenses summaries into ChartSlice objects for DonutChart
    val slices = remember(categoryExpenses) {
        val colorsMap = mapOf(
            "Rumah" to ColorRumah,
            "Belanja" to ColorBelanja,
            "Makanan" to ColorMakanan,
            "Tagihan" to ColorTagihan,
            "Transport" to ColorTransport,
            "Hiburan" to ColorHiburan,
            "Pendidikan" to ColorPendidikan,
            "Kesehatan" to ColorKesehatan,
            "Lainnya" to ColorLainnya
        )

        categoryExpenses.map {
            ChartSlice(
                label = it.category,
                value = it.amount,
                percentage = it.percentage,
                color = colorsMap[it.category] ?: ColorLainnya
            )
        }
    }

    // Prepare weekly trends from transaction data (Weeks 1 to 5)
    val weeklyCategoryData = remember(filteredTransactions) {
        computeWeeklyCategoryData(filteredTransactions)
    }

    val weeklyCashFlowData = remember(filteredTransactions) {
        computeWeeklyCashFlowData(filteredTransactions)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // 1. DONUT CHART CARD (Pengeluaran per Kategori)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "✔ Pengeluaran per Kategori",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (slices.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Belum ada pengeluaran di bulan ini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        DonutChart(slices = slices)
                    }
                }
            }
        }

        // 2. KATEGORI TERATAS (Progress spending lists)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star Icon",
                            tint = ColorTransport,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Kategori Teratas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (categoryExpenses.isEmpty()) {
                        Text(
                            "Belum ada data pengeluaran.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        categoryExpenses.take(5).forEach { summary ->
                            val catColor = when (summary.category) {
                                "Rumah" -> ColorRumah
                                "Belanja" -> ColorBelanja
                                "Makanan" -> ColorMakanan
                                "Tagihan" -> ColorTagihan
                                "Transport" -> ColorTransport
                                "Hiburan" -> ColorHiburan
                                "Pendidikan" -> ColorPendidikan
                                "Kesehatan" -> ColorKesehatan
                                "Gaji" -> ColorGaji
                                "Freelance" -> ColorFreelance
                                else -> ColorLainnya
                            }

                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = summary.category,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row {
                                        Text(
                                            text = formatRupiah(summary.amount).replace(",00", ""),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${(summary.percentage * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { summary.percentage },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = catColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. ALIRAN DANA SANKEY CARD
        item {
            val income = filteredTransactions.filter { it.isIncome }.sumOf { it.amount }
            val expense = filteredTransactions.filter { !it.isIncome }.sumOf { it.amount }
            val topCategory = categoryExpenses.firstOrNull()?.category ?: "Lainnya"
            val topCategoryAmount = categoryExpenses.firstOrNull()?.amount ?: 0.0

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Aliran Dana",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SankeyFlowChart(
                        incomeSource = if (income > 0) "Pendapatan" else "Tanpa Dana",
                        incomeAmount = income,
                        topExpenseCategory = topCategory,
                        topExpenseAmount = topCategoryAmount
                    )
                }
            }
        }

        // 4. CLICKABLE LINE CHART (Tren per Kategori)
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📊 Tren per Kategori",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val colorsMap = mapOf(
                        "Rumah" to ColorRumah,
                        "Belanja" to ColorBelanja,
                        "Makanan" to ColorMakanan,
                        "Tagihan" to ColorTagihan,
                        "Transport" to ColorTransport
                    )

                    MultiLineChart(
                        categoryData = weeklyCategoryData,
                        colors = colorsMap
                    )
                }
            }
        }

        // 5. DUAL BAR & LINE CASHFLOW TREND
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📈 Tren Arus Kas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    CashFlowTrendChart(
                        incomeData = weeklyCashFlowData.first,
                        expenseData = weeklyCashFlowData.second
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) } // Navigation Bar padding
    }
}

/**
 * Computes weekly aggregates (Week 1-5) for line trends
 */
private fun computeWeeklyCategoryData(txs: List<TransactionEntity>): Map<String, List<Double>> {
    val result = mutableMapOf(
        "Rumah" to mutableListOf(0.0, 0.0, 0.0, 0.0, 0.0),
        "Belanja" to mutableListOf(0.0, 0.0, 0.0, 0.0, 0.0),
        "Makanan" to mutableListOf(0.0, 0.0, 0.0, 0.0, 0.0),
        "Tagihan" to mutableListOf(0.0, 0.0, 0.0, 0.0, 0.0),
        "Transport" to mutableListOf(0.0, 0.0, 0.0, 0.0, 0.0)
    )

    txs.filter { !it.isIncome }.forEach { tx ->
        val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        // Divide into 5 weeks roughly
        val weekIndex = ((day - 1) / 6).coerceIn(0, 4)

        val list = result[tx.category]
        if (list != null) {
            list[weekIndex] = list[weekIndex] + tx.amount
        }
    }

    return result
}

/**
 * Computes weekly aggregates (Week 1-5) for dual Cash Flow bar/lines
 */
private fun computeWeeklyCashFlowData(txs: List<TransactionEntity>): Pair<List<Double>, List<Double>> {
    val incomeList = mutableListOf(0.0, 0.0, 0.0, 0.0, 0.0)
    val expenseList = mutableListOf(0.0, 0.0, 0.0, 0.0, 0.0)

    txs.forEach { tx ->
        val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val weekIndex = ((day - 1) / 6).coerceIn(0, 4)

        if (tx.isIncome) {
            incomeList[weekIndex] = incomeList[weekIndex] + tx.amount
        } else {
            expenseList[weekIndex] = expenseList[weekIndex] + tx.amount
        }
    }

    return Pair(incomeList, expenseList)
}
