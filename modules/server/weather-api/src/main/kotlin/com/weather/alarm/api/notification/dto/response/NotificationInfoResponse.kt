package com.weather.alarm.api.notification.dto.response

import com.weather.alarm.domain.notification.dto.response.NotificationInfoDomainResponse
import com.weather.alarm.domain.notification.type.NotificationType
import java.time.LocalTime

data class NotificationInfoResponse(
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

internal fun NotificationInfoDomainResponse.response() = NotificationInfoResponse(
    id = this.id,
    slackWebHookUrl = this.slackWebHookUrl,
    address = this.address,
    latitude = this.latitude,
    longitude = this.longitude,
    notificationEnabled = this.notificationEnabled,
    notificationTime = this.notificationTime,
    notificationType = this.notificationType,
    weatherTypes = this.weatherTypes,
    temperatureThreshold = this.temperatureThreshold
)
