package com.weather.alarm.infrastructure.slack.utils

import com.weather.alarm.domain.notification.entity.NotificationInfo

import com.weather.alarm.domain.notification.type.NotificationType
import com.weather.alarm.domain.weather.entity.WeatherInfo
import com.weather.alarm.domain.weather.type.WeatherCondition
import com.weather.alarm.domain.weather.type.WeatherStatus
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

@Component
class SlackMessageBuilder {

    fun buildWeatherMessage(notificationInfo: NotificationInfo, weatherInfo: WeatherInfo): String {
        val sb = StringBuilder()

        appendHeader(sb, notificationInfo, weatherInfo)

        if (weatherInfo.isWeatherAlert()) {
            appendEmergencyAlerts(sb, weatherInfo)
        }

        appendConditionalAlerts(sb, notificationInfo, weatherInfo)
        appendCurrentWeather(sb, weatherInfo)
        appendTodayForecast(sb, weatherInfo)
        appendDetailedInfo(sb, weatherInfo)
        appendRecommendations(sb, weatherInfo)

        return sb.toString()
    }

    private fun appendHeader(sb: StringBuilder, notificationInfo: NotificationInfo, weatherInfo: WeatherInfo) {
        val address = notificationInfo.address
        val date = weatherInfo.weatherDate.format(DateTimeFormatter.ofPattern("MM월 dd일"))
        val notificationType = getNotificationTypeText(notificationInfo.notificationType)
        val statusIcon = getWeatherStatusIcon(weatherInfo.getOverallWeatherStatus())

        sb.append("${statusIcon} *${address} ${notificationType}* (${date})\n\n")
    }

    private fun appendEmergencyAlerts(sb: StringBuilder, weatherInfo: WeatherInfo) {
        val alerts = weatherInfo.getWeatherAlerts()
        if (alerts.isNotEmpty()) {
            sb.append("🚨 *날씨 경보 발령!* 🚨\n")
            alerts.forEach { alert ->
                sb.append("• ${alert}\n")
            }
            sb.append("\n")
        }
    }

    private fun appendConditionalAlerts(
        sb: StringBuilder,
        notificationInfo: NotificationInfo,
        weatherInfo: WeatherInfo
    ) {
        val message = when (notificationInfo.notificationType) {
            NotificationType.TEMPERATURE -> {
                buildTemperatureAlert(notificationInfo, weatherInfo)
            }

            NotificationType.WEATHER -> {
                buildWeatherAlert(notificationInfo, weatherInfo)
            }

            NotificationType.DAILY -> null
        }

        message?.let {
            sb.append("${it}\n\n")
        }
    }

    private fun buildTemperatureAlert(notificationInfo: NotificationInfo, weatherInfo: WeatherInfo): String? {
        return notificationInfo.temperatureThreshold?.let { threshold ->
            weatherInfo.getCurrentTemp()?.let { temp ->
                if (temp <= threshold) {
                    "🌡️ *온도 알림*: 설정 온도(${threshold}°C) 이하! (현재 ${temp.toInt()}°C)"
                } else null
            }
        }
    }

    private fun buildWeatherAlert(notificationInfo: NotificationInfo, weatherInfo: WeatherInfo): String? {
        val weatherTypes = notificationInfo.getWeatherTypesList()
        return if (weatherTypes.isNotEmpty()) {
            weatherInfo.weatherCondition?.let { condition ->
                if (weatherTypes.contains(condition.name)) {
                    "☁️ *날씨 알림*: 설정한 날씨 조건(${getWeatherDescription(condition)})이 감지되었습니다!"
                } else null
            }
        } else null
    }

    private fun appendCurrentWeather(sb: StringBuilder, weatherInfo: WeatherInfo) {
        sb.append("📊 *현재 상황*\n")

        // 온도 (체감온도 포함)
        weatherInfo.getCurrentTemp()?.let { temp ->
            sb.append("🌡️ 기온: ${temp.toInt()}°C")

            weatherInfo.getFeelsLikeTemperature()?.let { feelsLike ->
                if (Math.abs(feelsLike - temp) > 2) {
                    sb.append(" (체감 ${feelsLike.toInt()}°C)")
                }
            }
            sb.append("\n")
        }

        // 습도
        weatherInfo.getCurrentHumidityValue()?.let { humidity ->
            val comfortLevel = getHumidityComfortLevel(humidity)
            sb.append("💧 습도: ${humidity}% (${comfortLevel})\n")
        }

        // 바람
        weatherInfo.getCurrentWindSpeedValue()?.let { windSpeed ->
            sb.append("💨 바람: ${windSpeed}m/s")

            weatherInfo.getWindDirectionDescription()?.let { direction ->
                sb.append(" ${direction}")
            }

            weatherInfo.getWindStrengthDescription()?.let { strength ->
                sb.append(" (${strength})")
            }
            sb.append("\n")
        }

        // 현재 강수
        if (weatherInfo.hasCurrentPrecipitation()) {
            weatherInfo.currentPrecipitation?.let { rain ->
                sb.append("🌧️ 현재 강수: ${rain}mm/h")

                weatherInfo.currentPrecipitationType?.let { type ->
                    val precipType = getPrecipitationTypeText(type)
                    sb.append(" (${precipType})")
                }
                sb.append("\n")
            }
        }

        sb.append("\n")
    }

    private fun appendTodayForecast(sb: StringBuilder, weatherInfo: WeatherInfo) {
        sb.append("📋 *오늘 예보*\n")

        // 최저/최고 온도
        appendTemperatureRange(sb, weatherInfo)

        // 하늘 상태 및 날씨
        appendWeatherCondition(sb, weatherInfo)

        // 강수확률
        appendPrecipitationProbability(sb, weatherInfo)

        sb.append("\n")
    }

