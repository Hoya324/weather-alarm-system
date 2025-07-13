package com.weather.alarm.api.notification.application

import com.weather.alarm.api.notification.dto.request.UpdateNotificationInfoRequest
import com.weather.alarm.api.notification.dto.request.domainDto
import com.weather.alarm.api.notification.dto.response.NotificationInfoResponse
import com.weather.alarm.api.notification.dto.response.response
import com.weather.alarm.domain.notification.application.UpdateNotificationInfoDomainService
import com.weather.alarm.domain.notification.dto.request.UpdateNotificationInfoDomainRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateNotificationInfoService(
    private val updateNotificationInfoDomainService: UpdateNotificationInfoDomainService
) {

    @Transactional
    fun update(notificationId: Long, request: UpdateNotificationInfoRequest): NotificationInfoResponse {
        val domainDto = request.domainDto()

        val domainResponse = updateNotificationInfoDomainService.update(notificationId, domainDto)

        return domainResponse.response()
    }
}