package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.local.entities.VisitEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.ui.viewmodels.StoreFlowViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VisitsListScreen(
    viewModel: StoreFlowViewModel,
    onResumeActiveVisit: () -> Unit,
    onStartNewVisit: () -> Unit
) {
    val allVisits by viewModel.allVisits.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val allActions by viewModel.allActions.collectAsState()
    val activeVisit by viewModel.activeVisit.collectAsState()

    val storeMap = stores.associateBy { it.id }
    val completedVisits = allVisits.filter { it.status == "COMPLETED" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Active in-progress banner if available
        if (activeVisit != null) {
            val currentStore = storeMap[activeVisit!!.storeId]
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onResumeActiveVisit() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CURRENT VISIT IN PROGRESS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = currentStore?.name ?: "Store",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "${activeVisit!!.durationMinutes} minutes elapsed",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Button(
                            onClick = onResumeActiveVisit,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume")
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Visit History (${completedVisits.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onStartNewVisit,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Visit")
                }
            }
        }

        if (completedVisits.isEmpty()) {
            item {
                EmptyStateView(
                    icon = { Icon(Icons.Default.EventNote, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary) },
                    title = "No visits completed yet",
                    message = "Start your first store visit to inspect merchandise, log actions, and attach photos.",
                    actionButton = {
                        Button(onClick = onStartNewVisit) {
                            Text("Start First Visit")
                        }
                    }
                )
            }
        } else {
            items(completedVisits, key = { it.id }) { visit ->
                val store = storeMap[visit.storeId]
                val visitActions = allActions.filter { it.originatingVisitId == visit.id }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = store?.name ?: "Store",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            StatusBadge(status = visit.status)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${store?.code ?: ""} • ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(visit.startTimestampUtc))}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "⏱️ Duration: ${visit.durationMinutes} min",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "📌 Actions: ${visitActions.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (visit.followUpRequired) {
                                Text(
                                    text = "🗓️ Follow-up: Yes",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }

                        if (visit.notes.isNotBlank() || visit.issuesIdentified.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (visit.notes.isNotBlank()) visit.notes else visit.issuesIdentified,
                                fontSize = 12.sp,
                                maxLines = 2,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (store != null) {
                                        val text = viewModel.shareHelper.buildVisitSummaryText(store, visit, visitActions)
                                        viewModel.shareHelper.shareSummaryText(text)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share Report", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
