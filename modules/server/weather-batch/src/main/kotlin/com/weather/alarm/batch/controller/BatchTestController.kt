package com.weather.alarm.batch.controller

import com.weather.alarm.batch.scheduler.WeatherBatchScheduler
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/batch")
class BatchTestController(
    private val jobLauncher: JobLauncher,
    private val weatherBatchScheduler: WeatherBatchScheduler,
    @Qualifier("weatherDataFetchJob") private val weatherDataFetchJob: Job,
    @Qualifier("notificationSendJob") private val notificationSendJob: Job,
    @Qualifier("weatherProcessCompleteJob") private val weatherProcessCompleteJob: Job
) {

    @PostMapping("/weather-fetch")
    fun triggerWeatherDataFetch(): String {
        return try {
            val jobParameters = JobParametersBuilder()
                .addString("timestamp", LocalDateTime.now().toString())
                .addString("jobType", "manual-weatherDataFetch")
                .toJobParameters()

            val jobExecution = jobLauncher.run(weatherDataFetchJob, jobParameters)
            "기상 데이터 수집 배치 실행 완료: ${jobExecution.status}"
        } catch (e: Exception) {
            "기상 데이터 수집 배치 실행 실패: ${e.message}"
        }
    }

    @PostMapping("/notification-send")
    fun triggerNotificationSend(): String {
        return try {
            val jobParameters = JobParametersBuilder()
                .addString("timestamp", LocalDateTime.now().toString())
                .addString("jobType", "manual-notificationSend")
                .toJobParameters()

            val jobExecution = jobLauncher.run(notificationSendJob, jobParameters)
            "알림 발송 배치 실행 완료: ${jobExecution.status}"
        } catch (e: Exception) {
            "알림 발송 배치 실행 실패: ${e.message}"
        }
    }

    @PostMapping("/complete-process")
    fun triggerCompleteProcess(): String {
        return weatherBatchScheduler.executeCompleteWeatherProcess()
    }
}
