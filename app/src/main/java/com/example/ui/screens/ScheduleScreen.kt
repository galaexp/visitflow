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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ScheduledVisitEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.viewmodels.StoreFlowViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ScheduleScreen(
    viewModel: StoreFlowViewModel,
    onStartVisitForStore: (String) -> Unit
) {
    val allSchedules by viewModel.allSchedules.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val storeMap = stores.associateBy { it.id }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Upcoming", "Overdue", "Completed", "All")

    var showPlanDialog by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val filteredSchedules = when (selectedTab) {
        0 -> allSchedules.filter { !it.isCompleted && it.scheduledDate >= now }
        1 -> allSchedules.filter { !it.isCompleted && it.scheduledDate < now }
        2 -> allSchedules.filter { it.isCompleted }
        3 -> allSchedules
        else -> allSchedules
    }

    Scaffold(
        floatingActionButton = {
            if (stores.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showPlanDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("plan_visit_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Plan Visit")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    val count = when (index) {
                        0 -> allSchedules.count { !it.isCompleted && it.scheduledDate >= now }
                        1 -> allSchedules.count { !it.isCompleted && it.scheduledDate < now }
                        2 -> allSchedules.count { it.isCompleted }
                        3 -> allSchedules.size
                        else -> 0
                    }
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = "$title ($count)",
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredSchedules.isEmpty()) {
                EmptyStateView(
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary) },
                    title = "No scheduled appointments",
                    message = if (selectedTab == 1) "Zero overdue visit appointments! Great job." else "Plan upcoming visits to stay on schedule.",
                    actionButton = if (stores.isNotEmpty()) {
                        {
                            Button(onClick = { showPlanDialog = true }) {
                                Text("Plan a Store Visit")
                            }
                        }
                    } else null
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSchedules, key = { it.id }) { schedule ->
                        val store = storeMap[schedule.storeId]
                        val isOverdue = !schedule.isCompleted && schedule.scheduledDate < now

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
                                        text = store?.name ?: "Unknown Store",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        color = if (isOverdue) Color(0xFFD32F2F).copy(alpha = 0.12f)
                                        else if (schedule.isCompleted) Color(0xFF2E7D32).copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (schedule.isCompleted) "COMPLETED" else if (isOverdue) "OVERDUE" else schedule.frequency,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOverdue) Color(0xFFD32F2F) else if (schedule.isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "📅 " + SimpleDateFormat("EEEE, dd MMM yyyy • HH:mm", Locale.getDefault()).format(Date(schedule.scheduledDate)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isOverdue) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    text = "Purpose: " + schedule.purpose,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (schedule.preparationNotes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Notes: " + schedule.preparationNotes,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!schedule.isCompleted) {
                                        Button(
                                            onClick = { onStartVisitForStore(schedule.storeId) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Start Visit", fontSize = 12.sp)
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.markScheduleCompleted(schedule.id) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Complete", fontSize = 12.sp)
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteSchedule(schedule.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (showPlanDialog && stores.isNotEmpty()) {
        PlanVisitDialog(
            stores = stores,
            onDismiss = { showPlanDialog = false },
            onSave = { storeId, date, purpose, notes, freq ->
                viewModel.scheduleVisit(
                    storeId = storeId,
                    scheduledDate = date,
                    purpose = purpose,
                    preparationNotes = notes,
                    frequency = freq,
                    onDone = { showPlanDialog = false }
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanVisitDialog(
    stores: List<com.example.data.local.entities.StoreEntity>,
    onDismiss: () -> Unit,
    onSave: (String, Long, String, String, String) -> Unit
) {
    var selectedStore by remember { mutableStateOf(stores.first()) }
    var expandedStoreDropdown by remember { mutableStateOf(false) }

    var purpose by remember { mutableStateOf("Routine Merchandising Inspection") }
    var preparationNotes by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("WEEKLY") }
    var daysAhead by remember { mutableStateOf("1") }

    val frequencies = listOf("ONCE", "DAILY", "WEEKLY", "BI_WEEKLY", "MONTHLY")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plan Store Visit") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Select Store:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    ExposedDropdownMenuBox(
                        expanded = expandedStoreDropdown,
                        onExpandedChange = { expandedStoreDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = "${selectedStore.name} (${selectedStore.code})",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStoreDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedStoreDropdown,
                            onDismissRequest = { expandedStoreDropdown = false }
                        ) {
                            stores.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text("${s.name} (${s.code})") },
                                    onClick = {
                                        selectedStore = s
                                        expandedStoreDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = purpose,
                        onValueChange = { purpose = it },
                        label = { Text("Visit Purpose") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Visit Frequency:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(frequencies) { freq ->
                            Surface(
                                modifier = Modifier.clickable { frequency = freq },
                                color = if (frequency == freq) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = freq,
                                    fontSize = 11.sp,
                                    color = if (frequency == freq) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = daysAhead,
                        onValueChange = { daysAhead = it },
                        label = { Text("Scheduled Days from Today (e.g. 1 = Tomorrow)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = preparationNotes,
                        onValueChange = { preparationNotes = it },
                        label = { Text("Preparation Notes (Materials to bring)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = daysAhead.toIntOrNull() ?: 1
                    val targetDate = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, days)
                        set(Calendar.HOUR_OF_DAY, 10)
                        set(Calendar.MINUTE, 0)
                    }.timeInMillis

                    onSave(selectedStore.id, targetDate, purpose.trim(), preparationNotes.trim(), frequency)
                }
            ) {
                Text("Save Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
