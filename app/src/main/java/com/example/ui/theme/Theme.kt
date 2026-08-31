package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Standard Geometric Balance Material 3 Color Schemes
private val DarkColorScheme = darkColorScheme(
    primary = AccentCyanGlow,
    onPrimary = DeepNavyDark,
    primaryContainer = NavyLight,
    onPrimaryContainer = Color.White,
    secondary = AccentBlue,
    onSecondary = Color.White,
    secondaryContainer = NavyCard,
    onSecondaryContainer = Color.White,
    tertiary = StatusApprovedGreen,
    background = DeepNavyDark,
    surface = NavySurface,
    surfaceVariant = NavyCard,
    surfaceContainer = SlateLightCard,
    surfaceContainerHigh = Color(0xFFF8FAFC),
    surfaceContainerHighest = Color(0xFFF1F5F9),
    onBackground = Color.White,
    onSurface = DeepNavyDark,
    onSurfaceVariant = SlateLightTextSecondary,
    outline = Color(0xFF94A3B8),
    outlineVariant = SlateLightBorder,
    error = StatusDeclinedRed
)

private val LightColorScheme = lightColorScheme(
    primary = DeepNavyDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E8F0),
    onPrimaryContainer = DeepNavyDark,
    secondary = AccentCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = StatusApprovedGreen,
    background = SlateLightBackground,
    surface = SlateLightCard,
    surfaceVariant = Color(0xFFF1F5F9),
    surfaceContainer = SlateLightCard,
    surfaceContainerHigh = Color(0xFFF8FAFC),
    surfaceContainerHighest = Color(0xFFF1F5F9),
    onBackground = SlateLightTextPrimary,
    onSurface = DeepNavyDark,
    onSurfaceVariant = SlateLightTextSecondary,
    outline = Color(0xFF94A3B8),
    outlineVariant = SlateLightBorder,
    error = StatusDeclinedRed
)

/**
 * Global OutlinedTextField color preset adhering to Geometric Balance design:
 * - Text color: Dark Navy (DeepNavyDark / onSurface)
 * - Placeholder / Hint: Gray (Color(0xFF94A3B8) / outline)
 * - Container: Crisp White (SlateLightCard)
 * - Border: Subtle Slate (unfocused) / Cyan Glow (focused)
 */
@Composable
fun vmsOutlinedTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = DeepNavyDark,
    unfocusedTextColor = DeepNavyDark,
    disabledTextColor = DeepNavyDark.copy(alpha = 0.5f),
    errorTextColor = StatusDeclinedRedText,
    focusedContainerColor = SlateLightCard,
    unfocusedContainerColor = SlateLightCard,
    disabledContainerColor = Color(0xFFF1F5F9),
    errorContainerColor = StatusDeclinedRedContainer,
    cursorColor = AccentCyan,
    errorCursorColor = StatusDeclinedRed,
    focusedBorderColor = AccentCyan,
    unfocusedBorderColor = SlateLightBorder,
    disabledBorderColor = SlateLightBorder.copy(alpha = 0.5f),
    errorBorderColor = StatusDeclinedRed,
    focusedLeadingIconColor = AccentCyan,
    unfocusedLeadingIconColor = SlateLightTextSecondary,
    disabledLeadingIconColor = SlateLightTextSecondary.copy(alpha = 0.5f),
    errorLeadingIconColor = StatusDeclinedRed,
    focusedTrailingIconColor = AccentCyan,
    unfocusedTrailingIconColor = SlateLightTextSecondary,
    disabledTrailingIconColor = SlateLightTextSecondary.copy(alpha = 0.5f),
    errorTrailingIconColor = StatusDeclinedRed,
    focusedLabelColor = DeepNavyDark,
    unfocusedLabelColor = SlateLightTextSecondary,
    disabledLabelColor = SlateLightTextSecondary.copy(alpha = 0.5f),
    errorLabelColor = StatusDeclinedRed,
    focusedPlaceholderColor = Color(0xFF94A3B8),
    unfocusedPlaceholderColor = Color(0xFF94A3B8),
    disabledPlaceholderColor = Color(0xFF94A3B8).copy(alpha = 0.5f),
    errorPlaceholderColor = StatusDeclinedRed
)

/**
 * Global filled TextField color preset
 */
@Composable
fun vmsTextFieldColors(): TextFieldColors = TextFieldDefaults.colors(
    focusedTextColor = DeepNavyDark,
    unfocusedTextColor = DeepNavyDark,
    disabledTextColor = DeepNavyDark.copy(alpha = 0.5f),
    errorTextColor = StatusDeclinedRedText,
    focusedContainerColor = SlateLightCard,
    unfocusedContainerColor = SlateLightCard,
    disabledContainerColor = Color(0xFFF1F5F9),
    errorContainerColor = StatusDeclinedRedContainer,
    cursorColor = AccentCyan,
    errorCursorColor = StatusDeclinedRed,
    focusedIndicatorColor = AccentCyan,
    unfocusedIndicatorColor = SlateLightBorder,
    disabledIndicatorColor = SlateLightBorder.copy(alpha = 0.5f),
    errorIndicatorColor = StatusDeclinedRed,
    focusedLeadingIconColor = AccentCyan,
    unfocusedLeadingIconColor = SlateLightTextSecondary,
    disabledLeadingIconColor = SlateLightTextSecondary.copy(alpha = 0.5f),
    errorLeadingIconColor = StatusDeclinedRed,
    focusedTrailingIconColor = AccentCyan,
    unfocusedTrailingIconColor = SlateLightTextSecondary,
    disabledTrailingIconColor = SlateLightTextSecondary.copy(alpha = 0.5f),
    errorTrailingIconColor = StatusDeclinedRed,
    focusedLabelColor = DeepNavyDark,
    unfocusedLabelColor = SlateLightTextSecondary,
    disabledLabelColor = SlateLightTextSecondary.copy(alpha = 0.5f),
    errorLabelColor = StatusDeclinedRed,
    focusedPlaceholderColor = Color(0xFF94A3B8),
    unfocusedPlaceholderColor = Color(0xFF94A3B8),
    disabledPlaceholderColor = Color(0xFF94A3B8).copy(alpha = 0.5f),
    errorPlaceholderColor = StatusDeclinedRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


