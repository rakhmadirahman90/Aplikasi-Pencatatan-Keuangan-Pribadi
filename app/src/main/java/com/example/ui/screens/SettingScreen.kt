package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun SettingScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val spreadsheetUrl by viewModel.spreadsheetUrl.collectAsState()
    val spreadsheetId by viewModel.spreadsheetId.collectAsState()
    val syncMode by viewModel.syncMode.collectAsState()
    val initialBalance by viewModel.initialBalance.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val oauthClientId by viewModel.oauthClientId.collectAsState()
    val oauthClientSecret by viewModel.oauthClientSecret.collectAsState()
    val isAuthorized by viewModel.isAuthorized.collectAsState()

    var urlInput by remember(spreadsheetUrl) { mutableStateOf(spreadsheetUrl) }
    var spreadsheetIdInput by remember(spreadsheetId) { mutableStateOf(spreadsheetId) }
    var balanceInput by remember(initialBalance) { mutableStateOf(initialBalance.toInt().toString()) }
    
    var clientIdInput by remember(oauthClientId) { mutableStateOf(oauthClientId) }
    var clientSecretInput by remember(oauthClientSecret) { mutableStateOf(oauthClientSecret) }
    var authCodeInput by remember { mutableStateOf("") }

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val appsScriptCode = """
function doPost(e) {
  try {
    var data = JSON.parse(e.postData.contents);
    var action = data.action;
    var sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName("Transaksi") || 
                SpreadsheetApp.getActiveSpreadsheet().insertSheet("Transaksi");
    
    // Set headers if new sheet
    if (sheet.getLastRow() === 0) {
      sheet.appendRow(["ID", "Tanggal", "Judul", "Kategori", "Pemasukan", "Pengeluaran", "Catatan"]);
    }
    
    if (action === "sync") {
      var localTxs = data.transactions || [];
      for (var i = 0; i < localTxs.length; i++) {
        var tx = localTxs[i];
        var dateFormatted = new Date(tx.date);
        var tgl = dateFormatted.getFullYear() + "-" + (dateFormatted.getMonth()+1) + "-" + dateFormatted.getDate();
        sheet.appendRow([
          tx.id,
          tgl,
          tx.title,
          tx.category,
          tx.isIncome ? tx.amount : 0,
          tx.isIncome ? 0 : tx.amount,
          tx.note
        ]);
      }
    }
    
    // Retrieve all data to sync back
    var rows = sheet.getDataRange().getValues();
    var allTransactions = [];
    for (var i = 1; i < rows.length; i++) {
      var row = rows[i];
      var isInc = parseFloat(row[4] || 0) > 0;
      var amt = isInc ? parseFloat(row[4]) : parseFloat(row[5] || 0);
      var dt = new Date(row[1]).getTime() || new Date().getTime();
      allTransactions.push({
        id: parseInt(row[0]) || i,
        title: row[2] || "",
        category: row[3] || "",
        amount: amt,
        isIncome: isInc,
        date: dt,
        note: row[6] || ""
      });
    }
    
    return ContentService.createTextOutput(JSON.stringify({
      success: true,
      message: "Synced " + localTxs.length + " transactions successfully.",
      transactions: allTransactions
    })).setMimeType(ContentService.MimeType.JSON);
    
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      success: false,
      message: err.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}
""".trimIndent()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // 1. HEADER TITLE
        item {
            Text(
                "Pengaturan & Sinkronisasi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // 2. SYNC MODE TABS (Apps Script vs Direct Sheets API)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Metode Sinkronisasi",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.saveSyncMode("AppsScript") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (syncMode == "AppsScript") TealPrimary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CloudQueue, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apps Script", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.saveSyncMode("SheetsAPI") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (syncMode == "SheetsAPI") TealPrimary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sheets API", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. CONFIGURATION FOR SELECTED SYNC MODE
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (syncMode == "SheetsAPI") "Konfigurasi Google Sheets API" else "Konfigurasi Apps Script",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (syncMode == "AppsScript") {
                        // URL Input Field for Apps Script mode
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("URL Google Apps Script Web App") },
                            placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("spreadsheet_url_input"),
                            trailingIcon = {
                                if (urlInput.isNotEmpty()) {
                                    IconButton(onClick = { urlInput = "" }) {
                                        Icon(Icons.Default.Clear, "Clear")
                                    }
                                }
                            }
                        )
                    } else {
                        // Spreadsheet ID and OAuth inputs for Google Sheets API mode
                        OutlinedTextField(
                            value = spreadsheetIdInput,
                            onValueChange = { spreadsheetIdInput = it },
                            label = { Text("Google Spreadsheet ID") },
                            placeholder = { Text("1a2b3c4d5e...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (spreadsheetIdInput.isNotEmpty()) {
                                    IconButton(onClick = { spreadsheetIdInput = "" }) {
                                        Icon(Icons.Default.Clear, "Clear")
                                    }
                                }
                            }
                        )

                        OutlinedTextField(
                            value = clientIdInput,
                            onValueChange = { clientIdInput = it },
                            label = { Text("OAuth Client ID") },
                            placeholder = { Text("Enter Google Client ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = clientSecretInput,
                            onValueChange = { clientSecretInput = it },
                            label = { Text("OAuth Client Secret") },
                            placeholder = { Text("Enter Google Client Secret") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // OAuth Status Indicator & Actions
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Status Otorisasi:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isAuthorized) MintGreen else Color.Red)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAuthorized) "Terhubung" else "Belum Terhubung",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAuthorized) MintGreen else Color.Red
                                )
                            }
                        }

                        if (!isAuthorized) {
                            // Step to request authorization code
                            Button(
                                onClick = {
                                    if (clientIdInput.isEmpty()) {
                                        Toast.makeText(context, "Harap masukkan OAuth Client ID terlebih dahulu", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.saveOAuthCredentials(clientIdInput, clientSecretInput)
                                    val authUrl = viewModel.googleSheetsService.getAuthorizationUrl(clientIdInput)
                                    uriHandler.openUri(authUrl)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Buka Otorisasi Google", fontWeight = FontWeight.Bold)
                            }

                            // Step to enter auth code
                            OutlinedTextField(
                                value = authCodeInput,
                                onValueChange = { authCodeInput = it },
                                label = { Text("Kode Otorisasi (Paste di sini)") },
                                placeholder = { Text("4/1AX4Xf...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (clientIdInput.isEmpty() || clientSecretInput.isEmpty() || authCodeInput.isEmpty()) {
                                        Toast.makeText(context, "Harap isi Client ID, Secret, dan Kode Otorisasi", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.exchangeCodeForToken(clientIdInput, clientSecretInput, authCodeInput) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Selesaikan Koneksi", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // Option to disconnect
                            Button(
                                onClick = {
                                    viewModel.disconnectSheetsAPI()
                                    Toast.makeText(context, "Berhasil memutuskan Google Sheets API", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                            ) {
                                Icon(Icons.Default.LinkOff, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Putuskan Koneksi", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Starting Balance Config (Common Settings)
                    OutlinedTextField(
                        value = balanceInput,
                        onValueChange = { if (it.all { c -> c.isDigit() }) balanceInput = it },
                        label = { Text("Saldo Awal Rekening (Rupiah)") },
                        prefix = { Text("Rp ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dark theme toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = "Tema Gelap", tint = TealPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Mode Tema Gelap", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { viewModel.saveDarkTheme(it) }
                        )
                    }

                    // Save settings button
                    Button(
                        onClick = {
                            if (syncMode == "AppsScript") {
                                viewModel.saveSpreadsheetUrl(urlInput)
                            } else {
                                viewModel.saveSpreadsheetId(spreadsheetIdInput)
                                viewModel.saveOAuthCredentials(clientIdInput, clientSecretInput)
                            }
                            val bal = balanceInput.toDoubleOrNull() ?: 0.0
                            viewModel.saveInitialBalance(bal)
                            Toast.makeText(context, "Pengaturan berhasil disimpan!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Text("Simpan Konfigurasi", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. TUTORIAL SETUP GUIDE CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoStories, contentDescription = "Manual", tint = ColorTransport)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Panduan Integrasi Google Sheets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    TutorialStep(1, "Buat sebuah Google Spreadsheet baru di Google Drive Anda.")
                    TutorialStep(2, "Buka menu **Ekstensi** -> **Apps Script** di bagian atas halaman Google Spreadsheet.")
                    TutorialStep(3, "Hapus semua kode default di layar Apps Script, lalu salin kode script lengkap di bawah.")
                    TutorialStep(4, "Klik tombol **Terapkan (Deploy)** -> **Penerapan baru (New deployment)**.")
                    TutorialStep(5, "Pilih jenis penerapan: **Aplikasi Web (Web App)**.")
                    TutorialStep(6, "Konfigurasi:\n- Jalankan sebagai: **Saya (Me)**\n- Yang memiliki akses: **Siapa saja (Anyone)**.")
                    TutorialStep(7, "Klik **Terapkan (Deploy)**, setujui izin keamanan Google, lalu salin **URL Aplikasi Web** yang diberikan.")
                    TutorialStep(8, "Tempelkan URL tersebut ke kolom di atas lalu klik **Simpan Konfigurasi**. Selesai!")
                }
            }
        }

        // 4. THE APPS SCRIPT CODE VIEWER & COPY BUTTON
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Kode Google Apps Script",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        // Copy Button action
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("AppsScriptCode", appsScriptCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Kode disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MintGreen)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Salin Kode", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Scrollable code container box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E2826))
                            .padding(10.dp)
                    ) {
                        SelectionContainer {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    Text(
                                        text = appsScriptCode,
                                        color = Color(0xFFC5E1A5),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun TutorialStep(
    stepNumber: Int,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(TealPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber.toString(),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            modifier = Modifier.weight(1f)
        )
    }
}
