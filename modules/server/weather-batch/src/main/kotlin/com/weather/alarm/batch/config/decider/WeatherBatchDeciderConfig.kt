package com.weather.alarm.batch.config.decider

import org.springframework.batch.core.job.flow.FlowExecutionStatus
import org.springframework.batch.core.job.flow.JobExecutionDecider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WeatherBatchDeciderConfig {

    @Bean
    fun notificationDecider(): JobExecutionDecider {
        return JobExecutionDecider { jobExecution, stepExecution ->
            val notificationEnabled = jobExecution.jobParameters.getString("notificationEnabled") ?: "true"

            if (notificationEnabled.toBoolean()) {
                FlowExecutionStatus("SEND")
            } else {
                FlowExecutionStatus("SKIP")
            }
        }
    }

    @Bean
    fun timeBasedNotificationDecider(): JobExecutionDecider {
        return JobExecutionDecider { jobExecution, stepExecution ->
            val currentHour = java.time.LocalTime.now().hour

            when (currentHour) {
                in 6..11 -> FlowExecutionStatus("MORNING")
                in 12..17 -> FlowExecutionStatus("AFTERNOON")
                in 18..23 -> FlowExecutionStatus("EVENING")
                else -> FlowExecutionStatus("NIGHT")
            }
        }
    }
}