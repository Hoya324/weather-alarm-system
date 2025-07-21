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
     * API 안정성을 위해 15분 후에 호출
     */
    @Scheduled(cron = "0 15 2,5,8,11,14,17,20,23 * * *", zone = "Asia/Seoul")
    fun executeWeatherDataFetchJob() {
        try {
            logger.info("=== 기상 데이터 수집 배치 시작 ===")

            val jobParameters = JobParametersBuilder()
                .addString("timestamp", LocalDateTime.now().toString())
                .addString("jobType", "weatherDataFetch")
                .addString("baseDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                .addString("baseTime", getCurrentBaseTime())
                .toJobParameters()

            val jobExecution = jobLauncher.run(weatherDataFetchJob, jobParameters)

            logger.info("=== 기상 데이터 수집 배치 완료: ${jobExecution.status} ===")
        } catch (e: Exception) {
            logger.error("기상 데이터 수집 배치 실행 중 오류", e)
        }
    }

    /**
     * 알림 발송 배치
     * 매시간 정각에 실행하여 설정된 시간에 알림 발송
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
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
                logger.info("=== 알림 발송 배치 완료: ${jobExecution.status}, 처리된 알림: ${jobExecution.stepExecutions.sumOf { it.readCount }} ===")
            } else {
                logger.debug("=== 알림 발송 배치 완료: 해당 시간에 발송할 알림 없음 ===")
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
                .addString("baseDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                .addString("baseTime", getCurrentBaseTime())
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
     * 현재 시점에서 사용할 수 있는 기상청 API baseTime 계산
     */
    private fun getCurrentBaseTime(): String {
        val now = LocalDateTime.now()
        val currentHour = now.hour
        val currentMinute = now.minute

        // 기상청 API 발표 시간: 02, 05, 08, 11, 14, 17, 20, 23시
        // 각 시간 15분 후부터 데이터 사용 가능
        val baseHours = listOf(23, 20, 17, 14, 11, 8, 5, 2)
        
        return when {
            // 현재가 02:15 이후면 02시 데이터 사용
            currentHour >= 2 && (currentHour > 2 || currentMinute >= 15) -> {
                baseHours.first { it <= currentHour }.toString().padStart(2, '0') + "00"
            }
            // 그 외는 전날 23시 데이터 사용
            else -> "2300"
        }
    }
}
