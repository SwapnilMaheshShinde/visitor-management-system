package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*

@Composable
fun StatMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SlateLightCard,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SlateLightBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = DeepNavyDark,
                        fontSize = 20.sp
                    )
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SlateLightTextSecondary,
                        fontSize = 12.sp
                    )
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = color,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bg, textCol, label) = when (status.uppercase()) {
        "ACCEPTED", "APPROVED" -> Triple(StatusApprovedGreenContainer, StatusApprovedGreenText, "APPROVED")
        "PENDING" -> Triple(StatusPendingAmberContainer, StatusPendingAmberText, "PENDING")
        "DECLINED", "REJECTED" -> Triple(StatusDeclinedRedContainer, StatusDeclinedRedText, "DECLINED")
        "INSIDE" -> Triple(StatusInsideBlueContainer, StatusInsideBlueText, "INSIDE PREMISES")
        "COMPLETED" -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), "CHECKED OUT")
        "ENTRY_GRANTED" -> Triple(StatusApprovedGreenContainer, StatusApprovedGreenText, "ENTRY GRANTED")
        "SCHEDULED" -> Triple(Color(0xFFEDE9FE), Color(0xFF6D28D9), "SCHEDULED")
        "ARRIVED" -> Triple(StatusInsideBlueContainer, StatusInsideBlueText, "ARRIVED")
        else -> Triple(Color(0xFFE2E8F0), Color(0xFF334155), status.uppercase())
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Text(
            text = label,
            color = textCol,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun VisitorCard(
    visitorName: String,
    visitorCompany: String,
    purpose: String,
    hostName: String,
    gateName: String,
    time: String,
    status: String,
    badgeType: String = "Walk-in",
    actions: @Composable (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SlateLightCard,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateLightBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NavySurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = visitorName.take(1).uppercase(),
                            color = AccentCyanGlow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = visitorName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DeepNavyDark,
                                fontSize = 15.sp
                            )
                        )
                        Text(
                            text = "$visitorCompany • $badgeType",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SlateLightTextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                StatusBadge(status)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = SlateLightBorder.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(10.dp))

            // Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Host Employee", color = SlateLightTextSecondary, fontSize = 11.sp)
                    Text(hostName, fontWeight = FontWeight.SemiBold, color = DeepNavyDark, fontSize = 13.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Purpose", color = SlateLightTextSecondary, fontSize = 11.sp)
                    Text(purpose, fontWeight = FontWeight.SemiBold, color = DeepNavyDark, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Gate & Time", color = SlateLightTextSecondary, fontSize = 11.sp)
                    Text("$gateName • $time", fontWeight = FontWeight.Medium, color = SlateLightTextPrimary, fontSize = 12.sp)
                }
            }

            if (actions != null) {
                Spacer(modifier = Modifier.height(12.dp))
                actions()
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    message: String,
    icon: ImageVector = Icons.Outlined.Inbox,
    actionButton: @Composable (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SlateLightCard,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .border(1.dp, SlateLightBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFFF1F5F9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = SlateLightTextSecondary, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = DeepNavyDark,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                color = SlateLightTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            if (actionButton != null) {
                Spacer(modifier = Modifier.height(16.dp))
                actionButton()
            }
        }
    }
}
