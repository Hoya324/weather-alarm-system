package com.weather.alarm.batch.reader

import com.weather.alarm.batch.dto.NotificationItem
import com.weather.alarm.domain.notification.entity.NotificationInfo
import com.weather.alarm.domain.notification.repository.NotificationInfoRepository
import com.weather.alarm.domain.notification.type.NotificationType
import com.weather.alarm.domain.weather.entity.WeatherInfo
import com.weather.alarm.domain.weather.repository.WeatherInfoRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemReader
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalTime

@Component
class NotificationItemReader(
    private val notificationInfoRepository: NotificationInfoRepository,
    private val weatherInfoRepository: WeatherInfoRepository
) : ItemReader<NotificationItem> {

    private val logger = LoggerFactory.getLogger(NotificationItemReader::class.java)
    private var notificationItems: MutableList<NotificationItem> = mutableListOf()
    private var currentIndex = 0
    private var initialized = false

    override fun read(): NotificationItem? {
        if (!initialized) {
            initializeItems()
            initialized = true
        }

        return if (currentIndex < notificationItems.size) {
            notificationItems[currentIndex++]
        } else {
            null
        }
    }

    private fun initializeItems() {
        logger.info("=== NotificationItemReader 초기화 시작 ===")
        
        val currentTime = LocalTime.now()
        val today = LocalDate.now()
        
        // 현재 시간대에 알림을 받아야 하는 NotificationInfo 조회
        // 현재 시간의 ±30분 범위 내의 알림 대상 선택
        val startTime = currentTime.minusMinutes(30)
        val endTime = currentTime.plusMinutes(30)
        
        val enabledNotifications = notificationInfoRepository.findByNotificationEnabledTrue()
            .filter { notification ->
                val notificationTime = notification.notificationTime
                isTimeInRange(notificationTime, startTime, endTime) && 
                shouldSendNotification(notification, today)
            }

        logger.info("알림 대상 개수: ${enabledNotifications.size}")

        enabledNotifications.forEach { notification ->
            try {
                val weatherInfo = findWeatherInfoForNotification(notification, today)
                if (weatherInfo != null) {
                    val notificationItem = NotificationItem(
                        notificationInfo = notification,
                        weatherInfo = weatherInfo,
                        shouldSend = shouldSendBasedOnConditions(notification, weatherInfo)
                    )
                    notificationItems.add(notificationItem)
                    logger.debug("알림 아이템 추가: userId=${notification.user.id}, shouldSend=${notificationItem.shouldSend}")
                } else {
                    logger.warn("날씨 정보를 찾을 수 없음: userId=${notification.user.id}")
                }
            } catch (e: Exception) {
                logger.error("알림 아이템 생성 중 오류: userId=${notification.user.id}", e)
            }
        }

        logger.info("=== NotificationItemReader 초기화 완료: ${notificationItems.size}개 아이템 ===")
    }

    private fun isTimeInRange(targetTime: LocalTime, startTime: LocalTime, endTime: LocalTime): Boolean {
        return if (startTime <= endTime) {
            targetTime in startTime..endTime
        } else {
            // 자정을 넘어가는 경우 (예: 23:30 ~ 00:30)
            targetTime >= startTime || targetTime <= endTime
        }
    }

    private fun shouldSendNotification(notification: NotificationInfo, today: LocalDate): Boolean {
        return when (notification.notificationType) {
            NotificationType.DAILY -> true
            NotificationType.WEATHER -> true // 날씨 조건은 항상 체크
            NotificationType.TEMPERATURE -> true // 온도 조건은 항상 체크
        }
    }

    private fun findWeatherInfoForNotification(notification: NotificationInfo, date: LocalDate): WeatherInfo? {
        val coordinate = notification.coordinate ?: return null
        
        return weatherInfoRepository.findByUserIdAndWeatherDateAndLatitudeAndLongitude(
            notification.user.id,
            date,
            coordinate.latitude,
            coordinate.longitude
        )
    }

    private fun shouldSendBasedOnConditions(notification: NotificationInfo, weatherInfo: WeatherInfo): Boolean {
        return when (notification.notificationType) {
            NotificationType.DAILY -> true
            
            NotificationType.TEMPERATURE -> {
                checkTemperatureConditions(notification, weatherInfo)
            }
            
            NotificationType.WEATHER -> {
                checkWeatherConditions(notification, weatherInfo)
            }
        }
    }

    private fun checkTemperatureConditions(notification: NotificationInfo, weatherInfo: WeatherInfo): Boolean {
        // 온도 임계값 체크
        notification.temperatureThreshold?.let { threshold ->
            weatherInfo.temperature?.let { temp ->
                if (temp <= threshold) {
                    logger.debug("온도 조건 만족: ${temp}°C <= ${threshold}°C")
                    return true
                }
            }
        }
        return false
    }

    private fun checkWeatherConditions(notification: NotificationInfo, weatherInfo: WeatherInfo): Boolean {
        // 특정 날씨 타입 체크
        val weatherTypes = notification.getWeatherTypesList()
        if (weatherTypes.isNotEmpty()) {
            weatherInfo.weatherCondition?.let { condition ->
                if (weatherTypes.contains(condition.name)) {
                    logger.debug("날씨 조건 만족: ${condition.name}")
                    return true
                }
            }
        }

        // 날씨 경보 체크 (비, 눈, 강풍 등)
        if (weatherInfo.isWeatherAlert()) {
            logger.debug("날씨 경보 조건 만족")
            return true
        }

        return false
    }
}
