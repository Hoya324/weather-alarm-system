package com.weather.alarm.domain.notification.application

import com.weather.alarm.domain.notification.dto.response.NotificationInfoDomainResponse
import com.weather.alarm.domain.notification.dto.response.response
import com.weather.alarm.domain.notification.repository.NotificationInfoRepository
import com.weather.alarm.domain.user.application.FindUserByIdDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FindAllNotificationInfosByUserIdDomainService(
    private val findUserByIdDomainService: FindUserByIdDomainService,
    private val notificationInfoRepository: NotificationInfoRepository
) {

    @Transactional(readOnly = true)
    fun findAll(userId: Long): List<NotificationInfoDomainResponse> {
        val user = findUserByIdDomainService.find(userId)

        return notificationInfoRepository.findByUserId(user.id)
            .map { it.response() }
    }
}