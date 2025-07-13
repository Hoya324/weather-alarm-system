package com.weather.alarm.api.notification.application

import com.weather.alarm.api.notification.dto.response.NotificationInfoResponse
import com.weather.alarm.api.notification.dto.response.response
import com.weather.alarm.domain.notification.application.ToggleNotificationEnabledByIdDomainService
import org.springframework.stereotype.Service

@Service
class ToggleNotificationEnabledByIdService(
    private val toggleNotificationEnabledByIdDomainService: ToggleNotificationEnabledByIdDomainService
) {

    fun toggle(id: Long): NotificationInfoResponse {
        val domainResponse = toggleNotificationEnabledByIdDomainService.toggle(id)
        return domainResponse.response()
    }
}