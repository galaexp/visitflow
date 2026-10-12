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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import com.example.data.local.entities.ActionEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.viewmodels.StoreFlowViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActionCenterScreen(
    viewModel: StoreFlowViewModel
) {
    val allActions by viewModel.allActions.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val storeMap = stores.associateBy { it.id }

    var selectedTabIndex by remember { mutableStateOf(1) } // Default to "Open"
    val filterTabs = listOf("All", "Open", "Overdue", "High / Critical", "Completed")

    var selectedActionForUpdate by remember { mutableStateOf<ActionEntity?>(null) }
    var showCreateActionDialog by remember { mutableStateOf(false) }

    val filteredActions = when (selectedTabIndex) {
        0 -> allActions
        1 -> allActions.filter { it.status != "COMPLETED" && it.status != "CANCELLED" }
        2 -> allActions.filter { it.isOverdue }
        3 -> allActions.filter { (it.priority == "HIGH" || it.priority == "CRITICAL") && it.status != "COMPLETED" }
        4 -> allActions.filter { it.status == "COMPLETED" }
        else -> allActions
    }

    Scaffold(
        floatingActionButton = {
            if (stores.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showCreateActionDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("action_center_add_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Action")
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
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                filterTabs.forEachIndexed { idx, tab ->
                    val count = when (idx) {
                        0 -> allActions.size
                        1 -> allActions.count { it.status != "COMPLETED" && it.status != "CANCELLED" }
                        2 -> allActions.count { it.isOverdue }
                        3 -> allActions.count { (it.priority == "HIGH" || it.priority == "CRITICAL") && it.status != "COMPLETED" }
                        4 -> allActions.count { it.status == "COMPLETED" }
                        else -> 0
                    }
                    Tab(
                        selected = selectedTabIndex == idx,
                        onClick = { selectedTabIndex = idx },
                        text = {
                            Text(
                                text = "$tab ($count)",
                                fontWeight = if (selectedTabIndex == idx) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredActions.isEmpty()) {
                EmptyStateView(
                    icon = { Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary) },
                    title = "No action items in this filter",
                    message = if (selectedTabIndex == 2) "Great job! All store action deadlines are on track." else "No items match the selected status.",
                    actionButton = if (stores.isNotEmpty()) {
                        {
                            Button(onClick = { showCreateActionDialog = true }) {
                                Text("Create Action Item")
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
                    items(filteredActions, key = { it.id }) { action ->
                        val store = storeMap[action.storeId]
                        ActionCard(
                            action = action,
                            storeName = store?.name ?: "Unknown Store",
                            onClick = { selectedActionForUpdate = action }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Action Status Update Dialog
    selectedActionForUpdate?.let { action ->
        val store = storeMap[action.storeId]
        ActionStatusDialog(
            action = action,
            storeName = store?.name ?: "Store",
            onDismiss = { selectedActionForUpdate = null },
            onUpdate = { newStatus, completedBy, notes, reason, newDue ->
                viewModel.updateActionStatus(
                    actionId = action.id,
                    newStatus = newStatus,
                    completedBy = completedBy,
                    completionNotes = notes,
                    deferredReason = reason,
                    newDueAt = newDue,
                    onDone = { selectedActionForUpdate = null }
                )
            }
        )
    }

    // Create New Action Dialog
    if (showCreateActionDialog && stores.isNotEmpty()) {
        var selectedStoreId by remember { mutableStateOf(stores.first().id) }
        var showPicker by remember { mutableStateOf(false) }

        NewActionDialog(
            storeId = selectedStoreId,
            visitId = null,
            onDismiss = { showCreateActionDialog = false },
            onSave = { title, desc, cat, prio, assigned, due ->
                viewModel.createAction(
                    storeId = selectedStoreId,
                    originatingVisitId = null,
                    title = title,
                    description = desc,
                    category = cat,
                    priority = prio,
                    assignedTo = assigned,
                    dueAt = due,
                    onDone = { showCreateActionDialog = false }
                )
            }
        )
    }
}

@Composable
private fun ActionCard(
    action: ActionEntity,
    storeName: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("action_item_${action.id}"),
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
                    text = storeName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                PriorityBadge(priority = action.priority)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = action.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (action.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = action.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val dueFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(action.dueAt))
                    Text(
                        text = if (action.isOverdue) "⚠️ OVERDUE (Due: $dueFormat)" else "Due: $dueFormat",
                        fontSize = 11.sp,
                        fontWeight = if (action.isOverdue) FontWeight.Bold else FontWeight.Normal,
                        color = if (action.isOverdue) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Assigned to: ${action.assignedTo}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(status = if (action.isOverdue) "OVERDUE" else action.status)
            }

            if (action.status == "COMPLETED" && action.completionNotes != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFF2E7D32).copy(alpha = 0.08f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Completion Notes: ${action.completionNotes}",
                        fontSize = 11.sp,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionStatusDialog(
    action: ActionEntity,
    storeName: String,
    onDismiss: () -> Unit,
    onUpdate: (String, String?, String?, String?, Long?) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(action.status) }
    var completedBy by remember { mutableStateOf(action.completedBy ?: "Field Rep") }
    var completionNotes by remember { mutableStateOf(action.completionNotes ?: "") }
    var deferredReason by remember { mutableStateOf(action.deferredReason ?: "") }

    val statusOptions = listOf("OPEN", "IN_PROGRESS", "COMPLETED", "DEFERRED", "UNABLE_TO_COMPLETE")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Action Status") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = action.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "$storeName • Priority: ${action.priority}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Text("Select Status:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(statusOptions) { st ->
                            Surface(
                                modifier = Modifier.clickable { selectedStatus = st },
                                color = if (selectedStatus == st) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = st,
                                    fontSize = 11.sp,
                                    color = if (selectedStatus == st) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                if (selectedStatus == "COMPLETED") {
                    item {
                        OutlinedTextField(
                            value = completedBy,
                            onValueChange = { completedBy = it },
                            label = { Text("Completed By") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = completionNotes,
                            onValueChange = { completionNotes = it },
                            label = { Text("Completion Evidence & Notes *") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                }

                if (selectedStatus == "DEFERRED" || selectedStatus == "UNABLE_TO_COMPLETE") {
                    item {
                        OutlinedTextField(
                            value = deferredReason,
                            onValueChange = { deferredReason = it },
                            label = { Text("Reason for Delay / Inability") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdate(
                        selectedStatus,
                        if (selectedStatus == "COMPLETED") completedBy.trim() else null,
                        if (selectedStatus == "COMPLETED") completionNotes.trim() else null,
                        if (selectedStatus == "DEFERRED" || selectedStatus == "UNABLE_TO_COMPLETE") deferredReason.trim() else null,
                        null
                    )
                }
            ) {
                Text("Update Status")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
