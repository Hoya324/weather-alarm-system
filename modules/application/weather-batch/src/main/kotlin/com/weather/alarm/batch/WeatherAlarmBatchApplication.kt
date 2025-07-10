package com.weather.alarm.batch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(
    scanBasePackages = [
        "com.weather.alarm.common",
        "com.weather.alarm.domain",
        "com.weather.alarm.infrastructure",
        "com.weather.alarm.batch"
    ]
)
@EnableScheduling
class WeatherAlarmBatchApplication

fun main(args: Array<String>) {
    runApplication<WeatherAlarmBatchApplication>(*args)
}
