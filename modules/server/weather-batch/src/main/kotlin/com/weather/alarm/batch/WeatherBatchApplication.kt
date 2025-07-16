package com.weather.alarm.batch

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.weather.alarm"])
@EnableBatchProcessing
@EnableScheduling
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = ["com.weather.alarm"])
@EntityScan(basePackages = ["com.weather.alarm"])
class WeatherBatchApplication

fun main(args: Array<String>) {
    SpringApplication.run(WeatherBatchApplication::class.java, *args)
}
