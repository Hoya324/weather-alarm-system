package com.weather.alarm.batch.domain.dto

import com.weather.alarm.domain.notification.entity.NotificationInfo
import com.weather.alarm.domain.weather.entity.WeatherInfo

data class NotificationItem(
    val notificationInfo: NotificationInfo,
    val weatherInfo: WeatherInfo,
    val shouldSend: Boolean
)
