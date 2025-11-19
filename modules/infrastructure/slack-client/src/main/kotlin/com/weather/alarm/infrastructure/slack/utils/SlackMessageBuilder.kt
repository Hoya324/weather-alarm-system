package com.weather.alarm.infrastructure.slack.utils

import com.weather.alarm.domain.notification.entity.NotificationInfo
import com.weather.alarm.domain.notification.type.NotificationType
import com.weather.alarm.domain.weather.entity.WeatherInfo
import com.weather.alarm.domain.weather.type.WeatherCondition
import com.weather.alarm.domain.weather.type.WeatherStatus
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter
import kotlin.random.Random

@Component
class SlackMessageBuilder {

    fun buildWeatherMessage(notificationInfo: NotificationInfo, weatherInfo: WeatherInfo): String {
        val sb = StringBuilder()

        appendHeader(sb, notificationInfo, weatherInfo)
        appendSeparator(sb)

        if (weatherInfo.isWeatherAlert()) {
            appendEmergencyAlerts(sb, weatherInfo)
            appendSeparator(sb)
        }

        appendConditionalAlerts(sb, notificationInfo, weatherInfo)
        appendCurrentWeather(sb, weatherInfo)
        appendTodayForecast(sb, weatherInfo)
        appendDetailedInfo(sb, weatherInfo)
        appendRecommendations(sb, weatherInfo)
        appendDailyQuote(sb)

        return sb.toString()
    }

