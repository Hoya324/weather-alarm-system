package com.weather.alarm.batch.domain.dto

data class ProcessedNotificationItem(
    val webhookUrl: String,
    val message: String,
    val userId: Long,
    val notificationId: Long
)
