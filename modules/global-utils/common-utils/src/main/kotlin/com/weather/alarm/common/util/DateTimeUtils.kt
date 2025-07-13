package com.weather.alarm.common.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DateTimeUtils {

    val WEATHER_API_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    val WEATHER_API_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmm")
    val WEATHER_API_DATETIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")

    /**
     * 현재 시간을 기상청 API 날짜 형식으로 변환
     */
    fun getCurrentDateForWeatherApi(): String {
        return LocalDateTime.now().format(WEATHER_API_DATE_FORMAT)
    }

    /**
     * 현재 시간을 기상청 API 시간 형식으로 변환 (정시 기준)
     */
    fun getCurrentTimeForWeatherApi(): String {
        val now = LocalDateTime.now()
        // 기상청 API는 정시 기준으로 10분 후에 데이터가 제공됨
        val adjustedTime = if (now.minute < 10) {
            now.minusHours(1)
        } else {
            now
        }

        return adjustedTime.withMinute(0).withSecond(0).format(WEATHER_API_TIME_FORMAT)
    }

    /**
     * 현재 시간을 기상청 API 일시 형식으로 변환
     */
    fun getCurrentDateTimeForWeatherApi(): String {
        return LocalDateTime.now().format(WEATHER_API_DATETIME_FORMAT)
    }

    /**
     * 단기예보용 발표시각 계산
     * 02, 05, 08, 11, 14, 17, 20, 23시에 발표
     */
    fun getVilageFcstBaseTime(): String {
        val now = LocalDateTime.now()
        val hour = now.hour

        val baseHour = when {
            hour < 2 || (hour == 2 && now.minute < 10) -> 23
            hour < 5 || (hour == 5 && now.minute < 10) -> 2
            hour < 8 || (hour == 8 && now.minute < 10) -> 5
            hour < 11 || (hour == 11 && now.minute < 10) -> 8
            hour < 14 || (hour == 14 && now.minute < 10) -> 11
            hour < 17 || (hour == 17 && now.minute < 10) -> 14
            hour < 20 || (hour == 20 && now.minute < 10) -> 17
            hour < 23 || (hour == 23 && now.minute < 10) -> 20
            else -> 23
        }

        return String.format("%02d00", baseHour)
    }

    /**
     * 초단기예보용 발표시각 계산
     * 매시 30분에 발표, 45분 후 호출 가능
     */
    fun getUltraSrtFcstBaseTime(): String {
        val now = LocalDateTime.now()
        val adjustedTime = if (now.minute < 45) {
            now.minusHours(1)
        } else {
            now
        }

        return adjustedTime.withMinute(30).withSecond(0).format(WEATHER_API_TIME_FORMAT)
    }
}
