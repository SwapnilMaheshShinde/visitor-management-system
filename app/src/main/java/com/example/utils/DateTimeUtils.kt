package com.example.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeUtils {

    private val isoFormatters = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    )

    private val displayFormat = SimpleDateFormat("EEE, MMM d • hh:mm a", Locale.getDefault())
    private val displayTimeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    /**
     * Converts a Calendar or Date instance into a strict ISO-8601 UTC timestamp string
     */
    fun toIso8601String(calendar: Calendar): String {
        return toIso8601String(calendar.time)
    }

    fun toIso8601String(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(date)
    }

    /**
     * Converts an ISO or raw timestamp string into an elegant, user-friendly display string
     */
    fun formatDisplayDateTime(raw: String?): String {
        if (raw.isNullOrBlank()) return "Scheduled"

        for (formatter in isoFormatters) {
            try {
                val parsed = formatter.parse(raw)
                if (parsed != null) {
                    return displayFormat.format(parsed)
                }
            } catch (_: Exception) {}
        }
        return raw
    }

    /**
     * Formats an ISO string to short time (e.g. "02:30 PM")
     */
    fun formatDisplayTime(raw: String?): String {
        if (raw.isNullOrBlank()) return "02:30 PM"

        for (formatter in isoFormatters) {
            try {
                val parsed = formatter.parse(raw)
                if (parsed != null) {
                    return displayTimeFormat.format(parsed)
                }
            } catch (_: Exception) {}
        }
        return raw
    }
}
