package com.weather.alarm.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(scanBasePackages = ["com.weather.alarm"])
@EntityScan(basePackages = ["com.weather.alarm.domain"])
@EnableJpaRepositories(basePackages = ["com.weather.alarm.domain"])
class WeatherAlarmApiApplication

fun main(args: Array<String>) {
    runApplication<WeatherAlarmApiApplication>(*args)
}
