package com.weather.alarm.domain.notification.application

import com.weather.alarm.domain.notification.dto.request.CreateNotificationInfoDomainRequest
import com.weather.alarm.domain.notification.dto.response.CreateNotificationInfoDomainResult
import com.weather.alarm.domain.notification.dto.response.response
import com.weather.alarm.domain.notification.entity.NotificationInfo
import com.weather.alarm.domain.notification.repository.NotificationInfoRepository
import com.weather.alarm.domain.port.out.GeocodingPort
import com.weather.alarm.domain.user.application.FindVerifiedUserByIdDomainService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateNotificationInfoDomainService(
    private val notificationInfoRepository: NotificationInfoRepository,
    private val findVerifiedUserByIdDomainService: FindVerifiedUserByIdDomainService,
    private val geocodingPort: GeocodingPort

) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun create(request: CreateNotificationInfoDomainRequest): CreateNotificationInfoDomainResult {

        logger.info("알림 정보 생성 시작 - 사용자 ID: ${request.userId}, 주소: ${request.address}")

        val user = findVerifiedUserByIdDomainService.find(request.userId)

        val coordinate = geocodingPort.getCoordinatesByAddress(request.address)

        val notificationInfo = NotificationInfo(
            _slackWebHookUrl = request.slackWebHookUrl,
            _address = request.address,
            _coordinate = coordinate,
            _notificationEnabled = request.notificationEnabled,
            _notificationTime = request.notificationTime,
            _notificationType = request.notificationType,
            _weatherTypes = request.weatherTypes?.joinToString(","),
            _temperatureThreshold = request.temperatureThreshold,
            _user = user
        )

        user.addNotificationInfo(notificationInfo)
        notificationInfoRepository.save(notificationInfo)

        logger.info("알림 정보 생성 완료 - ID: ${notificationInfo.id}")

        val response = notificationInfo.response()
        val hasCoordinate = coordinate != null
        val message = if (hasCoordinate) {
            "알림 설정이 성공적으로 생성되었습니다."
        } else {
            "알림 설정이 생성되었습니다. 단, 입력하신 주소의 정확한 좌표를 찾을 수 없어 대략적인 위치로 날씨 정보를 제공할 예정입니다. 더 정확한 주소로 수정하시려면 설정을 업데이트해주세요."
        }

        return CreateNotificationInfoDomainResult(
            notificationInfo = response,
            hasCoordinate = hasCoordinate,
            message = message
        )
    }
}
