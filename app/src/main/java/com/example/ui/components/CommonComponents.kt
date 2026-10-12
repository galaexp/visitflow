package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusCompleted
import com.example.ui.theme.StatusDeferred
import com.example.ui.theme.StatusInProgress
import com.example.ui.theme.StatusOpen
import com.example.ui.theme.StatusOverdue

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (status.uppercase()) {
        "OPEN" -> Triple(StatusOpen.copy(alpha = 0.15f), StatusOpen, "OPEN")
        "IN_PROGRESS" -> Triple(StatusInProgress.copy(alpha = 0.15f), StatusInProgress, "IN PROGRESS")
        "COMPLETED" -> Triple(StatusCompleted.copy(alpha = 0.15f), StatusCompleted, "COMPLETED")
        "OVERDUE" -> Triple(StatusOverdue.copy(alpha = 0.15f), StatusOverdue, "OVERDUE")
        "DEFERRED" -> Triple(StatusDeferred.copy(alpha = 0.15f), StatusDeferred, "DEFERRED")
        "CANCELLED" -> Triple(Color.Gray.copy(alpha = 0.15f), Color.DarkGray, "CANCELLED")
        "UNABLE_TO_COMPLETE" -> Triple(StatusOverdue.copy(alpha = 0.15f), StatusOverdue, "UNABLE")
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, status)
    }

    Surface(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        color = bgColor
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PriorityBadge(priority: String, modifier: Modifier = Modifier) {
    val (color, label) = when (priority.uppercase()) {
        "CRITICAL" -> Color(0xFFD32F2F) to "CRITICAL"
        "HIGH" -> Color(0xFFE65100) to "HIGH"
        "NORMAL" -> Color(0xFF1565C0) to "NORMAL"
        "LOW" -> Color(0xFF558B2F) to "LOW"
        else -> MaterialTheme.colorScheme.secondary to priority
    }

    Surface(
        modifier = modifier.clip(RoundedCornerShape(6.dp)),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun OfflineSyncBanner(
    pendingCount: Int,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSyncClick() }
            .testTag("offline_sync_banner"),
        colors = CardDefaults.cardColors(
            containerColor = if (pendingCount > 0)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (pendingCount > 0) Icons.Default.Sync else Icons.Default.CheckCircle,
                contentDescription = "Sync State",
                tint = if (pendingCount > 0) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (pendingCount > 0) "$pendingCount changes queued locally" else "Offline-First Storage Ready",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (pendingCount > 0) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = if (pendingCount > 0) "Tap to attempt background upload to server" else "Primary database stored safely on device",
                    fontSize = 11.sp,
                    color = (if (pendingCount > 0) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer).copy(alpha = 0.8f)
                )
            }
            if (pendingCount > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Sync",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    icon: @Composable () -> Unit,
    title: String,
    message: String,
    actionButton: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (actionButton != null) {
            Spacer(modifier = Modifier.height(16.dp))
            actionButton()
        }
    }
}
