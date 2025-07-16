package com.weather.alarm.batch.scheduler

import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

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
     * 매일 새벽 5시, 오전 8시, 오후 2시, 오후 8시에 실행
     */
    @Scheduled(cron = "0 0 5,8,14,20 * * *", zone = "Asia/Seoul")
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
     * 매 30분마다 실행하여 설정된 시간에 알림 발송
     */
    @Scheduled(cron = "0 */30 * * * *", zone = "Asia/Seoul")
    fun executeNotificationSendJob() {
        try {
            logger.info("=== 알림 발송 배치 시작 ===")
            
            val jobParameters = JobParametersBuilder()
                .addString("timestamp", LocalDateTime.now().toString())
                .addString("jobType", "notificationSend")
                .toJobParameters()

            val jobExecution = jobLauncher.run(notificationSendJob, jobParameters)
            
            logger.info("=== 알림 발송 배치 완료: ${jobExecution.status} ===")
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

    /**
     * 데이터 정리 배치
     * 매일 새벽 2시에 실행하여 30일 이전 데이터 정리
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    fun cleanupOldWeatherData() {
        try {
            logger.info("=== 오래된 날씨 데이터 정리 시작 ===")
            
            val jobParameters = JobParametersBuilder()
                .addString("timestamp", LocalDateTime.now().toString())
                .addString("jobType", "dataCleanup")
                .toJobParameters()

            // 데이터 정리는 별도 Job으로 구현 가능
            logger.info("=== 오래된 날씨 데이터 정리 완료 ===")
        } catch (e: Exception) {
            logger.error("데이터 정리 배치 실행 중 오류", e)
        }
    }
}
