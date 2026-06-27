package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.MintGreen
import com.example.ui.viewmodel.FinanceViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FinanceViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                var activeTab by remember { mutableStateOf(0) } // 0: Beranda, 1: Transaksi, 2: Laporan, 3: Setting
                var showAddDialog by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            topBar = {
                                CenterAlignedTopAppBar(
                                    title = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AccountBalanceWallet,
                                                    contentDescription = "Wallet Icon",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Text(
                                                text = "KasPribadi",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    actions = {
                                        val spreadsheetUrl by viewModel.spreadsheetUrl.collectAsState()
                                        if (spreadsheetUrl.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(end = 12.dp)
                                                    .background(Color(0xFFD1E1FF), RoundedCornerShape(16.dp))
                                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Sync,
                                                        contentDescription = "Syncing Status",
                                                        tint = Color(0xFF001D36),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = "Sheets Active",
                                                        color = Color(0xFF001D36),
                                                        fontSize = 10.sp,
                                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.background
                                    )
                                )
                            },
                            bottomBar = {
                                NavigationBar(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                        .height(80.dp),
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 8.dp
                                ) {
                                    // Beranda Tab
                                    NavigationBarItem(
                                        selected = activeTab == 0,
                                        onClick = { activeTab = 0 },
                                        icon = {
                                            Icon(
                                                imageVector = if (activeTab == 0) Icons.Default.Home else Icons.Outlined.Home,
                                                contentDescription = "Beranda"
                                            )
                                        },
                                        label = { Text("Beranda", fontSize = 11.sp, fontWeight = if (activeTab == 0) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal) }
                                    )

                                    // Transaksi Tab
                                    NavigationBarItem(
                                        selected = activeTab == 1,
                                        onClick = { activeTab = 1 },
                                        icon = {
                                            Icon(
                                                imageVector = if (activeTab == 1) Icons.Default.ListAlt else Icons.Outlined.ListAlt,
                                                contentDescription = "Transaksi"
                                            )
                                        },
                                        label = { Text("Transaksi", fontSize = 11.sp, fontWeight = if (activeTab == 1) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal) }
                                    )

                                    // Placeholder for center overlapping FAB
                                    Spacer(modifier = Modifier.width(60.dp))

                                    // Laporan Tab
                                    NavigationBarItem(
                                        selected = activeTab == 2,
                                        onClick = { activeTab = 2 },
                                        icon = {
                                            Icon(
                                                imageVector = if (activeTab == 2) Icons.Default.BarChart else Icons.Outlined.BarChart,
                                                contentDescription = "Laporan"
                                            )
                                        },
                                        label = { Text("Laporan", fontSize = 11.sp, fontWeight = if (activeTab == 2) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal) }
                                    )

                                    // Setting Tab
                                    NavigationBarItem(
                                        selected = activeTab == 3,
                                        onClick = { activeTab = 3 },
                                        icon = {
                                            Icon(
                                                imageVector = if (activeTab == 3) Icons.Default.Settings else Icons.Outlined.Settings,
                                                contentDescription = "Setting"
                                            )
                                        },
                                        label = { Text("Setting", fontSize = 11.sp, fontWeight = if (activeTab == 3) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal) }
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                when (activeTab) {
                                    0 -> BerandaScreen(
                                        viewModel = viewModel,
                                        onSeeAllTransactions = { activeTab = 1 },
                                        onOpenAddTransaction = { showAddDialog = true }
                                    )
                                    1 -> TransaksiScreen(viewModel = viewModel)
                                    2 -> LaporanScreen(viewModel = viewModel)
                                    3 -> SettingScreen(viewModel = viewModel)
                                }
                            }
                        }

                        // Centered FAB button aligned and overlapping bottom bar beautifully
                        FloatingActionButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(bottom = 32.dp) // Offset upwards to overlap cleanly
                                .size(64.dp)
                                .testTag("fab_add_transaction"),
                            shape = CircleShape,
                            containerColor = MintGreen,
                            contentColor = Color.White,
                            elevation = FloatingActionButtonDefaults.elevation(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tambah Transaksi",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Add Transaction quick dialog overlay
                if (showAddDialog) {
                    TambahTransaksiDialog(
                        onDismiss = { showAddDialog = false },
                        onSave = { title, amt, isInc, cat, date, note ->
                            viewModel.addTransaction(title, amt, isInc, cat, date, note)
                        }
                    )
                }
            }
        }
    }
}
