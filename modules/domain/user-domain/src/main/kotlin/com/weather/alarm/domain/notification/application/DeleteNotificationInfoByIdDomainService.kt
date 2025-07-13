package com.weather.alarm.domain.notification.application

import com.weather.alarm.domain.notification.repository.NotificationInfoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteNotificationInfoByIdDomainService(
    private val findNotificationInfoByIdDomainService: FindNotificationInfoByIdDomainService,
    private val notificationInfoRepository: NotificationInfoRepository
) {

    @Transactional
    fun delete(id: Long) {
        val notificationInfo = findNotificationInfoByIdDomainService.find(id)

        notificationInfo.delete()

        notificationInfoRepository.delete(notificationInfo)
    }
}