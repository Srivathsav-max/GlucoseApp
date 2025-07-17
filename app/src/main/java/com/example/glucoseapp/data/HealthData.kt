package com.example.glucoseapp.data

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class StepsData(
    val count: Long,
    val timestamp: Instant,
    val startTime: Instant,
    val endTime: Instant
) {
    fun getFormattedDateTime(): String {
        val localDateTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
        return localDateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
    }
    
    fun getFormattedDate(): String {
        val localDateTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
        return localDateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    }
}

data class HeartRateData(
    val beatsPerMinute: Long,
    val timestamp: Instant
) {
    fun getFormattedDateTime(): String {
        val localDateTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
        return localDateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
    }
    
    fun getFormattedDate(): String {
        val localDateTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
        return localDateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    }
}

data class GlucoseData(
    val level: Double,
    val unit: String,
    val timestamp: Instant,
    val mealType: String? = null
) {
    fun getFormattedDateTime(): String {
        val localDateTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
        return localDateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
    }
    
    fun getFormattedDate(): String {
        val localDateTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
        return localDateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    }
    
    fun getFormattedLevel(): String {
        return String.format("%.1f %s", level, unit)
    }
}

data class LatestVitals(
    val latestSteps: StepsData?,
    val latestHeartRate: HeartRateData?,
    val latestGlucose: GlucoseData?
)
