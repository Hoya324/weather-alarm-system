package com.weather.alarm.infrastructure.weather.type

enum class ApiType {
    CURRENT_WEATHER,  // 초단기실황 - 현재 날씨
    FORECAST,         // 단기예보 - 3일 예보
    SHORT_FORECAST    // 초단기예보 - 6시간 예보
}
