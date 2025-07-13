package com.weather.alarm.domain.notification.application

import com.weather.alarm.domain.exception.DataNotFoundException
import com.weather.alarm.domain.notification.entity.NotificationInfo
import com.weather.alarm.domain.notification.repository.NotificationInfoRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FindNotificationInfoByIdDomainService(
    private val notificationInfoRepository: NotificationInfoRepository
) {

    @Transactional(readOnly = true)
    fun find(id: Long): NotificationInfo {
        return notificationInfoRepository.findByIdOrNull(id)
            ?: throw DataNotFoundException("알림 설정을 찾을 수 없습니다. ID: $id")
    }
}