    private fun appendTemperatureRange(sb: StringBuilder, weatherInfo: WeatherInfo) {
        if (weatherInfo.temperatureMin != null || weatherInfo.temperatureMax != null) {
            sb.append("🌡️ 온도: ")
            weatherInfo.temperatureMin?.let { min -> sb.append("최저 ${min.toInt()}°C") }
            if (weatherInfo.temperatureMin != null && weatherInfo.temperatureMax != null) {
                sb.append(" / ")
            }
            weatherInfo.temperatureMax?.let { max -> sb.append("최고 ${max.toInt()}°C") }
            sb.append("\n")
        }
    }

    private fun appendWeatherCondition(sb: StringBuilder, weatherInfo: WeatherInfo) {
        val skyDescription = weatherInfo.getSkyDescription()
        val precipDescription = weatherInfo.getPrecipitationTypeDescription()

        val weatherText = when {
            precipDescription != null && precipDescription != "강수없음" -> {
                val emoji = getPrecipitationEmoji(weatherInfo.precipitationType)
                val skyInfo = skyDescription?.let { " (${it})" } ?: ""
                "${emoji} ${precipDescription}${skyInfo}"
            }

            skyDescription != null -> {
                val emoji = getSkyEmoji(weatherInfo.skyCondition)
                "${emoji} ${skyDescription}"
            }

            weatherInfo.weatherCondition != null -> {
                val emoji = getWeatherEmoji(weatherInfo.weatherCondition!!)
                val description = getWeatherDescription(weatherInfo.weatherCondition!!)
                "${emoji} ${description}"
            }

            else -> null
        }

        weatherText?.let {
            sb.append("☁️ 날씨: ${it}\n")
        }
    }

    private fun appendPrecipitationProbability(sb: StringBuilder, weatherInfo: WeatherInfo) {
        weatherInfo.precipitationProbability?.let { probability ->
            sb.append("☔ 강수확률: ${probability}%")
            weatherInfo.precipitation?.let { precipitation ->
                if (precipitation > 0) {
                    sb.append(" (예상 ${precipitation}mm)")
                }
            }
            sb.append("\n")
        }
    }

    private fun appendDetailedInfo(sb: StringBuilder, weatherInfo: WeatherInfo) {
        val detailItems = buildDetailItems(weatherInfo)

        if (detailItems.isNotEmpty()) {
            sb.append("🔍 *상세 정보*\n")
            detailItems.forEach { item ->
                sb.append("${item}\n")
            }
            sb.append("\n")
        }
    }

    private fun buildDetailItems(weatherInfo: WeatherInfo): List<String> {
        val items = mutableListOf<String>()

        weatherInfo.visibilityKm?.let { visibility ->
            items.add("👁️ 가시거리: ${visibility}km")
        }

        weatherInfo.uvIndex?.let { uv ->
            val uvLevel = getUvLevel(uv)
            items.add("☀️ 자외선: ${uv} (${uvLevel})")
        }

        weatherInfo.airPressure?.let { pressure ->
            items.add("📊 기압: ${pressure}hPa")
        }

        weatherInfo.lightning?.let { lightning ->
            if (lightning > 0) {
                items.add("⚡ 낙뢰 위험도: ${lightning}kA/㎢")
            }
        }

        return items
    }

    private fun appendRecommendations(sb: StringBuilder, weatherInfo: WeatherInfo) {
        val recommendations = weatherInfo.getAllRecommendations()

        if (recommendations.isNotEmpty()) {
            sb.append("💡 *권고사항*\n")
            recommendations.forEach { recommendation ->
                sb.append("${recommendation}\n")
            }
        }
    }


    private fun getNotificationTypeText(type: NotificationType): String {
        return when (type) {
            NotificationType.DAILY -> "일일 날씨"
            NotificationType.WEATHER -> "날씨 조건 알림"
            NotificationType.TEMPERATURE -> "온도 조건 알림"
        }
    }

    private fun getWeatherStatusIcon(status: WeatherStatus): String {
        return when (status) {
            WeatherStatus.GOOD -> "🌤️"
            WeatherStatus.CAUTION -> "⚠️"
            WeatherStatus.ALERT -> "🚨"
        }
    }

    private fun getHumidityComfortLevel(humidity: Int): String {
        return when {
            humidity < 40 -> "건조"
            humidity > 60 -> "습함"
            else -> "적정"
        }
    }

    private fun getPrecipitationTypeText(type: String): String {
        return when (type) {
            "1" -> "비"
            "2" -> "비/눈"
            "3" -> "눈"
            "4" -> "소나기"
            else -> "강수"
        }
    }

    private fun getUvLevel(uv: Int): String {
        return when {
            uv <= 2 -> "낮음"
            uv <= 5 -> "보통"
            uv <= 7 -> "높음"
            uv <= 10 -> "매우높음"
            else -> "위험"
        }
    }

    private fun getSkyEmoji(skyCondition: Int?): String {
        return when (skyCondition) {
            1 -> "☀️"
            3 -> "⛅"
            4 -> "☁️"
            else -> "🌤️"
        }
    }

    private fun getPrecipitationEmoji(precipitationType: Int?): String {
        return when (precipitationType) {
            1 -> "🌧️"
            2 -> "🌨️"
            3 -> "❄️"
            4 -> "⛈️"
            else -> "🌦️"
        }
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
