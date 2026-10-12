package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entities.ActionEntity
import com.example.data.local.entities.PhotoEntity
import com.example.data.local.entities.ScheduledVisitEntity
import com.example.data.local.entities.StoreEntity
import com.example.data.local.entities.VisitEntity
import com.example.export.WhatsAppShareHelper
import com.example.export.XlsxExporter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StoreFlowUnitTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun testVisitDurationCalculation() {
        val start = 1700000000000L
        val end = start + (45 * 60 * 1000L) // 45 minutes
        val visit = VisitEntity(
            id = UUID.randomUUID().toString(),
            storeId = "store-1",
            startTimestampUtc = start,
            endTimestampUtc = end,
            status = "COMPLETED"
        )
        assertEquals(45L, visit.durationMinutes)
    }

    @Test
    fun testActionOverdueDetection() {
        val now = System.currentTimeMillis()
        val overdueAction = ActionEntity(
            id = "act-1",
            storeId = "store-1",
            title = "Fix pricing tag",
            dueAt = now - 100000L, // past
            status = "OPEN"
        )
        assertTrue(overdueAction.isOverdue)

        val completedAction = ActionEntity(
            id = "act-2",
            storeId = "store-1",
            title = "Fix shelf wobblers",
            dueAt = now - 100000L,
            status = "COMPLETED"
        )
        assertFalse(completedAction.isOverdue)

        val futureAction = ActionEntity(
            id = "act-3",
            storeId = "store-1",
            title = "Plan holiday promo",
            dueAt = now + 1000000L,
            status = "OPEN"
        )
        assertFalse(futureAction.isOverdue)
    }

    @Test
    fun testWhatsAppSummaryFormatting() {
        val helper = WhatsAppShareHelper(context)
        val store = StoreEntity(
            id = "store-1",
            code = "RET-101",
            name = "Metro Supercenter",
            city = "Metropolis"
        )
        val visit = VisitEntity(
            id = "visit-1",
            storeId = store.id,
            workerName = "Alex Mercer",
            startTimestampUtc = 1700000000000L,
            endTimestampUtc = 1700002700000L,
            notes = "Good customer traffic. Shelf displays compliant.",
            status = "COMPLETED"
        )
        val actions = listOf(
            ActionEntity(
                id = "act-1",
                storeId = store.id,
                originatingVisitId = visit.id,
                title = "Restock cooler facings",
                priority = "HIGH",
                status = "OPEN",
                dueAt = 1700100000000L
            )
        )

        val reportText = helper.buildVisitSummaryText(store, visit, actions)
        assertTrue(reportText.contains("STORE VISIT REPORT"))
        assertTrue(reportText.contains("Metro Supercenter"))
        assertTrue(reportText.contains("RET-101"))
        assertTrue(reportText.contains("Restock cooler facings"))
        assertTrue(reportText.contains("[HIGH]"))
    }

    @Test
    fun testXlsxExportGeneration() = runBlocking {
        val exporter = XlsxExporter(context)
        val stores = listOf(
            StoreEntity(code = "STR-1", name = "Test Store 1", city = "City A")
        )
        val visits = listOf(
            VisitEntity(storeId = stores.first().id, status = "COMPLETED")
        )
        val actions = listOf(
            ActionEntity(storeId = stores.first().id, title = "Test Action")
        )

        val generatedFile = exporter.generateComprehensiveWorkbook(
            stores = stores,
            visits = visits,
            actions = actions,
            photos = emptyList(),
            schedules = emptyList(),
            fileNamePrefix = "Test_Export"
        )

        assertTrue(generatedFile.exists())
        assertTrue(generatedFile.length() > 0)
        assertTrue(generatedFile.name.endsWith(".xlsx"))

        // Verify valid OpenXML zip entries
        val zip = ZipFile(generatedFile)
        assertNotNull(zip.getEntry("[Content_Types].xml"))
        assertNotNull(zip.getEntry("xl/workbook.xml"))
        assertNotNull(zip.getEntry("xl/styles.xml"))
        assertNotNull(zip.getEntry("xl/worksheets/sheet1.xml"))
        assertNotNull(zip.getEntry("xl/worksheets/sheet2.xml"))
        assertNotNull(zip.getEntry("xl/worksheets/sheet3.xml"))
        assertNotNull(zip.getEntry("xl/worksheets/sheet4.xml"))
        assertNotNull(zip.getEntry("xl/worksheets/sheet5.xml"))
        assertNotNull(zip.getEntry("xl/worksheets/sheet6.xml"))
        zip.close()
    }
}
