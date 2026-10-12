package com.example.export

import android.content.Context
import com.example.data.local.entities.ActionEntity
import com.example.data.local.entities.PhotoEntity
import com.example.data.local.entities.ScheduledVisitEntity
import com.example.data.local.entities.StoreEntity
import com.example.data.local.entities.VisitEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class XlsxExporter(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun generateComprehensiveWorkbook(
        stores: List<StoreEntity>,
        visits: List<VisitEntity>,
        actions: List<ActionEntity>,
        photos: List<PhotoEntity>,
        schedules: List<ScheduledVisitEntity>,
        fileNamePrefix: String = "StoreFlow_Report"
    ): File = withContext(Dispatchers.IO) {
        val exportsDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outputFile = File(exportsDir, "${fileNamePrefix}_$timeStamp.xlsx")

        val storeMap = stores.associateBy { it.id }

        // Define sheets
        val sheetNames = listOf(
            "Visit Summary",
            "Visit Details",
            "Action Items",
            "Photo Index",
            "Store Directory",
            "Scheduled Visits"
        )

        val sheetXmls = mutableListOf<String>()

        // 1. Visit Summary Sheet
        sheetXmls.add(buildVisitSummarySheet(visits, storeMap, actions))

        // 2. Visit Details Sheet
        sheetXmls.add(buildVisitDetailsSheet(visits, storeMap))

        // 3. Action Items Sheet
        sheetXmls.add(buildActionItemsSheet(actions, storeMap))

        // 4. Photo Index Sheet
        sheetXmls.add(buildPhotoIndexSheet(photos, storeMap, visits))

        // 5. Store Directory Sheet
        sheetXmls.add(buildStoreDirectorySheet(stores))

        // 6. Scheduled Visits Sheet
        sheetXmls.add(buildScheduledVisitsSheet(schedules, storeMap))

        // Write OpenXML Zip container
        ZipOutputStream(FileOutputStream(outputFile)).use { zip ->
            // [Content_Types].xml
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(buildContentTypesXml(sheetNames.size).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // _rels/.rels
            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(buildRootRelsXml().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // xl/workbook.xml
            zip.putNextEntry(ZipEntry("xl/workbook.xml"))
            zip.write(buildWorkbookXml(sheetNames).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // xl/_rels/workbook.xml.rels
            zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zip.write(buildWorkbookRelsXml(sheetNames.size).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // xl/styles.xml
            zip.putNextEntry(ZipEntry("xl/styles.xml"))
            zip.write(buildStylesXml().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // xl/worksheets/sheetN.xml
            for (i in sheetXmls.indices) {
                zip.putNextEntry(ZipEntry("xl/worksheets/sheet${i + 1}.xml"))
                zip.write(sheetXmls[i].toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }

        outputFile
    }

    private fun escapeXml(input: String?): String {
        if (input == null) return ""
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun colLetter(colIdx: Int): String {
        var n = colIdx
        var result = ""
        while (n >= 0) {
            result = (('A'.code + (n % 26)).toChar()) + result
            n = (n / 26) - 1
        }
        return result
    }

    private fun buildRow(rowIdx: Int, values: List<Pair<String, Boolean>>): String {
        val sb = StringBuilder()
        sb.append("<row r=\"$rowIdx\">")
        for (colIdx in values.indices) {
            val (text, isHeader) = values[colIdx]
            val cellRef = "${colLetter(colIdx)}$rowIdx"
            val styleAttr = if (isHeader) " s=\"1\"" else " s=\"0\""
            sb.append("<c r=\"$cellRef\" t=\"inlineStr\"$styleAttr><is><t>${escapeXml(text)}</t></is></c>")
        }
        sb.append("</row>")
        return sb.toString()
    }

    private fun buildSheetWrapper(rows: List<String>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n")
        sb.append("  <sheetViews>\n")
        sb.append("    <sheetView tabSelected=\"0\" workbookViewId=\"0\">\n")
        sb.append("      <pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/>\n")
        sb.append("    </sheetView>\n")
        sb.append("  </sheetViews>\n")
        sb.append("  <sheetData>\n")
        for (r in rows) {
            sb.append("    ").append(r).append("\n")
        }
        sb.append("  </sheetData>\n")
        sb.append("</worksheet>")
        return sb.toString()
    }

    private fun buildVisitSummarySheet(
        visits: List<VisitEntity>,
        storeMap: Map<String, StoreEntity>,
        actions: List<ActionEntity>
    ): String {
        val rows = mutableListOf<String>()
        val headers = listOf(
            "Visit ID" to true, "Store Code" to true, "Store Name" to true, "City" to true,
            "Worker" to true, "Visit Date" to true, "Status" to true, "Duration (min)" to true,
            "Actions Count" to true, "Follow-up Required" to true, "Next Visit" to true
        )
        rows.add(buildRow(1, headers))

        var r = 2
        for (v in visits) {
            val store = storeMap[v.storeId]
            val visitActions = actions.filter { it.originatingVisitId == v.id }
            val nextDate = v.nextFollowUpDate?.let { dateOnlyFormat.format(Date(it)) } ?: "None"
            val rowValues = listOf(
                v.id.take(8) to false,
                (store?.code ?: "N/A") to false,
                (store?.name ?: "Unknown") to false,
                (store?.city ?: "") to false,
                v.workerName to false,
                dateFormat.format(Date(v.startTimestampUtc)) to false,
                v.status to false,
                "${v.durationMinutes}" to false,
                "${visitActions.size}" to false,
                (if (v.followUpRequired) "YES" else "NO") to false,
                nextDate to false
            )
            rows.add(buildRow(r++, rowValues))
        }
        return buildSheetWrapper(rows)
    }

    private fun buildVisitDetailsSheet(
        visits: List<VisitEntity>,
        storeMap: Map<String, StoreEntity>
    ): String {
        val rows = mutableListOf<String>()
        val headers = listOf(
            "Visit ID" to true, "Store" to true, "Date" to true, "Priority" to true,
            "General Notes" to true, "Key Observations" to true, "Issues Identified" to true,
            "Positive Observations" to true, "Recommendations" to true
        )
        rows.add(buildRow(1, headers))

        var r = 2
        for (v in visits) {
            val store = storeMap[v.storeId]
            val rowValues = listOf(
                v.id.take(8) to false,
                (store?.name ?: v.storeId) to false,
                dateFormat.format(Date(v.startTimestampUtc)) to false,
                v.priority to false,
                v.notes to false,
                v.keyObservations to false,
                v.issuesIdentified to false,
                v.positiveObservations to false,
                v.recommendations to false
            )
            rows.add(buildRow(r++, rowValues))
        }
        return buildSheetWrapper(rows)
    }

    private fun buildActionItemsSheet(
        actions: List<ActionEntity>,
        storeMap: Map<String, StoreEntity>
    ): String {
        val rows = mutableListOf<String>()
        val headers = listOf(
            "Action ID" to true, "Store Code" to true, "Store Name" to true, "Title" to true,
            "Category" to true, "Priority" to true, "Status" to true, "Assigned To" to true,
            "Created Date" to true, "Due Date" to true, "Completed Date" to true,
            "Completion Notes" to true, "Is Overdue" to true
        )
        rows.add(buildRow(1, headers))

        var r = 2
        for (a in actions) {
            val store = storeMap[a.storeId]
            val compDate = a.completedAt?.let { dateFormat.format(Date(it)) } ?: ""
            val rowValues = listOf(
                a.id.take(8) to false,
                (store?.code ?: "") to false,
                (store?.name ?: "") to false,
                a.title to false,
                a.category to false,
                a.priority to false,
                a.status to false,
                a.assignedTo to false,
                dateOnlyFormat.format(Date(a.createdAt)) to false,
                dateOnlyFormat.format(Date(a.dueAt)) to false,
                compDate to false,
                (a.completionNotes ?: a.deferredReason ?: "") to false,
                (if (a.isOverdue) "OVERDUE" else "OK") to false
            )
            rows.add(buildRow(r++, rowValues))
        }
        return buildSheetWrapper(rows)
    }

    private fun buildPhotoIndexSheet(
        photos: List<PhotoEntity>,
        storeMap: Map<String, StoreEntity>,
        visits: List<VisitEntity>
    ): String {
        val visitMap = visits.associateBy { it.id }
        val rows = mutableListOf<String>()
        val headers = listOf(
            "Photo ID" to true, "Filename" to true, "Store" to true, "Visit Date" to true,
            "Caption" to true, "Size (KB)" to true, "Is Evidence" to true, "Captured At" to true
        )
        rows.add(buildRow(1, headers))

        var r = 2
        for (p in photos) {
            val file = File(p.filePath)
            val visit = p.visitId?.let { visitMap[it] }
            val store = visit?.storeId?.let { storeMap[it] }
            val rowValues = listOf(
                p.id.take(8) to false,
                file.name to false,
                (store?.name ?: "N/A") to false,
                (visit?.let { dateFormat.format(Date(it.startTimestampUtc)) } ?: "N/A") to false,
                p.caption to false,
                "${p.fileSize / 1024}" to false,
                (if (p.isEvidence) "YES" else "NO") to false,
                dateFormat.format(Date(p.capturedAt)) to false
            )
            rows.add(buildRow(r++, rowValues))
        }
        return buildSheetWrapper(rows)
    }

    private fun buildStoreDirectorySheet(stores: List<StoreEntity>): String {
        val rows = mutableListOf<String>()
        val headers = listOf(
            "Store Code" to true, "Name" to true, "Branch" to true, "City" to true,
            "Region" to true, "Category" to true, "Contact Person" to true, "Phone" to true,
            "Email" to true, "Frequency" to true, "Status" to true
        )
        rows.add(buildRow(1, headers))

        var r = 2
        for (s in stores) {
            val status = if (s.isArchived) "ARCHIVED" else if (s.isActive) "ACTIVE" else "INACTIVE"
            val rowValues = listOf(
                s.code to false,
                s.name to false,
                s.branchName to false,
                s.city to false,
                s.region to false,
                s.category to false,
                s.contactPerson to false,
                s.contactPhone to false,
                s.contactEmail to false,
                s.visitFrequency to false,
                status to false
            )
            rows.add(buildRow(r++, rowValues))
        }
        return buildSheetWrapper(rows)
    }

    private fun buildScheduledVisitsSheet(
        schedules: List<ScheduledVisitEntity>,
        storeMap: Map<String, StoreEntity>
    ): String {
        val rows = mutableListOf<String>()
        val headers = listOf(
            "Schedule ID" to true, "Store Code" to true, "Store Name" to true,
            "Scheduled Date" to true, "Purpose" to true, "Frequency" to true,
            "Completed" to true, "Prep Notes" to true
        )
        rows.add(buildRow(1, headers))

        var r = 2
        for (sc in schedules) {
            val store = storeMap[sc.storeId]
            val rowValues = listOf(
                sc.id.take(8) to false,
                (store?.code ?: "") to false,
                (store?.name ?: "") to false,
                dateFormat.format(Date(sc.scheduledDate)) to false,
                sc.purpose to false,
                sc.frequency to false,
                (if (sc.isCompleted) "YES" else "PENDING") to false,
                sc.preparationNotes to false
            )
            rows.add(buildRow(r++, rowValues))
        }
        return buildSheetWrapper(rows)
    }

    private fun buildContentTypesXml(sheetCount: Int): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sb.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n")
        sb.append("  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n")
        sb.append("  <Default Extension=\"xml\" ContentType=\"application/xml\"/>\n")
        sb.append("  <Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>\n")
        sb.append("  <Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>\n")
        for (i in 1..sheetCount) {
            sb.append("  <Override PartName=\"/xl/worksheets/sheet$i.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n")
        }
        sb.append("</Types>")
        return sb.toString()
    }

    private fun buildRootRelsXml(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun buildWorkbookXml(sheetNames: List<String>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sb.append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n")
        sb.append("  <sheets>\n")
        for (i in sheetNames.indices) {
            sb.append("    <sheet name=\"${escapeXml(sheetNames[i])}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>\n")
        }
        sb.append("  </sheets>\n")
        sb.append("</workbook>")
        return sb.toString()
    }

    private fun buildWorkbookRelsXml(sheetCount: Int): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sb.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n")
        for (i in 1..sheetCount) {
            sb.append("  <Relationship Id=\"rId$i\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet$i.xml\"/>\n")
        }
        sb.append("  <Relationship Id=\"rId${sheetCount + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>\n")
        sb.append("</Relationships>")
        return sb.toString()
    }

    private fun buildStylesXml(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><color theme="1"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF0D47A1"/></patternFill></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/><diagonal/></border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
  </cellXfs>
</styleSheet>"""
}
