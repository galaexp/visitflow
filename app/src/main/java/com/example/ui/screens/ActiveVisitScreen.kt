package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.data.local.entities.ActionEntity
import com.example.data.local.entities.ChecklistItemEntity
import com.example.data.local.entities.PhotoEntity
import com.example.data.local.entities.VisitEntity
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.viewmodels.StoreFlowViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActiveVisitScreen(
    viewModel: StoreFlowViewModel,
    onVisitFinished: () -> Unit,
    onBack: () -> Unit
) {
    val activeVisit by viewModel.activeVisit.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val checklist by viewModel.activeVisitChecklist.collectAsState()
    val photos by viewModel.activeVisitPhotos.collectAsState()
    val actions by viewModel.activeVisitActions.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (activeVisit == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No visit currently in progress", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onBack) {
                Text("Back to Dashboard")
            }
        }
        return
    }

    val visit = activeVisit!!
    val currentStore = stores.find { it.id == visit.storeId }

    // Start listening to visit details if not yet
    LaunchedEffect(visit.id) {
        viewModel.observeActiveVisitDetails(visit.id)
    }

    // Live elapsed timer
    var elapsedMinutes by remember { mutableStateOf(visit.durationMinutes) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15000)
            elapsedMinutes = visit.durationMinutes
        }
    }

    // Editable visit observation fields
    var notes by remember(visit.id) { mutableStateOf(visit.notes) }
    var issues by remember(visit.id) { mutableStateOf(visit.issuesIdentified) }
    var highlights by remember(visit.id) { mutableStateOf(visit.positiveObservations) }
    var recommendations by remember(visit.id) { mutableStateOf(visit.recommendations) }

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Notes & Observations", "Inspection Checklist", "Action Items", "Evidence Photos")

    var showEndVisitDialog by remember { mutableStateOf(false) }
    var showAddActionDialog by remember { mutableStateOf(false) }
    var showPhotoCaptionDialog by remember { mutableStateOf<Uri?>(null) }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraFile != null) {
            viewModel.registerCapturedCameraPhoto(
                file = tempCameraFile!!,
                visitId = visit.id,
                actionId = null,
                caption = "Store Visit Camera Evidence",
                isEvidence = true
            )
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.attachPhotoFromUri(
                uri = uri,
                visitId = visit.id,
                actionId = null,
                caption = "Store Visit Photo",
                isEvidence = true
            )
        }
    }

    Scaffold(
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.updateActiveVisit(
                                visit.copy(
                                    notes = notes,
                                    issuesIdentified = issues,
                                    positiveObservations = highlights,
                                    recommendations = recommendations
                                )
                            )
                            onBack()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_draft_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Draft")
                    }

                    Button(
                        onClick = {
                            // Update notes first, then show prompt
                            viewModel.updateActiveVisit(
                                visit.copy(
                                    notes = notes,
                                    issuesIdentified = issues,
                                    positiveObservations = highlights,
                                    recommendations = recommendations
                                )
                            )
                            showEndVisitDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("end_visit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("End Visit")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header Store Banner
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentStore?.name ?: "Store Visit",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "$elapsedMinutes min",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${currentStore?.code ?: ""} • Started: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(visit.startTimestampUtc))}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // Scrollable Section Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = when (index) {
                                    2 -> "$title (${actions.size})"
                                    3 -> "$title (${photos.size})"
                                    else -> title
                                },
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // Tab Content
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> NotesAndObservationsTab(
                        notes = notes,
                        onNotesChange = { notes = it },
                        issues = issues,
                        onIssuesChange = { issues = it },
                        highlights = highlights,
                        onHighlightsChange = { highlights = it },
                        recommendations = recommendations,
                        onRecommendationsChange = { recommendations = it }
                    )
                    1 -> ChecklistTab(
                        checklist = checklist,
                        onItemStatusChange = { item, newStatus ->
                            viewModel.updateChecklistItem(item.copy(status = newStatus))
                        },
                        onItemNotesChange = { item, newNotes ->
                            viewModel.updateChecklistItem(item.copy(notes = newNotes))
                        }
                    )
                    2 -> ActionsTab(
                        actions = actions,
                        onAddAction = { showAddActionDialog = true }
                    )
                    3 -> PhotosTab(
                        photos = photos,
                        onTakePhoto = {
                            scope.launch {
                                val file = viewModel.createTempCameraFile()
                                tempCameraFile = file
                                val authority = "${context.packageName}.fileprovider"
                                val uri = FileProvider.getUriForFile(context, authority, file)
                                cameraLauncher.launch(uri)
                            }
                        },
                        onPickPhoto = {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onDeletePhoto = { photoId ->
                            viewModel.deletePhoto(photoId)
                        }
                    )
                }
            }
        }
    }

    // End Visit Confirmation Dialog
    if (showEndVisitDialog) {
        EndVisitDialog(
            storeName = currentStore?.name ?: "Store",
            durationMinutes = visit.durationMinutes,
            openActionsCount = actions.count { it.status != "COMPLETED" },
            onDismiss = { showEndVisitDialog = false },
            onConfirmEnd = { followUp, nextDate ->
                showEndVisitDialog = false
                viewModel.endVisit(
                    visitId = visit.id,
                    followUpRequired = followUp,
                    nextFollowUpDate = nextDate,
                    onEnded = onVisitFinished
                )
            }
        )
    }

    // Add In-Progress Action Item Dialog
    if (showAddActionDialog && currentStore != null) {
        NewActionDialog(
            storeId = currentStore.id,
            visitId = visit.id,
            onDismiss = { showAddActionDialog = false },
            onSave = { title, desc, category, priority, assignedTo, dueAt ->
                viewModel.createAction(
                    storeId = currentStore.id,
                    originatingVisitId = visit.id,
                    title = title,
                    description = desc,
                    category = category,
                    priority = priority,
                    assignedTo = assignedTo,
                    dueAt = dueAt,
                    onDone = { showAddActionDialog = false }
                )
            }
        )
    }
}

