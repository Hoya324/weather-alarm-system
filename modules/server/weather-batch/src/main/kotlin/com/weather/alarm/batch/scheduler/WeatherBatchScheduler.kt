package com.weather.alarm.batch.scheduler

import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class WeatherBatchScheduler(
    private val jobLauncher: JobLauncher,
    @Qualifier("weatherDataFetchJob") private val weatherDataFetchJob: Job,
    @Qualifier("notificationSendJob") private val notificationSendJob: Job,
    @Qualifier("weatherProcessCompleteJob") private val weatherProcessCompleteJob: Job
) {

    private val logger = LoggerFactory.getLogger(WeatherBatchScheduler::class.java)

    /**
     * 기상 데이터 수집 배치
     * 기상청 API 제공 시간: 02:10, 05:10, 08:10, 11:10, 14:10, 17:10, 20:10, 23:10
     */
    @Scheduled(cron = "0 10 2,5,8,11,14,17,20,23 * * *", zone = "Asia/Seoul")
    fun executeWeatherDataFetchJob() {
        try {
            logger.info("=== 기상 데이터 수집 배치 시작 ===")

            val jobParameters = JobParametersBuilder()
                .addString("timestamp", LocalDateTime.now().toString())
                .addString("jobType", "weatherDataFetch")
                .toJobParameters()

            val jobExecution = jobLauncher.run(weatherDataFetchJob, jobParameters)

            logger.info("=== 기상 데이터 수집 배치 완료: ${jobExecution.status} ===")
        } catch (e: Exception) {
            logger.error("기상 데이터 수집 배치 실행 중 오류", e)
        }
    }

    /**
     * 알림 발송 배치
     * 매분마다 실행하여 설정된 시간에 알림 발송
     */
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    fun executeNotificationSendJob() {
        try {
            val now = LocalDateTime.now()
            val currentTimeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"))

            logger.debug("=== 알림 발송 배치 체크: ${now.format(DateTimeFormatter.ofPattern("HH:mm"))} ===")

            val jobParameters = JobParametersBuilder()
                .addString("timestamp", now.toString())
                .addString("targetTime", currentTimeStr)
                .addString("jobType", "notificationSend")
                .toJobParameters()

            val jobExecution = jobLauncher.run(notificationSendJob, jobParameters)

            if (jobExecution.stepExecutions.any { it.readCount > 0 }) {
                logger.info("=== 알림 발송 배치 완료: ${jobExecution.status} ===")
            }
        } catch (e: Exception) {
            logger.error("알림 발송 배치 실행 중 오류", e)
        }
    }

    /**
     * 전체 프로세스 실행 (수동 호출용)
     * 기상 데이터 수집 후 알림 발송까지 한 번에 실행
     */
    fun executeCompleteWeatherProcess(): String {
        return try {
            logger.info("=== 전체 날씨 프로세스 시작 ===")

            val jobParameters = JobParametersBuilder()
                .addString("timestamp", LocalDateTime.now().toString())
                .addString("jobType", "completeProcess")
                .toJobParameters()

            val jobExecution = jobLauncher.run(weatherProcessCompleteJob, jobParameters)

            logger.info("=== 전체 날씨 프로세스 완료: ${jobExecution.status} ===")
            "배치 실행 완료: ${jobExecution.status}"
        } catch (e: Exception) {
            logger.error("전체 날씨 프로세스 실행 중 오류", e)
            "배치 실행 실패: ${e.message}"
        }
    }
}