    private fun appendHeader(
        sb: StringBuilder,
        notificationInfo: NotificationInfo,
        weatherInfo: WeatherInfo
    ) {
        val address = notificationInfo.address
        val date = weatherInfo.weatherDate.format(DateTimeFormatter.ofPattern("MM월 dd일"))
        val notificationType = getNotificationTypeText(notificationInfo.notificationType)
        val statusIcon = getWeatherStatusIcon(weatherInfo.getOverallWeatherStatus())

        sb.append("🏠 *${address} ${notificationType}*\n")
        sb.append("📅 ${date} ${statusIcon}\n")
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

    private fun buildTemperatureAlert(
        notificationInfo: NotificationInfo,
        weatherInfo: WeatherInfo
    ): String? {
        return notificationInfo.temperatureThreshold?.let { threshold ->
            weatherInfo.getCurrentTemp()?.let { temp ->
                if (temp <= threshold) {
                    "🌡️ *온도 알림*: 설정 온도(${threshold}°C) 이하! (현재 ${temp.toInt()}°C)"
                } else null
            }
        }
    }

    private fun buildWeatherAlert(
        notificationInfo: NotificationInfo,
        weatherInfo: WeatherInfo
    ): String? {
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
        sb.append("\n🌤️ *현재 날씨 상황*\n\n")

        val weatherItems = mutableListOf<String>()

        // 온도 (체감온도 포함)
        weatherInfo.getCurrentTemp()?.let { temp ->
            var tempText = "🌡️ *기온:* ${temp.toInt()}°C"
            weatherInfo.getFeelsLikeTemperature()?.let { feelsLike ->
                if (Math.abs(feelsLike - temp) > 2) {
                    tempText += " (체감 ${feelsLike.toInt()}°C)"
                }
            }
            weatherItems.add(tempText)
        }

        // 습도
        weatherInfo.getCurrentHumidityValue()?.let { humidity ->
            val comfortLevel = getHumidityComfortLevel(humidity)
            val comfortEmoji = when (comfortLevel) {
                "건조" -> "🔥"
                "습함" -> "💦"
                else -> "✅"
            }
            weatherItems.add("💧 *습도:* ${humidity}% ${comfortEmoji} _${comfortLevel}_")
        }

        // 바람
        weatherInfo.getCurrentWindSpeedValue()?.let { windSpeed ->
            var windText = "💨 *바람:* ${windSpeed}m/s"
            weatherInfo.getWindDirectionDescription()?.let { direction ->
                windText += " ${direction}"
            }
            weatherInfo.getWindStrengthDescription()?.let { strength ->
                windText += " _${strength}_"
            }
            weatherItems.add(windText)
        }

        // 현재 강수
        if (weatherInfo.hasCurrentPrecipitation()) {
            weatherInfo.currentPrecipitation?.let { rain ->
                var rainText = "🌧️ *현재 강수:* ${rain}mm/h"
                weatherInfo.currentPrecipitationType?.let { type ->
                    val precipType = getPrecipitationTypeText(type)
                    rainText += " _${precipType}_"
                }
                weatherItems.add(rainText)
            }
        }

        weatherItems.forEach { item ->
            sb.append("${item}\n")
        }
    }

    private fun appendTodayForecast(sb: StringBuilder, weatherInfo: WeatherInfo) {
        sb.append("\n📅 *오늘 하루 예보*\n\n")

        // 최저/최고 온도
        appendTemperatureRange(sb, weatherInfo)

        // 하늘 상태 및 날씨
        appendWeatherCondition(sb, weatherInfo)

        // 강수확률
        appendPrecipitationProbability(sb, weatherInfo)
    }

    private fun appendTemperatureRange(sb: StringBuilder, weatherInfo: WeatherInfo) {
        if (weatherInfo.temperatureMin != null || weatherInfo.temperatureMax != null) {
            sb.append("🌡️ *일교차:* ")
            weatherInfo.temperatureMin?.let { min ->
                val minEmoji = if (min < 5) "🥶" else if (min < 15) "😰" else "😊"
                sb.append("${minEmoji} 최저 *${min.toInt()}°C*")
            }
            if (weatherInfo.temperatureMin != null && weatherInfo.temperatureMax != null) {
                sb.append(" ↔️ ")
            }
            weatherInfo.temperatureMax?.let { max ->
                val maxEmoji = if (max > 30) "🔥" else if (max > 25) "😎" else "😊"
                sb.append("${maxEmoji} 최고 *${max.toInt()}°C*")
            }
            sb.append("\n")
        }
    }

    private fun appendWeatherCondition(sb: StringBuilder, weatherInfo: WeatherInfo) {
        val skyDescription = weatherInfo.getSkyDescription()
        val precipDescription = weatherInfo.getPrecipitationTypeDescription()

        val weatherText = when {
            precipDescription != null && precipDescription != "강수없음" -> {
                val emoji = getPrecipitationEmoji(weatherInfo.precipitationType)
                val skyInfo = skyDescription?.let { " _${it}_" } ?: ""
                "${emoji} *${precipDescription}*${skyInfo}"
            }

            skyDescription != null -> {
                val emoji = getSkyEmoji(weatherInfo.skyCondition)
                "${emoji} *${skyDescription}*"
            }

            weatherInfo.weatherCondition != null -> {
                val emoji = getWeatherEmoji(weatherInfo.weatherCondition!!)
                val description = getWeatherDescription(weatherInfo.weatherCondition!!)
                "${emoji} *${description}*"
            }

            else -> null
        }

        weatherText?.let {
            sb.append("🌤️ *하늘상태:* ${it}\n")
        }
    }

    private fun appendPrecipitationProbability(sb: StringBuilder, weatherInfo: WeatherInfo) {
        weatherInfo.precipitationProbability?.let { probability ->
            val probEmoji = when {
                probability >= 70 -> "☔"
                probability >= 40 -> "🌦️"
                probability >= 20 -> "⛅"
                else -> "☀️"
            }
            val probText = when {
                probability >= 70 -> "높음"
                probability >= 40 -> "보통"
                probability >= 20 -> "낮음"
                else -> "매우낮음"
            }
            sb.append("${probEmoji} *강수확률:* ${probability}% _${probText}_")
            weatherInfo.precipitation?.let { precipitation ->
                if (precipitation > 0) {
                    sb.append(" 💧 *예상강수량:* ${precipitation}mm")
                }
            }
            sb.append("\n")
        }
    }

    private fun appendDetailedInfo(sb: StringBuilder, weatherInfo: WeatherInfo) {
        val detailItems = buildDetailItems(weatherInfo)

        if (detailItems.isNotEmpty()) {
            sb.append("\n🔍 *상세 정보*\n")
            detailItems.forEach { item ->
                sb.append("${item}\n")
            }
        }
    }

    private fun buildDetailItems(weatherInfo: WeatherInfo): List<String> {
        val items = mutableListOf<String>()

        weatherInfo.visibilityKm?.let { visibility ->
            val visibilityLevel = when {
                visibility >= 10 -> "매우좋음 ✅"
                visibility >= 5 -> "좋음 😊"
                visibility >= 1 -> "보통 😐"
                else -> "나쁨 ⚠️"
            }
            items.add("👁️ *가시거리:* ${visibility}km _${visibilityLevel}_")
        }

        weatherInfo.uvIndex?.let { uv ->
            val uvLevel = getUvLevel(uv)
            val uvEmoji = when (uvLevel) {
                "위험" -> "🚨"
                "매우높음" -> "🔴"
                "높음" -> "🟠"
                "보통" -> "🟡"
                else -> "🟢"
            }
            items.add("☀️ *자외선지수:* ${uv} ${uvEmoji} _${uvLevel}_")
        }

        weatherInfo.airPressure?.let { pressure ->
            val pressureStatus = when {
                pressure >= 1020 -> "높음 📈"
                pressure >= 1000 -> "정상 ✅"
                else -> "낮음 📉"
            }
            items.add("📊 *기압:* ${pressure}hPa _${pressureStatus}_")
        }

        weatherInfo.lightning?.let { lightning ->
            if (lightning > 0) {
                items.add("⚡ *낙뢰위험:* ${lightning}kA/㎢ 🚨 _주의필요_")
            }
        }

        return items
    }

    private fun appendRecommendations(sb: StringBuilder, weatherInfo: WeatherInfo) {
        val recommendations = weatherInfo.getAllRecommendations()

        if (recommendations.isNotEmpty()) {
            sb.append("\n💡 *오늘의 권고사항*\n\n")
            recommendations.forEachIndexed { index, recommendation ->
                sb.append("${index + 1}. ${recommendation}\n")
            }
        }
    }

    private fun appendSeparator(sb: StringBuilder) {
        sb.append("\n")
    }

    private fun appendDailyQuote(sb: StringBuilder) {
        val quote = getRandomQuote()
        sb.append("\n\n✨ *오늘의 명언*\n\n")
        sb.append("_${quote}_\n\n")
        sb.append("\n좋은 하루 되세요!")
    }

    private fun getRandomQuote(): String {
        return try {
            val resource = ClassPathResource("quotes/daily-quotes.txt")
            if (resource.exists()) {
                val quotes =
                    resource.inputStream.bufferedReader().readLines().filter { it.isNotBlank() }
                if (quotes.isNotEmpty()) {
                    quotes[Random.nextInt(quotes.size)]
                } else {
                    "오늘도 행복한 하루 되세요!"
                }
            } else {
                "매일이 새로운 시작입니다. 최선을 다하세요!"
            }
        } catch (e: Exception) {
            "긍정적인 마음으로 하루를 시작하세요!"
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
