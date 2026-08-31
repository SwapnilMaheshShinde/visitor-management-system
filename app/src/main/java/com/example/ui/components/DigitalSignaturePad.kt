package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun DigitalSignaturePad(
    visitorName: String,
    onSignatureConfirmed: (signatureData: String, notes: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val points = remember { mutableStateListOf<List<Offset>>() }
    var currentLine by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var notes by remember { mutableStateOf("") }
    var hasSigned by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SlateLightCard,
        tonalElevation = 6.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(StatusApprovedGreenContainer, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Draw,
                        contentDescription = null,
                        tint = StatusApprovedGreenText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Digital Host Verification",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DeepNavyDark
                        )
                    )
                    Text(
                        text = "Sign below to verify meeting with $visitorName",
                        style = MaterialTheme.typography.labelSmall.copy(color = SlateLightTextSecondary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Signature Canvas Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFAFAFA))
                    .border(1.dp, if (hasSigned) StatusApprovedGreen else SlateLightBorder, RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentLine = listOf(offset)
                                hasSigned = true
                            },
                            onDrag = { change, _ ->
                                currentLine = currentLine + change.position
                            },
                            onDragEnd = {
                                if (currentLine.isNotEmpty()) {
                                    points.add(currentLine)
                                    currentLine = emptyList()
                                }
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw saved paths
                    for (line in points) {
                        if (line.size > 1) {
                            val path = Path().apply {
                                moveTo(line.first().x, line.first().y)
                                for (i in 1 until line.size) {
                                    lineTo(line[i].x, line[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = DeepNavyDark,
                                style = Stroke(
                                    width = 4f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    // Draw current dragging path
                    if (currentLine.size > 1) {
                        val path = Path().apply {
                            moveTo(currentLine.first().x, currentLine.first().y)
                            for (i in 1 until currentLine.size) {
                                lineTo(currentLine[i].x, currentLine[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = DeepNavyDark,
                            style = Stroke(
                                width = 4f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                if (points.isEmpty() && currentLine.isEmpty()) {
                    Text(
                        text = "Sign with finger or stylus here ✍️",
                        color = SlateLightTextSecondary.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Clear button inside canvas
                if (points.isNotEmpty() || currentLine.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            points.clear()
                            currentLine = emptyList()
                            hasSigned = false
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Signature",
                            tint = StatusDeclinedRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Meeting notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Meeting Notes (Optional)") },
                placeholder = { Text("e.g. Discussed Q3 roadmap & vendor agreement") },
                maxLines = 2,
                colors = vmsOutlinedTextFieldColors(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val signatureStr = if (hasSigned) "VECTOR_SIG_POINTS_${points.size}_STROKES" else "DIGITAL_HOST_VERIFIED"
                        onSignatureConfirmed(signatureStr, notes)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusApprovedGreen),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Verify & Confirm", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
