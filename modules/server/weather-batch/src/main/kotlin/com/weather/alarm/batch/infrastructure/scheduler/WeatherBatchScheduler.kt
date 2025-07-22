package com.weather.alarm.batch.infrastructure.scheduler

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
    @Qualifier("realTimeWeatherJob") private val realTimeWeatherJob: Job
) {

    private val logger = LoggerFactory.getLogger(WeatherBatchScheduler::class.java)

    /**
     * 기본 기상 데이터 수집 배치 (단기예보)
     * 기상청 API 제공 시간: 02:10, 05:10, 08:10, 11:10, 14:10, 17:10, 20:10, 23:10
     */
    @Scheduled(cron = COLLECT_WEATHER_DATA_DAILY, zone = "Asia/Seoul")
    fun executeWeatherDataFetchJob() {
        try {
            logger.info("=== 기상 데이터 수집 배치 시작 ===")

            val jobParameters = JobParametersBuilder()
                .addString("timestamp", LocalDateTime.now().toString())
                .addString("fetchType", "FORECAST")
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
     * 실시간 날씨 업데이트 (초단기실황)
     * 활동 시간대에만 실행: 오전 8시~오후 10시, 30분마다
     */
    @Scheduled(cron = COLLECT_WEATHER_IN_ACTIVE_TIME_PER_30M, zone = "Asia/Seoul")
    fun executeRealTimeWeatherUpdate() {
        try {
            logger.debug("=== 실시간 날씨 업데이트 시작 ===")

            val jobParameters = JobParametersBuilder()
                .addString("timestamp", LocalDateTime.now().toString())
                .addString("fetchType", "CURRENT")
                .toJobParameters()

            val jobExecution = jobLauncher.run(realTimeWeatherJob, jobParameters)
            logger.debug("=== 실시간 날씨 업데이트 완료: ${jobExecution.status} ===")
        } catch (e: Exception) {
            logger.error("실시간 날씨 업데이트 실행 중 오류", e)
        }
    }

    /**
     * 일반 알림 발송 배치 (사용자 지정 시간)
     * 매시간 정각에 실행하여 설정된 시간에 알림 발송
     */
    @Scheduled(cron = CHECK_NOTIFICATION_INFO_HOURLY, zone = "Asia/Seoul")
    fun executeNotificationSendJob() {
        try {
            val currentTime = LocalDateTime.now()

            val currentTimeStr = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            logger.debug("=== 일반 알림 발송 배치 체크: ${currentTimeStr} ===")

            val jobParameters = JobParametersBuilder()
                .addString("timestamp", currentTime.toString())
                .addString("targetTime", currentTimeStr)
                .toJobParameters()

            val jobExecution = jobLauncher.run(notificationSendJob, jobParameters)

            val processedCount = jobExecution.stepExecutions.sumOf { it.readCount }
            if (processedCount > 0) {
                logger.info("=== 일반 알림 발송 배치 완료: ${jobExecution.status}, 처리된 알림: ${processedCount} ===")
            } else {
                logger.debug("=== 일반 알림 발송 배치 완료: 해당 시간에 발송할 알림 없음 ===")
            }
        } catch (e: Exception) {
            logger.error("일반 알림 발송 배치 실행 중 오류", e)
        }
    }

    /**
     * 기상 특보/경보 확인 및 긴급 알림
     * 매 시간 30분에 실행하여 위험 날씨 감지시 즉시 알림
     */
    @Scheduled(cron = "0 30 * * * *", zone = "Asia/Seoul")
    fun executeWeatherAlertCheck() {
        try {
            logger.debug("=== 기상 특보/경보 확인 시작 ===")

            val jobParameters = JobParametersBuilder()
                .addString("timestamp", LocalDateTime.now().toString())
                .addString("fetchType", "CURRENT")
                .addString("alertOnly", "true")
                .toJobParameters()

            val jobExecution = jobLauncher.run(realTimeWeatherJob, jobParameters)
            logger.debug("=== 기상 특보/경보 확인 완료: ${jobExecution.status} ===")
        } catch (e: Exception) {
            logger.error("기상 특보/경보 확인 실행 중 오류", e)
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
            // 현재가 02:15 이후면 해당 시간의 데이터 사용
            currentHour >= 2 && (currentHour > 2 || currentMinute >= 15) -> {
                baseHours.first { it <= currentHour }.toString().padStart(2, '0') + "00"
            }
            // 그 외는 전날 23시 데이터 사용
            else -> "2300"
        }
    }
}
