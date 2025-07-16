package com.weather.alarm.domain.notification.repository

import com.weather.alarm.domain.notification.entity.NotificationInfo
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationInfoRepository : JpaRepository<NotificationInfo, Long> {
    fun findByUserId(userId: Long): List<NotificationInfo>

    fun findByNotificationEnabledTrue(): List<NotificationInfo>
}