package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BudgetEntity
import com.example.data.model.RecurringEntity
import com.example.data.model.TransactionEntity
import com.example.ui.components.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BerandaScreen(
    viewModel: FinanceViewModel,
    onSeeAllTransactions: () -> Unit,
    onOpenAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthlySummary by viewModel.monthlySummary.collectAsState()
    val categoryExpenses by viewModel.categoryExpenses.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val budgets by viewModel.allBudgets.collectAsState()
    val recurring by viewModel.allRecurring.collectAsState()

    val currentMonth by viewModel.currentMonth.collectAsState()
    val currentYear by viewModel.currentYear.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()

    val monthNames = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    // Dialog sheets states
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showRecurringDialog by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Space under top safe margins
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // 1. TOP CARD: Total Balance & Stats with Mini Graph overlay
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .testTag("total_balance_card"),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Mini Canvas graph drawn inside background with subtle onPrimaryContainer alpha lines
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.BottomEnd)
                    ) {
                        val path = Path()
                        val w = size.width
                        val h = size.height
                        path.moveTo(w * 0.5f, h * 0.7f)
                        path.cubicTo(
                            w * 0.65f, h * 0.65f,
                            w * 0.8f, h * 0.4f,
                            w, h * 0.3f
                        )
                        drawPath(
                            path = path,
                            color = TealPrimary.copy(alpha = 0.12f),
                            style = Stroke(width = 8f)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Saldo (IDR)",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatRupiah(monthlySummary.totalBalance),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Wallet Icon",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                            thickness = 1.dp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = "Pemasukan",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Pemasukan",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = formatRupiah(monthlySummary.monthlyIncome),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingDown,
                                        contentDescription = "Pengeluaran",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Pengeluaran",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = formatRupiah(monthlySummary.monthlyExpense),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Month Selector & Synchronization
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(onClick = { viewModel.prevMonth() }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Bulan Sebelumnya")
                    }
                    Text(
                        text = "${monthNames[currentMonth]} $currentYear",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Bulan Selanjutnya")
                    }
                }

                // Sync action trigger
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (syncStatus == "Sinkronisasi...") MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                        .clickable { viewModel.triggerSync() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (syncStatus == "Sinkronisasi...") "Syncing" else "Sync Sheets",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 3. Sync Notification message overlay if active
        if (syncMessage.isNotEmpty()) {
            item {
                Surface(
                    color = if (syncStatus == "Gagal") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (syncStatus == "Gagal") Icons.Default.ErrorOutline else Icons.Default.Info,
                            contentDescription = "Sync status icon",
                            tint = if (syncStatus == "Gagal") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = syncMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (syncStatus == "Gagal") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 4. Recommendation banner
        item {
            val topCategory = categoryExpenses.firstOrNull()
            if (topCategory != null && monthlySummary.monthlyExpense > 0) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Tip",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        val percent = (topCategory.percentage * 100).toInt()
                        Text(
                            text = "Pengeluaran terbesarmu ($percent%) ada di kategori ${topCategory.category}. Jaga terus anggaranmu!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 5. QUICK MENU
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Menu Layanan",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickMenuButton(
                        title = "Anggaran",
                        icon = Icons.Default.TrackChanges,
                        color = ColorRumah,
                        onClick = { showBudgetDialog = true }
                    )
                    QuickMenuButton(
                        title = "Berulang",
                        icon = Icons.Default.HistoryToggleOff,
                        color = ColorBelanja,
                        onClick = { showRecurringDialog = true }
                    )
                    QuickMenuButton(
                        title = "Kategori",
                        icon = Icons.Default.Widgets,
                        color = ColorTagihan,
                        onClick = { showCategorySheet = true }
                    )
                    QuickMenuButton(
                        title = "Tambah",
                        icon = Icons.Default.Add,
                        color = ColorGaji,
                        onClick = onOpenAddTransaction
                    )
                }
            }
        }

        // 6. BUDGETS SECTION
        if (budgets.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Anggaran Aktif",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Kelola",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { showBudgetDialog = true }
                    )
                }
            }

            items(budgets) { budget ->
                val spent = categoryExpenses.find { it.category == budget.category }?.amount ?: 0.0
                val ratio = (spent / budget.amount).toFloat().coerceIn(0f, 1f)
                val isOverBudget = spent > budget.amount

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = budget.category,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${formatRupiah(spent).replace(",00","")} / ${formatRupiah(budget.amount).replace(",00","")}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isOverBudget) ExpenseRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = if (isOverBudget) ExpenseRed else ColorBelanja,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        // 7. LATEST TRANSACTIONS
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaksi Terakhir",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Lihat semua",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onSeeAllTransactions() }
                )
            }
        }

        val latestTxs = allTransactions.take(4)
        if (latestTxs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada transaksi disimpan. Tekan '+' untuk menambah.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            items(latestTxs) { tx ->
                TransactionItemRow(tx = tx, onDelete = { viewModel.deleteTransaction(tx.id) })
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) } // Spacer for navigation bar cushion
    }

    // --- DIALOG POPUPS IMPLEMENTATIONS ---

    // 1. Manage Budgets Dialog
    if (showBudgetDialog) {
        ManageBudgetsDialog(
            budgets = budgets,
            onDismiss = { showBudgetDialog = false },
            onSaveBudget = { cat, amt -> viewModel.addBudget(cat, amt) },
            onDeleteBudget = { cat -> viewModel.deleteBudget(cat) }
        )
    }

    // 2. Manage Recurring Dialog
    if (showRecurringDialog) {
        ManageRecurringDialog(
            recurringList = recurring,
            onDismiss = { showRecurringDialog = false },
            onAddRecurring = { title, amt, isInc, cat, interval ->
                viewModel.addRecurring(title, amt, isInc, cat, interval, System.currentTimeMillis())
            },
            onDeleteRecurring = { id -> viewModel.deleteRecurring(id) }
        )
    }

    // 3. Category Viewer BottomSheet-style Dialog
    if (showCategorySheet) {
        CategoryViewerDialog(
            onDismiss = { showCategorySheet = false }
        )
    }
}

