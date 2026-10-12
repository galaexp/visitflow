package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodels.StoreFlowViewModel
import java.io.File

@Composable
fun ReportsScreen(
    viewModel: StoreFlowViewModel
) {
    val context = LocalContext.current
    val stores by viewModel.stores.collectAsState()
    val allVisits by viewModel.allVisits.collectAsState()
    val allActions by viewModel.allActions.collectAsState()

    var isExporting by remember { mutableStateOf(false) }
    var exportedFile by remember { mutableStateOf<File?>(null) }
    var showWhatsAppPreviewDialog by remember { mutableStateOf(false) }
    var whatsAppPreviewText by remember { mutableStateOf("") }
    var backupStatusMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Reports & Offline Data Export",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Generate authentic multi-sheet Excel (.xlsx) workbooks and share summaries via Android Sharesheet 100% offline.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Summary Statistics Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Local Database Summary",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ReportStat(label = "Stores", value = "${stores.size}")
                        ReportStat(label = "Total Visits", value = "${allVisits.size}")
                        ReportStat(label = "Actions Logged", value = "${allActions.size}")
                        ReportStat(label = "Open Actions", value = "${allActions.count { it.status != "COMPLETED" }}")
                    }
                }
            }
        }

        // 1. Full Excel Export Card
        item {
            ReportActionCard(
                title = "Export Master Excel Workbook (.xlsx)",
                subtitle = "Includes 6 sheets: Visit Summary, Visit Details, Action Items, Photo Index, Store Directory, and Scheduled Visits.",
                icon = Icons.Default.FileDownload,
                buttonText = if (isExporting) "Generating..." else "Export Excel (.xlsx)",
                isLoading = isExporting,
                onClick = {
                    isExporting = true
                    viewModel.exportComprehensiveExcel { file ->
                        isExporting = false
                        exportedFile = file
                        viewModel.shareHelper.shareFile(
                            file = file,
                            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            subject = "StoreFlow Field Report"
                        )
                    }
                }
            )
        }

        // 2. WhatsApp Visit Report Card
        item {
            ReportActionCard(
                title = "Share Visit Summary (WhatsApp / Messaging)",
                subtitle = "Format observations, high-priority actions, and follow-up dates as clean markdown text for store managers or supervisors.",
                icon = Icons.Default.Share,
                buttonText = "Preview & Share Summary",
                isLoading = false,
                onClick = {
                    val lastCompleted = allVisits.filter { it.status == "COMPLETED" }.maxByOrNull { it.startTimestampUtc }
                    if (lastCompleted != null) {
                        val store = stores.find { it.id == lastCompleted.storeId }
                        val visitActions = allActions.filter { it.originatingVisitId == lastCompleted.id }
                        if (store != null) {
                            whatsAppPreviewText = viewModel.shareHelper.buildVisitSummaryText(store, lastCompleted, visitActions)
                            showWhatsAppPreviewDialog = true
                        }
                    } else if (stores.isNotEmpty()) {
                        // Sample preview if no completed visits yet
                        whatsAppPreviewText = "STORE VISIT REPORT\n\nStore: ${stores.first().name}\nCode: ${stores.first().code}\nVisit Date: Today\n\nOBSERVATIONS:\nRoutine inspection completed without issues.\n\nACTIONS REQUIRED:\n1. [NORMAL] Check shelf facings (Due in 7 days)\n\nFOLLOW-UP:\nNext routine visit in 7 days."
                        showWhatsAppPreviewDialog = true
                    } else {
                        Toast.makeText(context, "No visit records available to share yet.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // 3. Backup and Restore
        item {
            ReportActionCard(
                title = "Complete Local Backup & Restore",
                subtitle = "Export full database records as a standalone JSON backup archive to survive device upgrades or clearing cache.",
                icon = Icons.Default.Backup,
                buttonText = "Create Offline Backup",
                isLoading = false,
                onClick = {
                    viewModel.createBackup { backupFile ->
                        backupStatusMessage = "Backup generated at: ${backupFile.name} (${backupFile.length() / 1024} KB)"
                        viewModel.shareHelper.shareFile(
                            file = backupFile,
                            mimeType = "application/json",
                            subject = "StoreFlow Offline Backup"
                        )
                    }
                }
            )
        }

        backupStatusMessage?.let { msg ->
            item {
                Surface(
                    color = Color(0xFF2E7D32).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✅ $msg",
                        color = Color(0xFF2E7D32),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // WhatsApp Message Preview & Edit Dialog
    if (showWhatsAppPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showWhatsAppPreviewDialog = false },
            title = { Text("Preview Report Text") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "You can edit this message before launching the Android Sharesheet:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = whatsAppPreviewText,
                        onValueChange = { whatsAppPreviewText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWhatsAppPreviewDialog = false
                        viewModel.shareHelper.shareSummaryText(whatsAppPreviewText)
                    }
                ) {
                    Text("Share Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWhatsAppPreviewDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ReportStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ReportActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    buttonText: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onClick,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(buttonText)
            }
        }
    }
}
