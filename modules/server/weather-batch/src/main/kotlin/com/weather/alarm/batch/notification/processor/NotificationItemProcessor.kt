package com.weather.alarm.batch.notification.processor

import com.weather.alarm.batch.dto.NotificationItem
import com.weather.alarm.batch.dto.ProcessedNotificationItem
import com.weather.alarm.domain.weather.type.WeatherCondition
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

@Component
class NotificationItemProcessor : ItemProcessor<NotificationItem, ProcessedNotificationItem> {

    private val logger = LoggerFactory.getLogger(NotificationItemProcessor::class.java)

    override fun process(item: NotificationItem): ProcessedNotificationItem? {
        try {
            if (!item.shouldSend) {
                logger.debug("알림 조건 불만족으로 건너뜀: userId=${item.notificationInfo.user.id}")
                return null
            }

            val notificationInfo = item.notificationInfo
            val weatherInfo = item.weatherInfo

            val message = buildSlackMessage(notificationInfo, weatherInfo)

            logger.debug("알림 메시지 생성 완료: userId=${notificationInfo.user.id}")

            return ProcessedNotificationItem(
                webhookUrl = notificationInfo.slackWebHookUrl,
                message = message,
                userId = notificationInfo.user.id,
                notificationId = notificationInfo.id
            )
        } catch (e: Exception) {
            logger.error("알림 처리 중 오류: userId=${item.notificationInfo.user.id}", e)
            return null
        }
    }