@Composable
fun QuickMenuButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun TransactionItemRow(
    tx: TransactionEntity,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val catColor = when (tx.category) {
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

    val catIcon = when (tx.category) {
        "Rumah" -> Icons.Default.Home
        "Belanja" -> Icons.Default.ShoppingBag
        "Makanan" -> Icons.Default.Restaurant
        "Tagihan" -> Icons.Default.Receipt
        "Transport" -> Icons.Default.DirectionsCar
        "Hiburan" -> Icons.Default.Movie
        "Pendidikan" -> Icons.Default.School
        "Kesehatan" -> Icons.Default.LocalHospital
        "Gaji" -> Icons.Default.Payments
        "Freelance" -> Icons.Default.MonetizationOn
        else -> Icons.Default.Category
    }

    val dateStr = SimpleDateFormat("dd/MM", Locale("id", "ID")).format(Date(tx.date))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDeleteConfirm = true },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon with Colored background circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(catColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = catIcon,
                    contentDescription = tx.category,
                    tint = catColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & category/date info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${tx.category} • $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // Amount (+/-)
            Text(
                text = "${if (tx.isIncome) "+" else "-"} ${formatRupiah(tx.amount).replace(",00", "")}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (tx.isIncome) IncomeGreen else ExpenseRed
            )
        }
    }

    // Delete transaction confirmation dialog on click
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Transaksi?") },
            text = { Text("Apakah Anda yakin ingin menghapus catatan transaksi '${tx.title}'? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Hapus", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

/**
 * Dialog for Managing Budgets
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageBudgetsDialog(
    budgets: List<BudgetEntity>,
    onDismiss: () -> Unit,
    onSaveBudget: (category: String, amount: Double) -> Unit,
    onDeleteBudget: (category: String) -> Unit
) {
    var budgetCategory by remember { mutableStateOf("Makanan") }
    var budgetAmountStr by remember { mutableStateOf("") }
    val categories = listOf("Makanan", "Belanja", "Transport", "Tagihan", "Rumah", "Hiburan", "Pendidikan", "Kesehatan", "Lainnya")

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Atur Anggaran Kategori",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Select category
            Text("Pilih Kategori", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(budgetCategory)
                    Icon(Icons.Default.ArrowDropDown, "Dropdown")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                budgetCategory = cat
                                expanded = false
                                val existing = budgets.find { it.category == cat }
                                if (existing != null) {
                                    budgetAmountStr = existing.amount.toInt().toString()
                                } else {
                                    budgetAmountStr = ""
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Enter Budget Amount
            OutlinedTextField(
                value = budgetAmountStr,
                onValueChange = { if (it.all { c -> c.isDigit() }) budgetAmountStr = it },
                label = { Text("Jumlah Anggaran Bulanan") },
                prefix = { Text("Rp ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Delete button
                OutlinedButton(
                    onClick = {
                        onDeleteBudget(budgetCategory)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                    border = BorderStroke(1.dp, ExpenseRed)
                ) {
                    Text("Hapus")
                }

                // Save button
                Button(
                    onClick = {
                        val amt = budgetAmountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0) {
                            onSaveBudget(budgetCategory, amt)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text("Simpan Anggaran")
                }
            }

            // List of active budgets
            if (budgets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Daftar Anggaran Saat Ini:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.height(120.dp).padding(top = 4.dp)) {
                    items(budgets) { budget ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${budget.category}:", style = MaterialTheme.typography.bodySmall)
                            Text(formatRupiah(budget.amount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog for managing recurring payments
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageRecurringDialog(
    recurringList: List<RecurringEntity>,
    onDismiss: () -> Unit,
    onAddRecurring: (title: String, amount: Double, isIncome: Boolean, category: String, interval: String) -> Unit,
    onDeleteRecurring: (id: Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tagihan") }
    var interval by remember { mutableStateOf("Bulanan") }

    val categories = listOf("Rumah", "Tagihan", "Hiburan", "Belanja", "Makanan", "Lainnya")
    val intervals = listOf("Harian", "Mingguan", "Bulanan")

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Atur Pengeluaran Berulang",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Nama Layanan/Sewa") },
                placeholder = { Text("Netflix, Kos, Listrik, dsb.") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { if (it.all { c -> c.isDigit() }) amountStr = it },
                    label = { Text("Jumlah") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.2f)
                )

                // Interval Dropdown
                var expandedInterval by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                    OutlinedButton(onClick = { expandedInterval = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(interval, style = MaterialTheme.typography.labelSmall)
                    }
                    DropdownMenu(expanded = expandedInterval, onDismissRequest = { expandedInterval = false }) {
                        intervals.forEach { iv ->
                            DropdownMenuItem(text = { Text(iv) }, onClick = { interval = iv; expandedInterval = false })
                        }
                    }
                }
            }

            // Category Selection
            var expandedCat by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { expandedCat = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Kategori: $category")
                }
                DropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                    categories.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; expandedCat = false })
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (title.isNotEmpty() && amt > 0.0) {
                        onAddRecurring(title, amt, false, category, interval)
                        title = ""
                        amountStr = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotEmpty() && (amountStr.toDoubleOrNull() ?: 0.0) > 0.0
            ) {
                Text("Tambah Tagihan Berulang")
            }

            if (recurringList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Layanan Berulang Terdaftar:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.height(120.dp).padding(top = 4.dp)) {
                    items(recurringList) { rec ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(rec.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text("${rec.category} • ${rec.interval}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(formatRupiah(rec.amount).replace(",00",""), style = MaterialTheme.typography.bodySmall, color = ExpenseRed)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus",
                                    tint = ExpenseRed,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { onDeleteRecurring(rec.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Dialog to view list of Category colors and summaries
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryViewerDialog(
    onDismiss: () -> Unit
) {
    val items = listOf(
        CategoryItem("Gaji", Icons.Default.Payments, ColorGaji),
        CategoryItem("Freelance", Icons.Default.MonetizationOn, ColorFreelance),
        CategoryItem("Rumah", Icons.Default.Home, ColorRumah),
        CategoryItem("Belanja", Icons.Default.ShoppingBag, ColorBelanja),
        CategoryItem("Makanan", Icons.Default.Restaurant, ColorMakanan),
        CategoryItem("Tagihan", Icons.Default.Receipt, ColorTagihan),
        CategoryItem("Transport", Icons.Default.DirectionsCar, ColorTransport),
        CategoryItem("Hiburan", Icons.Default.Movie, ColorHiburan),
        CategoryItem("Pendidikan", Icons.Default.School, ColorPendidikan),
        CategoryItem("Kesehatan", Icons.Default.LocalHospital, ColorKesehatan),
        CategoryItem("Lainnya", Icons.Default.Category, ColorLainnya)
    )

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Daftar Kategori Finansial",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(item.color),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(item.icon, contentDescription = item.name, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(item.name, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Tutup")
            }
        }
    }
}
