package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodels.StoreFlowViewModel

@Composable
fun SettingsScreen(
    viewModel: StoreFlowViewModel
) {
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val syncMessage by viewModel.syncStateMessage.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var workerName by remember { mutableStateOf("Alex Mercer") }
    var workerTerritory by remember { mutableStateOf("Metropolis & Western Territory") }
    var workerId by remember { mutableStateOf("FW-8041") }

    var workerUrl by remember { mutableStateOf("") }
    var authToken by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings & Synchronization",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Field representative preferences and optional Cloudflare Worker sync endpoint.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Worker Profile
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Field Representative Profile", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    OutlinedTextField(
                        value = workerName,
                        onValueChange = { workerName = it },
                        label = { Text("Worker Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = workerTerritory,
                        onValueChange = { workerTerritory = it },
                        label = { Text("Assigned Territory / Region") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = workerId,
                        onValueChange = { workerId = it },
                        label = { Text("Employee Identifier") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        // 2. Synchronization & Cloudflare Worker Endpoint
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Optional Cloudflare Worker Sync", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Text(
                        text = "The local Room SQLite database is the primary source of truth. When no Worker URL is set, the app functions 100% offline. All changes are stored in the local outbox and survive app restarts.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = workerUrl,
                        onValueChange = { workerUrl = it },
                        label = { Text("Worker URL (e.g. https://api.storeflow.workers.dev)") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Optional HTTPS endpoint") }
                    )

                    OutlinedTextField(
                        value = authToken,
                        onValueChange = { authToken = it },
                        label = { Text("Bearer Auth Token (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Queued events: $pendingSyncCount",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (pendingSyncCount > 0) Color(0xFFE65100) else Color(0xFF2E7D32)
                        )

                        Button(
                            onClick = { viewModel.triggerSync(workerUrl.ifBlank { null }, authToken.ifBlank { null }) },
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("manual_sync_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Sync Now")
                        }
                    }

                    syncMessage?.let { msg ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 3. Storage and Diagnostics
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Architecture & Database Diagnostics", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    DiagnosticsRow(label = "Architecture", value = "Offline-First Room (SQLite)")
                    DiagnosticsRow(label = "Database File", value = "storeflow_offline.db")
                    DiagnosticsRow(label = "Schema Version", value = "v1 (Normalized 8 Tables)")
                    DiagnosticsRow(label = "Export Engine", value = "Genuine OpenXML .xlsx Builder")
                    DiagnosticsRow(label = "App Version", value = "StoreFlow v1.0 (Production)")
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun DiagnosticsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
