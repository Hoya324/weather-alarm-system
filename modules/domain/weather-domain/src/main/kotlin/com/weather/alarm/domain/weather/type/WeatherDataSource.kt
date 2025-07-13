package com.weather.alarm.domain.weather.type

enum class WeatherDataSource {
    KMA_API,        // 기상청 API
    OPEN_WEATHER,   // OpenWeather API
    WEATHER_API,    // WeatherAPI
    MANUAL          // 수동 입력
}
