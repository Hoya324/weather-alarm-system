package com.weather.alarm.domain.port.out

import com.weather.alarm.domain.port.out.dto.NotificationRequest

/**
 * 알림을 보내기 위한 interface
 * 의존성 역전을 위함 infrastructure이 domain을 의존
 */
interface NotificationPort {

    fun send(notificationRequest: NotificationRequest): Boolean
}
