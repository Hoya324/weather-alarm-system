package com.weather.alarm.domain.notification.dto.response

import com.weather.alarm.domain.notification.entity.NotificationInfo
import com.weather.alarm.domain.notification.type.NotificationType
import java.time.LocalTime

data class NotificationInfoDomainResponse(
    val id: Long,
    val slackWebHookUrl: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val notificationEnabled: Boolean,
    val notificationTime: LocalTime,
    val notificationType: NotificationType,
    val weatherTypes: List<String>?,
    val temperatureThreshold: Int?
)

internal fun NotificationInfo.response() = NotificationInfoDomainResponse(
    id = this.id,
    slackWebHookUrl = this.slackWebHookUrl,
    address = this.address,
    latitude = this.getLatitude(),
    longitude = this.getLongitude(),
    notificationEnabled = this.notificationEnabled,
    notificationTime = this.notificationTime,
    notificationType = this.notificationType,
    weatherTypes = this.getWeatherTypesList(),
    temperatureThreshold = this.temperatureThreshold
)
