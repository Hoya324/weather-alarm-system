package com.weather.alarm.batch.core.chunk.reader

import com.weather.alarm.batch.domain.dto.NotificationItem
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
import java.time.format.DateTimeParseException

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
            try {
                initializeItems()
                initialized = true
            } catch (e: Exception) {
                logger.error("NotificationItemReader 초기화 실패", e)
                return null
            }
        }

        return if (currentIndex < notificationItems.size) {
            notificationItems[currentIndex++]
        } else {
            null
        }
    }

    private fun initializeItems() {
        val currentTimeStr = parseTargetTime()
        val today = LocalDate.now()

        logger.info("=== NotificationItemReader 초기화: 대상 시간 $currentTimeStr ===")

        try {
            val enabledNotifications = findEnabledNotifications(currentTimeStr)
            logger.info("시간 매칭된 알림 대상 개수: ${enabledNotifications.size}")

            processNotifications(enabledNotifications, today)

        } catch (e: Exception) {
            logger.error("알림 초기화 중 예외 발생", e)
            throw e
        }

        logger.info("=== NotificationItemReader 초기화 완료: ${notificationItems.size}개 아이템 ===")
    }

    private fun parseTargetTime(): String {
        return try {
            targetTime?.let { time ->
                LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"))
                time
            } ?: LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: DateTimeParseException) {
            logger.warn("잘못된 targetTime 형식: $targetTime, 현재 시간 사용", e)
            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        }
    }

    private fun findEnabledNotifications(currentTimeStr: String): List<NotificationInfo> {
        return try {
            notificationInfoRepository.findByNotificationEnabledTrue()
                .filter { notification ->
                    val isTimeMatch = isTimeMatched(notification, currentTimeStr)
                    val shouldSend = shouldSendNotification(notification)

                    logger.debug(
                        "시간 매칭 체크: userId=${notification.user.id}, " +
                                "설정시간=${notification.notificationTime.format(DateTimeFormatter.ofPattern("HH:mm"))}, " +
                                "현재시간=$currentTimeStr, 매칭=$isTimeMatch, 발송가능=$shouldSend"
                    )

                    isTimeMatch && shouldSend
                }
        } catch (e: Exception) {
            logger.error("활성화된 알림 조회 실패", e)
            emptyList()
        }
    }

    private fun isTimeMatched(notification: NotificationInfo, currentTimeStr: String): Boolean {
        return try {
            val notificationTimeStr = notification.notificationTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            notificationTimeStr == currentTimeStr
        } catch (e: Exception) {
            logger.warn("알림 시간 비교 실패: userId=${notification.user.id}", e)
            false
        }
    }

    private fun processNotifications(notifications: List<NotificationInfo>, today: LocalDate) {
        notifications.forEach { notification ->
            try {
                processSingleNotification(notification, today)
            } catch (e: Exception) {
                logger.error("개별 알림 처리 실패: userId=${notification.user.id}", e)
                // 개별 알림 실패가 전체를 중단시키지 않도록 계속 진행
            }
        }
    }

    private fun processSingleNotification(notification: NotificationInfo, today: LocalDate) {
        val weatherInfo = findWeatherInfoForNotification(notification, today)

        if (weatherInfo != null) {
            val shouldSend = shouldSendBasedOnConditions(notification, weatherInfo)
            val notificationItem = NotificationItem(
                notificationInfo = notification,
                weatherInfo = weatherInfo,
                shouldSend = shouldSend
            )

            notificationItems.add(notificationItem)
            logger.info(
                "알림 아이템 추가: userId=${notification.user.id}, " +
                        "알림시간=${notification.notificationTime}, shouldSend=$shouldSend"
            )
        } else {
            logger.warn(
                "날씨 정보를 찾을 수 없음: userId=${notification.user.id}, " +
                        "좌표=${notification.coordinate}"
            )
        }
    }

    private fun shouldSendNotification(notification: NotificationInfo): Boolean {
        return try {
            when (notification.notificationType) {
                NotificationType.DAILY -> true
                NotificationType.WEATHER -> true
                NotificationType.TEMPERATURE -> true
                else -> {
                    logger.debug("지원되지 않는 알림 타입: ${notification.notificationType}")
                    false
                }
            }
        } catch (e: Exception) {
            logger.warn("알림 타입 확인 실패: userId=${notification.user.id}", e)
            false
        }
    }

    private fun findWeatherInfoForNotification(notification: NotificationInfo, date: LocalDate): WeatherInfo? {
        return try {
            val coordinate = notification.coordinate ?: return null

            weatherInfoRepository.findByUserIdAndWeatherDateAndNxAndNy(
                notification.user.id,
                date,
                coordinate.nx,
                coordinate.ny
            )
        } catch (e: Exception) {
            logger.error("날씨 정보 조회 실패: userId=${notification.user.id}", e)
            null
        }
    }

    private fun shouldSendBasedOnConditions(notification: NotificationInfo, weatherInfo: WeatherInfo): Boolean {
        return try {
            when (notification.notificationType) {
                NotificationType.DAILY -> true
                NotificationType.TEMPERATURE -> checkTemperatureConditions(notification, weatherInfo)
                NotificationType.WEATHER -> checkWeatherConditions(notification, weatherInfo)
            }
        } catch (e: Exception) {
            logger.error("알림 조건 확인 실패: userId=${notification.user.id}", e)
            false
        }
    }

    private fun checkTemperatureConditions(notification: NotificationInfo, weatherInfo: WeatherInfo): Boolean {
        return try {
            notification.temperatureThreshold?.let { threshold ->
                weatherInfo.temperature?.let { temp ->
                    if (temp <= threshold) {
                        logger.debug("온도 조건 만족: ${temp}°C <= ${threshold}°C")
                        return true
                    }
                }
            }
            false
        } catch (e: Exception) {
            logger.error("온도 조건 확인 실패", e)
            false
        }
    }

    private fun checkWeatherConditions(notification: NotificationInfo, weatherInfo: WeatherInfo): Boolean {
        return try {
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

            false
        } catch (e: Exception) {
            logger.error("날씨 조건 확인 실패", e)
            false
        }
    }
}
