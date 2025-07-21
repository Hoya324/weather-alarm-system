package com.weather.alarm.batch.notification.reader

import com.weather.alarm.batch.dto.NotificationItem
import com.weather.alarm.domain.notification.entity.NotificationInfo
import com.weather.alarm.domain.notification.repository.NotificationInfoRepository
import com.weather.alarm.domain.notification.type.NotificationType
import com.weather.alarm.domain.weather.entity.WeatherInfo
import com.weather.alarm.domain.weather.repository.WeatherInfoRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Component
@StepScope
class NotificationItemReader(
    private val notificationInfoRepository: NotificationInfoRepository,
    private val weatherInfoRepository: WeatherInfoRepository,
    @Value("#{jobParameters['targetTime']}") private val targetTime: String?
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
        val currentTimeStr = targetTime ?: LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val today = LocalDate.now()

        logger.info("=== NotificationItemReader 초기화: 대상 시간 $currentTimeStr ===")

        // 정확한 시간(per minutes)에 매칭되는 알림 조회
        val enabledNotifications = notificationInfoRepository.findByNotificationEnabledTrue()
            .filter { notification ->
                val notificationTimeStr = notification.notificationTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                val isTimeMatch = notificationTimeStr == currentTimeStr

                logger.debug("시간 매칭 체크: userId=${notification.user.id}, 설정시간=$notificationTimeStr, 현재시간=$currentTimeStr, 매칭=$isTimeMatch")

                isTimeMatch && shouldSendNotification(notification)
            }

        logger.info("시간 매칭된 알림 대상 개수: ${enabledNotifications.size}")

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
                    logger.info("알림 아이템 추가: userId=${notification.user.id}, 알림시간=${notification.notificationTime}, shouldSend=${notificationItem.shouldSend}")
                } else {
                    logger.warn("날씨 정보를 찾을 수 없음: userId=${notification.user.id}, 좌표=${notification.coordinate}")
                }
            } catch (e: Exception) {
                logger.error("알림 아이템 생성 중 오류: userId=${notification.user.id}", e)
            }
        }

        logger.info("=== NotificationItemReader 초기화 완료: ${notificationItems.size}개 아이템 ===")
    }

    private fun shouldSendNotification(notification: NotificationInfo): Boolean {
        return when (notification.notificationType) {
            NotificationType.DAILY -> true
            NotificationType.WEATHER -> true
            NotificationType.TEMPERATURE -> true
            else -> false
        }
    }

    private fun findWeatherInfoForNotification(notification: NotificationInfo, date: LocalDate): WeatherInfo? {
        val coordinate = notification.coordinate ?: return null

        return weatherInfoRepository.findByUserIdAndWeatherDateAndNxAndNy(
            notification.user.id,
            date,
            coordinate.nx,
            coordinate.ny
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
        val weatherTypes = notification.getWeatherTypesList()
        if (weatherTypes.isNotEmpty()) {
            weatherInfo.weatherCondition?.let { condition ->
                if (weatherTypes.contains(condition.name)) {
                    logger.debug("날씨 조건 만족: ${condition.name}")
                    return true
                }
            }
        }

        if (weatherInfo.isWeatherAlert()) {
            logger.debug("날씨 경보 조건 만족")
            return true
        }

        return false
    }
}
