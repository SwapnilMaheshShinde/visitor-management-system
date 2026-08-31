package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun QrPassVisualizer(
    visitorName: String,
    otpCode: String,
    qrToken: String,
    hostName: String,
    expectedTime: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SlateLightCard,
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SlateLightBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pass Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(NavySurface, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Badge, contentDescription = null, tint = AccentCyanGlow, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Digital Visitor Pass", fontWeight = FontWeight.Bold, color = DeepNavyDark, fontSize = 15.sp)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StatusApprovedGreenContainer
                ) {
                    Text(
                        text = "ACTIVE PASS",
                        color = StatusApprovedGreenText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // QR Code Matrix Simulation
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Top QR Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QrFinderCorner()
                        Box(modifier = Modifier.size(24.dp).background(Color(0xFF38BDF8), RoundedCornerShape(4.dp)))
                        QrFinderCorner()
                    }

                    // Center Token & OTP Text
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PASS OTP",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = otpCode,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            letterSpacing = 4.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Bottom QR Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QrFinderCorner()
                        Box(modifier = Modifier.size(18.dp).background(Color(0xFF38BDF8), RoundedCornerShape(2.dp)))
                        Box(modifier = Modifier.size(28.dp).background(Color.White, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = DeepNavyDark, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = visitorName,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = DeepNavyDark
            )
            Text(
                text = "Host: $hostName • $expectedTime",
                fontSize = 12.sp,
                color = SlateLightTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Token: $qrToken",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = AccentCyan
            )
        }
    }
}

@Composable
private fun QrFinderCorner() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .border(3.dp, Color.White, RoundedCornerShape(6.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(Color.White, RoundedCornerShape(2.dp))
        )
    }
}

@Composable
fun QrScannerViewfinder(
    onScanToken: (String) -> Unit,
    onManualInput: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 220f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DeepNavyDark,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "High-Speed QR Scanner",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
                Surface(
                    color = NavyCard,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "GUARD CHECKPOINT",
                        color = AccentCyanGlow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Viewfinder Target Box with animated laser
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NavySurface)
                    .border(2.dp, AccentCyanGlow.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.TopCenter
            ) {
                // Viewfinder Corners
                Icon(
                    imageVector = Icons.Outlined.QrCodeScanner,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier
                        .size(160.dp)
                        .align(Alignment.Center)
                )

                // Laser scan line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .offset(y = laserOffset.dp)
                        .background(AccentCyanGlow)
                )

                Text(
                    text = "Align Visitor QR Pass inside frame",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Simulated Barcode Triggers for rapid testing
            Text(
                text = "Rapid Test Scans (Click to test validation):",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onScanToken("VMS-APPT-482910-SEC") },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyLight),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Pass 482910", fontSize = 11.sp, color = Color.White)
                }
                Button(
                    onClick = onManualInput,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Enter OTP", fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun OtpKeypadBox(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    onVerify: (String) -> Unit,
    lastDevOtp: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SlateLightCard,
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Enter 6-Digit Visitor OTP",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = DeepNavyDark
            )
            Text(
                text = "Ask visitor for the verification code received on their phone",
                fontSize = 12.sp,
                color = SlateLightTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 6 OTP Box Display
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 6) {
                    val digit = if (i < otpValue.length) otpValue[i].toString() else ""
                    val isFocused = i == otpValue.length

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isFocused) SlateLightBackground else SlateLightCard)
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) AccentCyan else if (digit.isNotEmpty()) DeepNavyDark else SlateLightBorder,
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = digit,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = DeepNavyDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Auto-fill Dev OTP Helper Chip
            if (lastDevOtp.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AccentCyan.copy(alpha = 0.1f),
                    modifier = Modifier.clickable {
                        onOtpChange(lastDevOtp)
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Test OTP: $lastDevOtp (Tap to fill)",
                            color = AccentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Numeric Keypad for Guard 1-handed operation
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("CLEAR", "0", "DELETE")
                )

                for (row in rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (key in row) {
                            Button(
                                onClick = {
                                    when (key) {
                                        "CLEAR" -> onOtpChange("")
                                        "DELETE" -> if (otpValue.isNotEmpty()) onOtpChange(otpValue.dropLast(1))
                                        else -> if (otpValue.length < 6) {
                                            val newOtp = otpValue + key
                                            onOtpChange(newOtp)
                                            if (newOtp.length == 6) {
                                                onVerify(newOtp)
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (key == "DELETE" || key == "CLEAR") Color(0xFFF1F5F9) else DeepNavyDark
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                if (key == "DELETE") {
                                    Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = DeepNavyDark, modifier = Modifier.size(20.dp))
                                } else {
                                    Text(
                                        text = key,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (key == "CLEAR") 12.sp else 18.sp,
                                        color = if (key == "CLEAR") DeepNavyDark else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
