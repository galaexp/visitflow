package com.example.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.entities.ActionEntity
import com.example.data.local.entities.PhotoEntity
import com.example.data.local.entities.StoreEntity
import com.example.data.local.entities.VisitEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WhatsAppShareHelper(private val context: Context) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun buildVisitSummaryText(
        store: StoreEntity,
        visit: VisitEntity,
        actions: List<ActionEntity>
    ): String {
        val openActions = actions.filter { it.status != "COMPLETED" && it.status != "CANCELLED" }
        val completedActions = actions.filter { it.status == "COMPLETED" }

        val sb = StringBuilder()
        sb.append("📋 *STORE VISIT REPORT*\n\n")
        sb.append("🏬 *Store:* ${store.name}\n")
        sb.append("🏷️ *Store Code:* ${store.code}\n")
        sb.append("📍 *Location:* ${store.city.ifBlank { store.address }}\n")
        sb.append("📅 *Visit Date:* ${dateFormat.format(Date(visit.startTimestampUtc))}\n")
        sb.append("⏱️ *Start Time:* ${timeFormat.format(Date(visit.startTimestampUtc))}\n")
        visit.endTimestampUtc?.let {
            sb.append("🏁 *End Time:* ${timeFormat.format(Date(it))}\n")
            sb.append("⏳ *Duration:* ${visit.durationMinutes} minutes\n")
        }
        sb.append("👤 *Representative:* ${visit.workerName}\n\n")

        sb.append("🔍 *OBSERVATIONS:*\n")
        if (visit.notes.isNotBlank()) {
            sb.append(visit.notes.trim()).append("\n")
        }
        if (visit.issuesIdentified.isNotBlank()) {
            sb.append("⚠️ *Issues:* ").append(visit.issuesIdentified.trim()).append("\n")
        }
        if (visit.positiveObservations.isNotBlank()) {
            sb.append("✅ *Highlights:* ").append(visit.positiveObservations.trim()).append("\n")
        }
        if (visit.recommendations.isNotBlank()) {
            sb.append("💡 *Recommendations:* ").append(visit.recommendations.trim()).append("\n")
        }
        if (visit.notes.isBlank() && visit.issuesIdentified.isBlank() && visit.positiveObservations.isBlank()) {
            sb.append("Routine inspection completed without issues.\n")
        }
        sb.append("\n")

        sb.append("📌 *ACTIONS REQUIRED (${openActions.size}):*\n")
        if (openActions.isEmpty()) {
            sb.append("No pending store actions.\n")
        } else {
            openActions.forEachIndexed { idx, act ->
                val dueStr = dateFormat.format(Date(act.dueAt))
                sb.append("${idx + 1}. *[${act.priority}]* ${act.title} (Due: $dueStr)\n")
                if (act.description.isNotBlank()) {
                    sb.append("   _${act.description.take(80)}_\n")
                }
            }
        }
        sb.append("\n")

        if (completedActions.isNotEmpty()) {
            sb.append("✅ *COMPLETED ACTIONS (${completedActions.size}):*\n")
            completedActions.forEachIndexed { idx, act ->
                sb.append("${idx + 1}. ${act.title}")
                act.completionNotes?.let { if (it.isNotBlank()) sb.append(" — Notes: $it") }
                sb.append("\n")
            }
            sb.append("\n")
        }

        sb.append("🗓️ *FOLLOW-UP:*\n")
        if (visit.followUpRequired && visit.nextFollowUpDate != null) {
            sb.append("Next scheduled visit: *${dateFormat.format(Date(visit.nextFollowUpDate))}*\n")
        } else {
            sb.append("Standard routine frequency: ${store.visitFrequency}\n")
        }

        sb.append("\n_Generated via StoreFlow_")
        return sb.toString()
    }

    fun shareSummaryText(summaryText: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Store Visit Report")
            putExtra(Intent.EXTRA_TEXT, summaryText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Share Store Visit Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun shareFile(file: File, mimeType: String, subject: String = "StoreFlow Export") {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Share via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun shareSummaryWithPhotos(summaryText: String, photos: List<PhotoEntity>) {
        val authority = "${context.packageName}.fileprovider"
        val validUris = arrayListOf<Uri>()

        for (photo in photos) {
            val file = File(photo.filePath)
            if (file.exists()) {
                validUris.add(FileProvider.getUriForFile(context, authority, file))
            }
        }

        val intent = if (validUris.size > 1) {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/jpeg"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, validUris)
                putExtra(Intent.EXTRA_TEXT, summaryText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else if (validUris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, validUris[0])
                putExtra(Intent.EXTRA_TEXT, summaryText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, summaryText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        val chooser = Intent.createChooser(intent, "Share Report & Photos").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
