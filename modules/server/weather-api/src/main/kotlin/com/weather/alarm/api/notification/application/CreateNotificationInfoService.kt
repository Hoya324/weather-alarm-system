package com.weather.alarm.api.notification.application

import com.weather.alarm.api.notification.dto.request.CreateNotificationInfoRequest
import com.weather.alarm.api.notification.dto.request.domainDto
import com.weather.alarm.api.notification.dto.response.CreateNotificationInfoResponse
import com.weather.alarm.domain.notification.application.CreateNotificationInfoDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateNotificationInfoService(
    private val createNotificationInfoDomainService: CreateNotificationInfoDomainService
) {

    @Transactional
    fun create(request: CreateNotificationInfoRequest): CreateNotificationInfoResponse {
        val domainDto = request.domainDto()

        val result = createNotificationInfoDomainService.create(domainDto)

        return CreateNotificationInfoResponse(
            notificationInfoId = result.notificationInfo.id,
            message = result.message
        )
    }
}