    private fun buildSlackMessage(
        notificationInfo: com.weather.alarm.domain.notification.entity.NotificationInfo,
        weatherInfo: com.weather.alarm.domain.weather.entity.WeatherInfo
    ): String {
        val address = notificationInfo.address
        val date = weatherInfo.weatherDate.format(DateTimeFormatter.ofPattern("MM월 dd일"))
        val notificationType = when (notificationInfo.notificationType) {
            com.weather.alarm.domain.notification.type.NotificationType.DAILY -> "일일 날씨"
            com.weather.alarm.domain.notification.type.NotificationType.WEATHER -> "날씨 조건 알림"
            com.weather.alarm.domain.notification.type.NotificationType.TEMPERATURE -> "온도 조건 알림"
        }

        val sb = StringBuilder()
        sb.append("🌤️ *${address} ${notificationType}* (${date})\n\n")

        // 알림 타입별 조건 표시
        when (notificationInfo.notificationType) {
            com.weather.alarm.domain.notification.type.NotificationType.TEMPERATURE -> {
                notificationInfo.temperatureThreshold?.let { threshold ->
                    weatherInfo.temperature?.let { temp ->
                        if (temp <= threshold) {
                            sb.append("🚨 **온도 알림**: 설정 온도(${threshold}°C) 이하입니다!\n\n")
                        }
                    }
                }
            }

            com.weather.alarm.domain.notification.type.NotificationType.WEATHER -> {
                val weatherTypes = notificationInfo.getWeatherTypesList()
                if (weatherTypes.isNotEmpty()) {
                    weatherInfo.weatherCondition?.let { condition ->
                        if (weatherTypes.contains(condition.name)) {
                            sb.append("🚨 **날씨 알림**: 설정한 날씨 조건(${getWeatherDescription(condition)})이 감지되었습니다!\n\n")
                        }
                    }
                }

                if (weatherInfo.isWeatherAlert()) {
                    sb.append("🚨 **날씨 경보**: 주의가 필요한 날씨입니다!\n\n")
                }
            }

            com.weather.alarm.domain.notification.type.NotificationType.DAILY -> {
                // 일반 인사말
            }
        }

        weatherInfo.temperature?.let { temp ->
            sb.append("🌡️ **현재 온도**: ${temp.toInt()}°C")

            // 체감온도
            weatherInfo.getFeelsLikeTemperature()?.let { feelsLike ->
                if (feelsLike != temp) {
                    sb.append(" (체감 ${feelsLike.toInt()}°C)")
                }
            }
            sb.append("\n")
        }

        // 최저/최고 온도
        if (weatherInfo.temperatureMin != null || weatherInfo.temperatureMax != null) {
            sb.append("📊 **온도 범위**: ")
            weatherInfo.temperatureMin?.let { min -> sb.append("최저 ${min.toInt()}°C") }
            if (weatherInfo.temperatureMin != null && weatherInfo.temperatureMax != null) {
                sb.append(" / ")
            }
            weatherInfo.temperatureMax?.let { max -> sb.append("최고 ${max.toInt()}°C") }
            sb.append("\n")
        }

        // 날씨 상태
        weatherInfo.weatherCondition?.let { condition ->
            val emoji = getWeatherEmoji(condition)
            val description = getWeatherDescription(condition)
            sb.append("☁️ **날씨**: ${emoji} ${description}\n")
        }

        // 강수 정보
        weatherInfo.precipitationProbability?.let { probability ->
            sb.append("☔ **강수확률**: ${probability}%")
            weatherInfo.precipitation?.let { precipitation ->
                if (precipitation > 0) {
                    sb.append(" (${precipitation}mm)")
                }
            }
            sb.append("\n")
        }

        // 습도
        weatherInfo.humidity?.let { humidity ->
            sb.append("💧 **습도**: ${humidity}%\n")
        }

        // 바람
        weatherInfo.windSpeed?.let { windSpeed ->
            sb.append("💨 **바람**: ${windSpeed}m/s")
            weatherInfo.windDirection?.let { direction ->
                sb.append(" ${direction}")
            }
            sb.append("\n")
        }

        // 추가 경고 메시지 (DAILY 타입에서만 표시)
        if (notificationInfo.notificationType == com.weather.alarm.domain.notification.type.NotificationType.DAILY && weatherInfo.isWeatherAlert()) {
            sb.append("\n⚠️ **날씨 경보**\n")

            weatherInfo.precipitationProbability?.let { prob ->
                if (prob >= 70) sb.append("• 높은 강수확률 (${prob}%)\n")
            }

            weatherInfo.windSpeed?.let { wind ->
                if (wind >= 15.0) sb.append("• 강풍 주의 (${wind}m/s)\n")
            }

            weatherInfo.temperature?.let { temp ->
                when {
                    temp <= 0 -> sb.append("• 한파 주의 (${temp.toInt()}°C)\n")
                    temp >= 35 -> sb.append("• 폭염 주의 (${temp.toInt()}°C)\n")
                    else -> {}
                }
            }

            weatherInfo.uvIndex?.let { uv ->
                if (uv >= 8) sb.append("• 높은 자외선 지수 (${uv})\n")
            }
        }

        // 외출 추천 정보
        if (weatherInfo.isGoodWeatherForOutdoor()) {
            sb.append("\n✨ **외출하기 좋은 날씨입니다!**")
        }

        return sb.toString()
    }

    private fun getWeatherEmoji(condition: WeatherCondition): String {
        return when (condition) {
            WeatherCondition.CLEAR -> "☀️"
            WeatherCondition.PARTLY_CLOUDY -> "⛅"
            WeatherCondition.CLOUDY -> "☁️"
            WeatherCondition.LIGHT_RAIN -> "🌦️"
            WeatherCondition.HEAVY_RAIN -> "🌧️"
            WeatherCondition.SNOW -> "🌨️"
            WeatherCondition.SLEET -> "🌨️"
            WeatherCondition.THUNDERSTORM -> "⛈️"
            WeatherCondition.FOG -> "🌫️"
            WeatherCondition.WIND -> "💨"
        }
    }

    private fun getWeatherDescription(condition: WeatherCondition): String {
        return when (condition) {
            WeatherCondition.CLEAR -> "맑음"
            WeatherCondition.PARTLY_CLOUDY -> "구름많음"
            WeatherCondition.CLOUDY -> "흐림"
            WeatherCondition.LIGHT_RAIN -> "비"
            WeatherCondition.HEAVY_RAIN -> "많은 비"
            WeatherCondition.SNOW -> "눈"
            WeatherCondition.SLEET -> "진눈깨비"
            WeatherCondition.THUNDERSTORM -> "천둥번개"
            WeatherCondition.FOG -> "안개"
            WeatherCondition.WIND -> "강풍"
        }
    }
}