@Composable
private fun NotesAndObservationsTab(
    notes: String,
    onNotesChange: (String) -> Unit,
    issues: String,
    onIssuesChange: (String) -> Unit,
    highlights: String,
    onHighlightsChange: (String) -> Unit,
    recommendations: String,
    onRecommendationsChange: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "General Visit Notes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("visit_notes_input"),
                placeholder = { Text("Record general discussion with store manager, store traffic, overall vibe...") },
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Text(
                text = "Issues & Non-Compliance Identified",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F)
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = issues,
                onValueChange = onIssuesChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Stock shortages, damaged cartons, missing price tags, competitor promotions...") },
                minLines = 2,
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Text(
                text = "Positive Observations & Wins",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = highlights,
                onValueChange = onHighlightsChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Prime end-cap positioning, great staff cooperation, high stock turnover...") },
                minLines = 2,
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Text(
                text = "Recommendations for Next Visit",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = recommendations,
                onValueChange = onRecommendationsChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Items to check next time, distributor contacts needed, stock order adjustments...") },
                minLines = 2,
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ChecklistTab(
    checklist: List<ChecklistItemEntity>,
    onItemStatusChange: (ChecklistItemEntity, String) -> Unit,
    onItemNotesChange: (ChecklistItemEntity, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(checklist, key = { it.id }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = item.prompt,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ChecklistStatusButton(
                            label = "PASS",
                            isSelected = item.status == "PASS",
                            color = Color(0xFF2E7D32),
                            onClick = { onItemStatusChange(item, "PASS") }
                        )
                        ChecklistStatusButton(
                            label = "FAIL",
                            isSelected = item.status == "FAIL",
                            color = Color(0xFFD32F2F),
                            onClick = { onItemStatusChange(item, "FAIL") }
                        )
                        ChecklistStatusButton(
                            label = "N/A",
                            isSelected = item.status == "NA",
                            color = Color.Gray,
                            onClick = { onItemStatusChange(item, "NA") }
                        )
                        ChecklistStatusButton(
                            label = "FOLLOW-UP",
                            isSelected = item.status == "NEEDS_FOLLOW_UP",
                            color = Color(0xFFE65100),
                            onClick = { onItemStatusChange(item, "NEEDS_FOLLOW_UP") }
                        )
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ChecklistStatusButton(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable { onClick() }
            .clip(RoundedCornerShape(8.dp)),
        color = if (isSelected) color else color.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ActionsTab(
    actions: List<ActionEntity>,
    onAddAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Store Actions Required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onAddAction,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("add_action_in_visit_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Action")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (actions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No action items created yet for this visit.\nTap 'Add Action' to record required follow-ups.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(actions, key = { it.id }) { action ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                PriorityBadge(priority = action.priority)
                                StatusBadge(status = action.status)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = action.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (action.description.isNotBlank()) {
                                Text(
                                    text = action.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Assigned to: ${action.assignedTo} • Due: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(action.dueAt))}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotosTab(
    photos: List<PhotoEntity>,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onDeletePhoto: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onTakePhoto,
                modifier = Modifier
                    .weight(1f)
                    .testTag("take_photo_btn")
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Camera")
            }
            OutlinedButton(
                onClick = onPickPhoto,
                modifier = Modifier
                    .weight(1f)
                    .testTag("pick_photo_btn")
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Photo Picker")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (photos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No photographs attached yet.\nCapture camera evidence of shelf facings, displays or damaged goods.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(photos, key = { it.id }) { photo ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val imgFile = File(photo.filePath)
                            Image(
                                painter = rememberAsyncImagePainter(imgFile),
                                contentDescription = photo.caption,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = photo.caption.ifBlank { "Store evidence photo" },
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(photo.capturedAt)),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${photo.fileSize / 1024} KB",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { onDeletePhoto(photo.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EndVisitDialog(
    storeName: String,
    durationMinutes: Long,
    openActionsCount: Int,
    onDismiss: () -> Unit,
    onConfirmEnd: (Boolean, Long?) -> Unit
) {
    var followUpRequired by remember { mutableStateOf(false) }
    val defaultNextDate = remember { System.currentTimeMillis() + (7 * 24 * 3600 * 1000L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete Visit: $storeName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Visit duration: $durationMinutes minutes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (openActionsCount > 0) {
                    Text(
                        text = "Notice: There are $openActionsCount pending action items recorded for this store.",
                        fontSize = 13.sp,
                        color = Color(0xFFE65100)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { followUpRequired = !followUpRequired }
                ) {
                    Checkbox(
                        checked = followUpRequired,
                        onCheckedChange = { followUpRequired = it }
                    )
                    Text("Requires follow-up visit?", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                if (followUpRequired) {
                    Text(
                        text = "Next scheduled visit will be planned in 7 days (${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(defaultNextDate))}).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmEnd(followUpRequired, if (followUpRequired) defaultNextDate else null) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Finalize & End Visit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Continue Visit")
            }
        }
    )
}

@Composable
fun NewActionDialog(
    storeId: String,
    visitId: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Merchandising") }
    var priority by remember { mutableStateOf("NORMAL") }
    var assignedTo by remember { mutableStateOf("Store Manager") }
    var daysToDue by remember { mutableStateOf("7") }

    val categories = listOf("Merchandising", "Pricing", "Stock", "Damaged Goods", "Promotional", "Staff Training")
    val priorities = listOf("LOW", "NORMAL", "HIGH", "CRITICAL")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Store Action Item") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Action Title *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Detailed Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
                item {
                    Text("Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            Surface(
                                modifier = Modifier.clickable { category = cat },
                                color = if (category == cat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    color = if (category == cat) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                item {
                    Text("Priority:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        priorities.forEach { prio ->
                            Surface(
                                modifier = Modifier.clickable { priority = prio },
                                color = if (priority == prio) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = prio,
                                    fontSize = 11.sp,
                                    color = if (priority == prio) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = assignedTo,
                        onValueChange = { assignedTo = it },
                        label = { Text("Assigned Person / Lead") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val dueMillis = System.currentTimeMillis() + (daysToDue.toLongOrNull() ?: 7) * 86400000L
                        onSave(title.trim(), description.trim(), category, priority, assignedTo.trim(), dueMillis)
                    }
                }
            ) {
                Text("Create Action")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
