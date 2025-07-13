package com.weather.alarm.domain.notification.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig {

    @Bean(name = ["notificationTaskExecutor"])
    fun notificationTaskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.setCorePoolSize(2)
        executor.setMaxPoolSize(5)
        executor.setQueueCapacity(100)
        executor.setThreadNamePrefix("user-notification-")
        executor.initialize()
        return executor
    }
}
