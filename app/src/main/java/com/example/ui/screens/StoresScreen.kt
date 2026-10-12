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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.StoreEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.viewmodels.StoreFlowViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoresScreen(
    viewModel: StoreFlowViewModel,
    onStartVisit: (String) -> Unit
) {
    val stores by viewModel.stores.collectAsState()
    val searchQuery by viewModel.storeSearchQuery.collectAsState()
    val allActions by viewModel.allActions.collectAsState()
    val allVisits by viewModel.allVisits.collectAsState()

    var filterActiveOnly by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var storeToEdit by remember { mutableStateOf<StoreEntity?>(null) }
    var selectedStoreForDetail by remember { mutableStateOf<StoreEntity?>(null) }

    val filteredStores = stores.filter {
        if (filterActiveOnly) !it.isArchived else it.isArchived
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    storeToEdit = null
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_store_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Store")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setStoreSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("store_search_input"),
                placeholder = { Text("Search by store name, code, city...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setStoreSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterActiveOnly,
                    onClick = { filterActiveOnly = true },
                    label = { Text("Active Stores (${stores.count { !it.isArchived }})") }
                )
                FilterChip(
                    selected = !filterActiveOnly,
                    onClick = { filterActiveOnly = false },
                    label = { Text("Archived (${stores.count { it.isArchived }})") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredStores.isEmpty()) {
                EmptyStateView(
                    icon = { Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary) },
                    title = if (searchQuery.isNotBlank()) "No stores found" else "Store directory empty",
                    message = if (searchQuery.isNotBlank()) "Try a different search keyword" else "Add your first retail store to begin logging visits.",
                    actionButton = {
                        Button(onClick = { showAddDialog = true }) {
                            Text("Add Store Now")
                        }
                    }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredStores, key = { it.id }) { store ->
                        val storeOpenActions = allActions.filter { it.storeId == store.id && it.status != "COMPLETED" && it.status != "CANCELLED" }
                        val lastVisit = allVisits.filter { it.storeId == store.id && it.status == "COMPLETED" }.maxByOrNull { it.startTimestampUtc }

                        StoreCardItem(
                            store = store,
                            openActionsCount = storeOpenActions.size,
                            lastVisitTimestamp = lastVisit?.startTimestampUtc,
                            onStartVisit = { onStartVisit(store.id) },
                            onClick = { selectedStoreForDetail = store }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Add / Edit Store Dialog
    if (showAddDialog || storeToEdit != null) {
        StoreFormDialog(
            initialStore = storeToEdit,
            onDismiss = {
                showAddDialog = false
                storeToEdit = null
            },
            onSave = { newStore ->
                viewModel.saveStore(newStore)
                showAddDialog = false
                storeToEdit = null
            }
        )
    }

    // Store Details Bottom Sheet
    selectedStoreForDetail?.let { store ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { selectedStoreForDetail = null },
            sheetState = sheetState
        ) {
            StoreDetailContent(
                store = store,
                onStartVisit = {
                    selectedStoreForDetail = null
                    onStartVisit(store.id)
                },
                onEdit = {
                    val toEdit = store
                    selectedStoreForDetail = null
                    storeToEdit = toEdit
                },
                onToggleArchive = {
                    viewModel.setStoreArchived(store.id, !store.isArchived)
                    selectedStoreForDetail = null
                },
                onClose = { selectedStoreForDetail = null }
            )
        }
    }
}

@Composable
private fun StoreCardItem(
    store: StoreEntity,
    openActionsCount: Int,
    lastVisitTimestamp: Long?,
    onStartVisit: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("store_card_${store.code}"),
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
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = store.code,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = store.category,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = store.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (store.branchName.isNotBlank()) {
                Text(
                    text = store.branchName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${store.city.ifBlank { store.address }}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (openActionsCount > 0) {
                    Surface(
                        color = Color(0xFFE65100).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "$openActionsCount Open Actions",
                            color = Color(0xFFE65100),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Frequency: ${store.visitFrequency}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onStartVisit,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("start_visit_${store.code}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Visit", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun StoreDetailContent(
    store: StoreEntity,
    onStartVisit: () -> Unit,
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = store.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${store.code} • ${store.category}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        DetailRow(label = "Branch / Location", value = store.branchName.ifBlank { "Main Branch" })
        DetailRow(label = "Address", value = store.address)
        DetailRow(label = "City & Region", value = "${store.city}, ${store.region}".trim(',', ' '))
        DetailRow(label = "Contact Person", value = store.contactPerson)
        DetailRow(label = "Phone", value = store.contactPhone)
        DetailRow(label = "Email", value = store.contactEmail)
        DetailRow(label = "Visit Frequency", value = store.visitFrequency)
        DetailRow(label = "Preferred Days", value = store.preferredDays)
        DetailRow(label = "Opening Hours", value = store.openingHours)
        if (store.remarks.isNotBlank()) {
            DetailRow(label = "Remarks", value = store.remarks)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onStartVisit,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start Visit")
            }
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit Store")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onToggleArchive,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(if (store.isArchived) Icons.Default.Unarchive else Icons.Default.Archive, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (store.isArchived) "Restore Store" else "Archive Store")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isNotBlank()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1.5f)
            )
        }
    }
}

@Composable
private fun StoreFormDialog(
    initialStore: StoreEntity?,
    onDismiss: () -> Unit,
    onSave: (StoreEntity) -> Unit
) {
    var code by remember { mutableStateOf(initialStore?.code ?: "STR-${(100..999).random()}") }
    var name by remember { mutableStateOf(initialStore?.name ?: "") }
    var branchName by remember { mutableStateOf(initialStore?.branchName ?: "") }
    var address by remember { mutableStateOf(initialStore?.address ?: "") }
    var city by remember { mutableStateOf(initialStore?.city ?: "") }
    var region by remember { mutableStateOf(initialStore?.region ?: "") }
    var contactPerson by remember { mutableStateOf(initialStore?.contactPerson ?: "") }
    var contactPhone by remember { mutableStateOf(initialStore?.contactPhone ?: "") }
    var category by remember { mutableStateOf(initialStore?.category ?: "Supermarket") }
    var frequency by remember { mutableStateOf(initialStore?.visitFrequency ?: "Weekly") }
    var remarks by remember { mutableStateOf(initialStore?.remarks ?: "") }

    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialStore == null) "Add New Store" else "Edit Store") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; showError = false },
                        label = { Text("Store Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = showError && name.isBlank()
                    )
                }
                item {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Store Code *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = branchName,
                        onValueChange = { branchName = it },
                        label = { Text("Branch / Mall Location") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Street Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = region,
                            onValueChange = { region = it },
                            label = { Text("Region") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = contactPerson,
                            onValueChange = { contactPerson = it },
                            label = { Text("Contact Person") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            label = { Text("Phone") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = frequency,
                            onValueChange = { frequency = it },
                            label = { Text("Visit Frequency") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Remarks & Guidelines") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || code.isBlank()) {
                        showError = true
                    } else {
                        val store = initialStore?.copy(
                            code = code.trim(),
                            name = name.trim(),
                            branchName = branchName.trim(),
                            address = address.trim(),
                            city = city.trim(),
                            region = region.trim(),
                            contactPerson = contactPerson.trim(),
                            contactPhone = contactPhone.trim(),
                            category = category.trim(),
                            visitFrequency = frequency.trim(),
                            remarks = remarks.trim(),
                            updatedAt = System.currentTimeMillis()
                        ) ?: StoreEntity(
                            id = UUID.randomUUID().toString(),
                            code = code.trim(),
                            name = name.trim(),
                            branchName = branchName.trim(),
                            address = address.trim(),
                            city = city.trim(),
                            region = region.trim(),
                            contactPerson = contactPerson.trim(),
                            contactPhone = contactPhone.trim(),
                            category = category.trim(),
                            visitFrequency = frequency.trim(),
                            remarks = remarks.trim()
                        )
                        onSave(store)
                    }
                }
            ) {
                Text("Save Store")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
