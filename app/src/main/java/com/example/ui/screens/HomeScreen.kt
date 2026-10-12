package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.OfflineSyncBanner
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.viewmodels.StoreFlowViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: StoreFlowViewModel,
    onNavigateToStores: () -> Unit,
    onNavigateToActiveVisit: () -> Unit,
    onNavigateToActions: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    onNavigateToReports: () -> Unit,
    onStartVisitForStore: (String) -> Unit
) {
    val activeVisit by viewModel.activeVisit.collectAsState()
    val visitsToday by viewModel.visitsTodayCount.collectAsState()
    val openActions by viewModel.openActionsCount.collectAsState()
    val overdueActions by viewModel.overdueActionsCount.collectAsState()
    val upcomingSchedules by viewModel.upcomingSchedulesCount.collectAsState()
    val pendingSync by viewModel.pendingSyncCount.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val allActions by viewModel.allActions.collectAsState()
    val allSchedules by viewModel.allSchedules.collectAsState()

    val overdueList = allActions.filter { it.isOverdue }.take(3)
    val todaySchedules = allSchedules.filter { !it.isCompleted }.take(3)
    val storeMap = stores.associateBy { it.id }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            OfflineSyncBanner(
                pendingCount = pendingSync,
                onSyncClick = { viewModel.triggerSync(null, null) }
            )
        }

        // Active In-Progress Visit Prompt
        if (activeVisit != null) {
            val visit = activeVisit!!
            val store = storeMap[visit.storeId]
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_visit_card")
                        .clickable { onNavigateToActiveVisit() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE65100))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "VISIT IN PROGRESS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = "${visit.durationMinutes} min elapsed",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = store?.name ?: "Current Store",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${store?.code ?: ""} • ${store?.city ?: ""}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onNavigateToActiveVisit() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("resume_visit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume Visit & Checklists")
                        }
                    }
                }
            }
        }

        // Metrics Grid (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "Visits Today",
                        count = "$visitsToday",
                        icon = Icons.Default.CheckCircle,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToActiveVisit
                    )
                    KpiCard(
                        title = "Open Actions",
                        count = "$openActions",
                        icon = Icons.Default.Assignment,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToActions
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "Overdue Actions",
                        count = "$overdueActions",
                        icon = Icons.Default.Warning,
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToActions
                    )
                    KpiCard(
                        title = "Scheduled Visits",
                        count = "$upcomingSchedules",
                        icon = Icons.Default.CalendarToday,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSchedule
                    )
                }
            }
        }

        // Quick Action Buttons
        item {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNavigateToStores,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("start_visit_quick_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select Store", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onNavigateToReports,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_excel_quick_btn")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Report", fontSize = 13.sp)
                }
            }
        }

        // Overdue Action Items Alert Section
        if (overdueList.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Attention Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                    }
                    Text(
                        text = "View All",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateToActions() }
                    )
                }
            }
            items(overdueList.size) { idx ->
                val act = overdueList[idx]
                val store = storeMap[act.storeId]
                ActionItemSummaryCard(
                    action = act,
                    storeName = store?.name ?: "Store",
                    onClick = onNavigateToActions
                )
            }
        }

        // Scheduled Visits Preview
        if (todaySchedules.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Upcoming Planned Visits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Calendar",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateToSchedule() }
                    )
                }
            }
            items(todaySchedules.size) { idx ->
                val sched = todaySchedules[idx]
                val store = storeMap[sched.storeId]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSchedule() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = store?.name ?: "Store",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(sched.scheduledDate)) + " • " + sched.purpose,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    count: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
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
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = count,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ActionItemSummaryCard(
    action: com.example.data.local.entities.ActionEntity,
    storeName: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = storeName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                PriorityBadge(priority = action.priority)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = action.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Due: " + SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(action.dueAt)),
                    fontSize = 11.sp,
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(status = if (action.isOverdue) "OVERDUE" else action.status)
            }
        }
    }
}
