package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.local.entities.ActionEntity
import com.example.data.local.entities.ScheduledVisitEntity
import com.example.data.local.entities.StoreEntity
import com.example.data.repository.ActionRepository
import com.example.data.repository.PhotoRepository
import com.example.data.repository.ScheduleRepository
import com.example.data.repository.StoreRepository
import com.example.data.repository.VisitRepository
import com.example.export.BackupManager
import com.example.export.WhatsAppShareHelper
import com.example.export.XlsxExporter
import com.example.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class StoreFlowApplication : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val storeRepository by lazy {
        StoreRepository(database.storeDao(), database.syncOutboxDao(), database.auditDao())
    }
    val visitRepository by lazy {
        VisitRepository(database.visitDao(), database.checklistDao(), database.syncOutboxDao(), database.auditDao())
    }
    val actionRepository by lazy {
        ActionRepository(database.actionDao(), database.syncOutboxDao(), database.auditDao())
    }
    val photoRepository by lazy {
        PhotoRepository(this, database.photoDao(), database.syncOutboxDao())
    }
    val scheduleRepository by lazy {
        ScheduleRepository(database.scheduleDao(), database.syncOutboxDao(), database.auditDao())
    }
    val syncManager by lazy {
        SyncManager(database.syncOutboxDao())
    }
    val xlsxExporter by lazy {
        XlsxExporter(this)
    }
    val shareHelper by lazy {
        WhatsAppShareHelper(this)
    }
    val backupManager by lazy {
        BackupManager(this, database)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        seedInitialDataIfEmpty()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "visit_reminders",
                "Store Visit Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for scheduled retail visits and overdue actions"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun seedInitialDataIfEmpty() {
        CoroutineScope(Dispatchers.IO).launch {
            val count = database.storeDao().getActiveStoresCount()
            if (count == 0) {
                val store1 = StoreEntity(
                    id = UUID.randomUUID().toString(),
                    code = "RET-101",
                    name = "Metro Supercenter Downtown",
                    branchName = "Central Plaza",
                    address = "450 Market Street",
                    city = "Metropolis",
                    region = "Central Territory",
                    category = "Supermarket",
                    contactPerson = "Marcus Vance",
                    contactPhone = "+1 555-0192",
                    contactEmail = "m.vance@metrosuper.com",
                    assignedWorker = "Alex Mercer",
                    visitFrequency = "Weekly",
                    preferredDays = "Mon, Thu",
                    openingHours = "07:00 - 22:00",
                    remarks = "High volume flagship location. Prioritize dairy & beverage end-caps."
                )

                val store2 = StoreEntity(
                    id = UUID.randomUUID().toString(),
                    code = "RET-102",
                    name = "Apex Fresh Hypermarket",
                    branchName = "Northgate Mall",
                    address = "1200 Northway Blvd",
                    city = "North Heights",
                    region = "Northern District",
                    category = "Supermarket",
                    contactPerson = "Elena Rostova",
                    contactPhone = "+1 555-0144",
                    contactEmail = "erostova@apexfresh.com",
                    assignedWorker = "Alex Mercer",
                    visitFrequency = "Bi-weekly",
                    preferredDays = "Tuesday",
                    openingHours = "08:00 - 21:00",
                    remarks = "Promotional compliance check required for upcoming holiday campaign."
                )

                val store3 = StoreEntity(
                    id = UUID.randomUUID().toString(),
                    code = "RET-103",
                    name = "Cornerstone Express",
                    branchName = "Subway Station Bay",
                    address = "88 Grand Concourse",
                    city = "Metropolis",
                    region = "Central Territory",
                    category = "Convenience",
                    contactPerson = "Tariq Mansoor",
                    contactPhone = "+1 555-0188",
                    contactEmail = "tmansoor@cornerstone.net",
                    assignedWorker = "Alex Mercer",
                    visitFrequency = "Weekly",
                    preferredDays = "Wednesday",
                    openingHours = "06:00 - 23:00",
                    remarks = "High foot traffic convenience store. Check chilled grab-and-go stock."
                )

                val store4 = StoreEntity(
                    id = UUID.randomUUID().toString(),
                    code = "RET-104",
                    name = "Evergreen Organic Market",
                    branchName = "Westside Commons",
                    address = "310 Valley View Ave",
                    city = "Oakridge",
                    region = "Western Territory",
                    category = "Specialty Retail",
                    contactPerson = "Sarah Jenkins",
                    contactPhone = "+1 555-0176",
                    contactEmail = "sjenkins@evergreenmarket.org",
                    assignedWorker = "Alex Mercer",
                    visitFrequency = "Monthly",
                    preferredDays = "Friday",
                    openingHours = "09:00 - 20:00",
                    remarks = "Strict organic certification audit. Shelf facings check."
                )

                database.storeDao().insertAll(listOf(store1, store2, store3, store4))

                // Seed starter action items
                val now = System.currentTimeMillis()
                val action1 = ActionEntity(
                    id = UUID.randomUUID().toString(),
                    storeId = store1.id,
                    title = "Replace damaged aisle 3 promotional shelf wobblers",
                    description = "End-cap promotional tags worn down; request fresh marketing collateral from distributor.",
                    category = "Promotional",
                    priority = "HIGH",
                    status = "OPEN",
                    assignedTo = "Store Manager",
                    dueAt = now + (3 * 24 * 60 * 60 * 1000L)
                )

                val action2 = ActionEntity(
                    id = UUID.randomUUID().toString(),
                    storeId = store2.id,
                    title = "Audit pricing disparity on 500ml Energy Drinks",
                    description = "Shelf tag displays \$2.49 but register scanned \$2.79. Re-print barcode label.",
                    category = "Pricing",
                    priority = "CRITICAL",
                    status = "OPEN",
                    assignedTo = "Pricing Lead",
                    dueAt = now - (1 * 24 * 60 * 60 * 1000L) // overdue for testing
                )

                val action3 = ActionEntity(
                    id = UUID.randomUUID().toString(),
                    storeId = store3.id,
                    title = "Restock zero-sugar beverage cooler facings",
                    description = "Inventory in rear storage needs replenishment onto front shelves.",
                    category = "Stock",
                    priority = "NORMAL",
                    status = "COMPLETED",
                    assignedTo = "Stock Clerk",
                    completedAt = now - 3600000L,
                    completedBy = "Alex Mercer",
                    completionNotes = "Fully restocked and faced 4 trays from backroom cooler."
                )

                database.actionDao().insertAll(listOf(action1, action2, action3))

                // Seed scheduled visits
                val sched1 = ScheduledVisitEntity(
                    id = UUID.randomUUID().toString(),
                    storeId = store1.id,
                    scheduledDate = now + (4 * 3600 * 1000L), // Today
                    purpose = "Weekly Merchandising & POS Audit",
                    preparationNotes = "Bring updated pricing sheets and promotional sample packs.",
                    frequency = "WEEKLY"
                )

                val sched2 = ScheduledVisitEntity(
                    id = UUID.randomUUID().toString(),
                    storeId = store2.id,
                    scheduledDate = now + (2 * 24 * 3600 * 1000L),
                    purpose = "Follow-up on pricing tag correction",
                    preparationNotes = "Verify that register scanning price matches the revised shelf tags.",
                    frequency = "BI_WEEKLY"
                )

                database.scheduleDao().insertAll(listOf(sched1, sched2))
            }
        }
    }
}
