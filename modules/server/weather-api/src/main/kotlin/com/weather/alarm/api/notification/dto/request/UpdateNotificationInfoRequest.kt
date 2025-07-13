package com.weather.alarm.api.notification.dto.request

import com.weather.alarm.domain.notification.dto.request.UpdateNotificationInfoDomainRequest
import com.weather.alarm.domain.notification.type.NotificationType
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalTime
import javax.swing.text.html.HTML.Tag.U


data class UpdateNotificationInfoRequest(
    @field:NotBlank(message = "Slack Webhook URL은 필수입니다")
    val slackWebHookUrl: String,

    @field:NotBlank(message = "주소는 필수입니다")
    @field:Size(max = 500, message = "주소는 500자 이하여야 합니다")
    val address: String,

    @field:NotNull(message = "알림 시간은 필수입니다")
    val notificationTime: LocalTime,

    @field:NotNull(message = "알림 타입은 필수입니다")
    val notificationType: NotificationType,

    val weatherTypes: List<String>? = null,

    @field:Min(value = -50, message = "온도 임계값은 -50도 이상이어야 합니다")
    @field:Max(value = 60, message = "온도 임계값은 60도 이하여야 합니다")
    val temperatureThreshold: Int? = null,

    val notificationEnabled: Boolean = true
)

internal fun UpdateNotificationInfoRequest.domainDto() = UpdateNotificationInfoDomainRequest(
    slackWebHookUrl = this.slackWebHookUrl,
    address = this.address,
    notificationTime = this.notificationTime,
    notificationType = this.notificationType,
    weatherTypes = this.weatherTypes,
    temperatureThreshold = this.temperatureThreshold,
    notificationEnabled = this.notificationEnabled
)
