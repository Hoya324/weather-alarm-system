package com.weather.alarm.api.notification.application

import com.weather.alarm.domain.notification.application.DeleteNotificationInfoByIdDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteNotificationInfoByIdService(
    private val deleteNotificationInfoByIdDomainService: DeleteNotificationInfoByIdDomainService
) {

    @Transactional
    fun delete(id: Long) {
        deleteNotificationInfoByIdDomainService.delete(id)
    }
}