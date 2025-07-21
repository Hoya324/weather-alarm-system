package com.weather.alarm.infrastructure.weather.exception

class KmaWeatherApiException(
    message: String, cause: Throwable? = null
) : RuntimeException(message, cause)
