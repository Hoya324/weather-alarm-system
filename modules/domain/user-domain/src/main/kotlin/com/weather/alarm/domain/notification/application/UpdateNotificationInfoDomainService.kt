package com.weather.alarm.domain.notification.application

import com.weather.alarm.domain.notification.dto.request.UpdateNotificationInfoDomainRequest
import com.weather.alarm.domain.notification.dto.response.NotificationInfoDomainResponse
import com.weather.alarm.domain.notification.dto.response.response
import com.weather.alarm.domain.notification.entity.NotificationInfo
import com.weather.alarm.domain.port.out.GeocodingPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateNotificationInfoDomainService(
    private val findNotificationInfoByIdDomainService: FindNotificationInfoByIdDomainService,
    private val geocodingPort: GeocodingPort
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun update(notificationId: Long, request: UpdateNotificationInfoDomainRequest): NotificationInfoDomainResponse {
        val notificationInfo = findNotificationInfoByIdDomainService.find(notificationId)

        updateCoordinate(request.address, notificationInfo)

        notificationInfo.update(
            request.slackWebHookUrl,
            request.address,
            request.notificationEnabled,
            request.notificationTime,
            request.notificationType,
            request.weatherTypes?.joinToString(","),
            request.temperatureThreshold,
        )

        return notificationInfo.response()
    }

    private fun updateCoordinate(
        address: String?,
        notificationInfo: NotificationInfo
    ) {
        if (address != null && notificationInfo.address != address) {
            val coordinate = geocodingPort.getCoordinatesByAddress(address)

            if (coordinate == null) {
                logger.warn("주소 업데이트 시 좌표 변환 실패: $address")
            } else {
                notificationInfo.updateCoordinate(coordinate)
            }

        }
    }
}