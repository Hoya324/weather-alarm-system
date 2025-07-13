package com.weather.alarm.api.notification.application

import com.weather.alarm.api.notification.dto.response.NotificationInfoResponse
import com.weather.alarm.api.notification.dto.response.response
import com.weather.alarm.domain.notification.application.FindAllNotificationInfosByUserIdDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FindAllNotificationInfosByUserIdService(
    private val findAllNotificationInfosByUserIdDomainService: FindAllNotificationInfosByUserIdDomainService
) {

    @Transactional(readOnly = true)
    fun find(userId: Long): List<NotificationInfoResponse> {
        return findAllNotificationInfosByUserIdDomainService.findAll(userId)
            .map { domainResponse ->
                domainResponse.response()
            }
    }
}