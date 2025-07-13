package com.weather.alarm.domain.notification.application

import com.weather.alarm.domain.notification.dto.response.NotificationInfoDomainResponse
import com.weather.alarm.domain.notification.dto.response.response
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ToggleNotificationEnabledByIdDomainService(
    private val findNotificationInfoByIdDomainService: FindNotificationInfoByIdDomainService
) {

    @Transactional
    fun toggle(id: Long): NotificationInfoDomainResponse {
        val notificationInfo = findNotificationInfoByIdDomainService.find(id)

        notificationInfo.toggle()

        return notificationInfo.response()
    }
}