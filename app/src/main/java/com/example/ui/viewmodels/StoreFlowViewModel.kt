package com.example.ui.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.StoreFlowApplication
import com.example.data.local.entities.ActionEntity
import com.example.data.local.entities.ChecklistItemEntity
import com.example.data.local.entities.PhotoEntity
import com.example.data.local.entities.ScheduledVisitEntity
import com.example.data.local.entities.StoreEntity
import com.example.data.local.entities.VisitEntity
import com.example.export.BackupStats
import com.example.sync.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

class StoreFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as StoreFlowApplication
    private val storeRepo = app.storeRepository
    private val visitRepo = app.visitRepository
    private val actionRepo = app.actionRepository
    private val photoRepo = app.photoRepository
    private val scheduleRepo = app.scheduleRepository
    private val syncManager = app.syncManager
    val xlsxExporter = app.xlsxExporter
    val shareHelper = app.shareHelper
    val backupManager = app.backupManager

    // --- HOME & STATS ---
    val activeVisit: StateFlow<VisitEntity?> = visitRepo.activeVisitFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val visitsTodayCount: StateFlow<Int> = visitRepo.getVisitsCompletedTodayFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val visitsThisWeekCount: StateFlow<Int> = visitRepo.getVisitsCompletedThisWeekFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val openActionsCount: StateFlow<Int> = actionRepo.getOpenActionsCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val overdueActionsCount: StateFlow<Int> = actionRepo.getOverdueActionsCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedActionsThisMonth: StateFlow<Int> = actionRepo.getCompletedActionsThisMonthFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val upcomingSchedulesCount: StateFlow<Int> = scheduleRepo.getUpcomingCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val overdueSchedulesCount: StateFlow<Int> = scheduleRepo.getOverdueCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pendingSyncCount: StateFlow<Int> = syncManager.pendingCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- STORES ---
    private val _storeSearchQuery = MutableStateFlow("")
    val storeSearchQuery: StateFlow<String> = _storeSearchQuery.asStateFlow()

    val stores: StateFlow<List<StoreEntity>> = combine(
        storeRepo.allStores,
        _storeSearchQuery
    ) { all, query ->
        if (query.isBlank()) {
            all
        } else {
            all.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.code.contains(query, ignoreCase = true) ||
                it.city.contains(query, ignoreCase = true) ||
                it.contactPerson.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setStoreSearchQuery(q: String) {
        _storeSearchQuery.value = q
    }

    fun saveStore(store: StoreEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            storeRepo.saveStore(store)
            onDone?.invoke()
        }
    }

    fun setStoreArchived(storeId: String, archived: Boolean) {
        viewModelScope.launch {
            storeRepo.setArchived(storeId, archived)
        }
    }

    fun deleteStore(storeId: String) {
        viewModelScope.launch {
            storeRepo.deleteStore(storeId)
        }
    }

    // --- VISITS ---
    val allVisits: StateFlow<List<VisitEntity>> = visitRepo.allVisits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeVisitChecklist = MutableStateFlow<List<ChecklistItemEntity>>(emptyList())
    val activeVisitChecklist: StateFlow<List<ChecklistItemEntity>> = _activeVisitChecklist.asStateFlow()

    private val _activeVisitPhotos = MutableStateFlow<List<PhotoEntity>>(emptyList())
    val activeVisitPhotos: StateFlow<List<PhotoEntity>> = _activeVisitPhotos.asStateFlow()

    private val _activeVisitActions = MutableStateFlow<List<ActionEntity>>(emptyList())
    val activeVisitActions: StateFlow<List<ActionEntity>> = _activeVisitActions.asStateFlow()

    fun observeActiveVisitDetails(visitId: String) {
        viewModelScope.launch {
            visitRepo.getChecklistForVisit(visitId).collect {
                _activeVisitChecklist.value = it
            }
        }
        viewModelScope.launch {
            photoRepo.getPhotosForVisit(visitId).collect {
                _activeVisitPhotos.value = it
            }
        }
        viewModelScope.launch {
            actionRepo.getActionsForVisit(visitId).collect {
                _activeVisitActions.value = it
            }
        }
    }

    fun startVisit(storeId: String, workerName: String = "Field Rep", onStarted: (VisitEntity) -> Unit) {
        viewModelScope.launch {
            val visit = visitRepo.startVisit(storeId, workerName)
            observeActiveVisitDetails(visit.id)
            onStarted(visit)
        }
    }

    fun updateActiveVisit(visit: VisitEntity) {
        viewModelScope.launch {
            visitRepo.updateVisit(visit)
        }
    }

    fun updateChecklistItem(item: ChecklistItemEntity) {
        viewModelScope.launch {
            visitRepo.updateChecklistItem(item)
        }
    }

    fun endVisit(
        visitId: String,
        followUpRequired: Boolean,
        nextFollowUpDate: Long?,
        onEnded: () -> Unit
    ) {
        viewModelScope.launch {
            visitRepo.endVisit(
                visitId = visitId,
                followUpRequired = followUpRequired,
                nextFollowUpDate = nextFollowUpDate
            )
            onEnded()
        }
    }

    // --- ACTIONS ---
    val allActions: StateFlow<List<ActionEntity>> = actionRepo.allActions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createAction(
        storeId: String,
        originatingVisitId: String?,
        title: String,
        description: String,
        category: String,
        priority: String,
        assignedTo: String,
        dueAt: Long,
        onDone: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            actionRepo.createAction(
                storeId = storeId,
                originatingVisitId = originatingVisitId,
                title = title,
                description = description,
                category = category,
                priority = priority,
                assignedTo = assignedTo,
                dueAt = dueAt
            )
            onDone?.invoke()
        }
    }

    fun updateActionStatus(
        actionId: String,
        newStatus: String,
        completedBy: String? = null,
        completionNotes: String? = null,
        deferredReason: String? = null,
        newDueAt: Long? = null,
        onDone: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            actionRepo.updateActionStatus(
                actionId = actionId,
                newStatus = newStatus,
                completedBy = completedBy,
                completionNotes = completionNotes,
                deferredReason = deferredReason,
                newDueAt = newDueAt
            )
            onDone?.invoke()
        }
    }

    // --- PHOTOS ---
    fun attachPhotoFromUri(
        uri: Uri,
        visitId: String?,
        actionId: String?,
        caption: String,
        isEvidence: Boolean = false
    ) {
        viewModelScope.launch {
            photoRepo.saveImageFromUri(
                uri = uri,
                visitId = visitId,
                actionId = actionId,
                caption = caption,
                isEvidence = isEvidence
            )
        }
    }

    suspend fun createTempCameraFile(): File = photoRepo.createTempCameraFile()

    fun registerCapturedCameraPhoto(
        file: File,
        visitId: String?,
        actionId: String?,
        caption: String,
        isEvidence: Boolean = false
    ) {
        viewModelScope.launch {
            photoRepo.registerCapturedFile(
                file = file,
                visitId = visitId,
                actionId = actionId,
                caption = caption,
                isEvidence = isEvidence
            )
        }
    }

    fun deletePhoto(photoId: String) {
        viewModelScope.launch {
            photoRepo.deletePhoto(photoId)
        }
    }

    // --- SCHEDULE ---
    val allSchedules: StateFlow<List<ScheduledVisitEntity>> = scheduleRepo.allSchedules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun scheduleVisit(
        storeId: String,
        scheduledDate: Long,
        purpose: String,
        preparationNotes: String,
        frequency: String,
        onDone: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            scheduleRepo.scheduleVisit(
                storeId = storeId,
                scheduledDate = scheduledDate,
                purpose = purpose,
                preparationNotes = preparationNotes,
                frequency = frequency
            )
            onDone?.invoke()
        }
    }

    fun markScheduleCompleted(scheduleId: String, linkedVisitId: String? = null) {
        viewModelScope.launch {
            scheduleRepo.markCompleted(scheduleId, linkedVisitId)
        }
    }

    fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            scheduleRepo.deleteSchedule(scheduleId)
        }
    }

    // --- SYNC ---
    private val _syncStateMessage = MutableStateFlow<String?>(null)
    val syncStateMessage: StateFlow<String?> = _syncStateMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun triggerSync(workerUrl: String?, token: String?) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStateMessage.value = "Attempting sync..."
            val result = syncManager.performSync(workerUrl, token)
            _isSyncing.value = false
            _syncStateMessage.value = when (result) {
                is SyncResult.Success -> "Successfully synchronized ${result.syncedCount} records."
                is SyncResult.OfflineNoEndpoint -> result.message
                is SyncResult.Error -> "Sync failed: ${result.error}. ${result.failedCount} records retained safely in offline outbox."
            }
        }
    }

    fun clearSyncMessage() {
        _syncStateMessage.value = null
    }

    // --- EXPORT & SHARE ---
    fun exportComprehensiveExcel(onExported: (File) -> Unit) {
        viewModelScope.launch {
            val storesList = storeRepo.getAllStoresList()
            val visitsList = visitRepo.getAllVisitsList()
            val actionsList = actionRepo.getAllActionsList()
            val photosList = photoRepo.getAllPhotosList()
            val schedulesList = scheduleRepo.getAllSchedulesList()

            val file = xlsxExporter.generateComprehensiveWorkbook(
                stores = storesList,
                visits = visitsList,
                actions = actionsList,
                photos = photosList,
                schedules = schedulesList
            )
            onExported(file)
        }
    }

    // --- BACKUP & RESTORE ---
    fun createBackup(onBackupCreated: (File) -> Unit) {
        viewModelScope.launch {
            val file = backupManager.createFullBackup()
            onBackupCreated(file)
        }
    }

    fun restoreBackup(file: File, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = backupManager.restoreFromBackup(file)
            onResult(ok)
        }
    }
}
