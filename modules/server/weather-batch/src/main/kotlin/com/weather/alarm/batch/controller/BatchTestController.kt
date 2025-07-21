package com.weather.alarm.batch.controller

import com.weather.alarm.batch.scheduler.WeatherBatchScheduler
import com.weather.alarm.infrastructure.weather.client.KmaWeatherClient
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/batch")
class BatchTestController(
    private val jobLauncher: JobLauncher,
    private val weatherBatchScheduler: WeatherBatchScheduler,
    private val kmaWeatherClient: KmaWeatherClient,
    @Qualifier("weatherDataFetchJob") private val weatherDataFetchJob: Job,
    @Qualifier("notificationSendJob") private val notificationSendJob: Job,
) {

    private val logger = LoggerFactory.getLogger(BatchTestController::class.java)

    @PostMapping("/execute/complete")
    fun executeCompleteProcess(): Map<String, Any> {
        logger.info("수동 전체 프로세스 실행 요청")

        return try {
            val result = weatherBatchScheduler.executeCompleteWeatherProcess()
            mapOf(
                "success" to true,
                "message" to result,
                "timestamp" to LocalDateTime.now().toString()
            )
        } catch (e: Exception) {
            logger.error("수동 프로세스 실행 중 오류", e)
            mapOf(
                "success" to false,
                "message" to "실행 실패: ${e.message}",
                "timestamp" to LocalDateTime.now().toString()
            )
        }
    }

    @PostMapping("/execute/weather-fetch")
    fun executeWeatherFetch(): Map<String, Any> {
        logger.info("수동 기상 데이터 수집 실행 요청")

        return try {
            val jobParameters = JobParametersBuilder()
                .addString("timestamp", LocalDateTime.now().toString())
                .addString("jobType", "manual-weatherDataFetch")
                .toJobParameters()

            val jobExecution = jobLauncher.run(weatherDataFetchJob, jobParameters)

            mapOf(
                "success" to true,
                "message" to "기상 데이터 수집 배치 실행 완료: ${jobExecution.status}",
                "jobStatus" to jobExecution.status.toString(),
                "timestamp" to LocalDateTime.now().toString()
            )
        } catch (e: Exception) {
            logger.error("기상 데이터 수집 실행 중 오류", e)
            mapOf(
                "success" to false,
                "message" to "실행 실패: ${e.message}",
                "timestamp" to LocalDateTime.now().toString()
            )
        }
    }

    @PostMapping("/execute/notification")
    fun executeNotification(): Map<String, Any> {
        logger.info("수동 알림 발송 실행 요청")

        return try {
            val now = LocalDateTime.now()
            val currentTimeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"))

            val jobParameters = JobParametersBuilder()
                .addString("timestamp", now.toString())
                .addString("targetTime", currentTimeStr)
                .addString("jobType", "manual-notificationSend")
                .toJobParameters()

            val jobExecution = jobLauncher.run(notificationSendJob, jobParameters)

            mapOf(
                "success" to true,
                "message" to "알림 발송 배치 실행 완료: ${jobExecution.status}",
                "jobStatus" to jobExecution.status.toString(),
                "targetTime" to currentTimeStr,
                "timestamp" to LocalDateTime.now().toString()
            )
        } catch (e: Exception) {
            logger.error("알림 발송 실행 중 오류", e)
            mapOf(
                "success" to false,
                "message" to "실행 실패: ${e.message}",
                "timestamp" to LocalDateTime.now().toString()
            )
        }
    }

    @GetMapping("/test/kma-api")
    suspend fun testKmaApi(
        @RequestParam(defaultValue = "60") nx: Int,
        @RequestParam(defaultValue = "127") ny: Int,
        @RequestParam(required = false) baseDate: String?,
        @RequestParam(required = false) baseTime: String?
    ): Map<String, Any> {
        logger.info("KMA API 연결 테스트 - nx: $nx, ny: $ny")

        return try {
            val date = baseDate ?: LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            val time = baseTime ?: kmaWeatherClient.getCurrentAvailableBaseTime()

            val response = kmaWeatherClient.getVilageFcst(
                nx = nx,
                ny = ny,
                baseDate = date,
                baseTime = time
            )

            val itemCount = response.response.body?.items?.item?.size ?: 0

            mapOf(
                "success" to true,
                "resultCode" to response.response.header.resultCode,
                "resultMsg" to response.response.header.resultMsg,
                "itemCount" to itemCount,
                "requestParams" to mapOf(
                    "nx" to nx,
                    "ny" to ny,
                    "baseDate" to date,
                    "baseTime" to time
                ),
                "timestamp" to LocalDateTime.now().toString()
            )
        } catch (e: Exception) {
            logger.error("KMA API 테스트 중 오류", e)
            mapOf(
                "success" to false,
                "message" to "API 테스트 실패: ${e.message}",
                "timestamp" to LocalDateTime.now().toString()
            )
        }
    }

    @GetMapping("/test/base-time")
    fun getAvailableBaseTime(): Map<String, Any> {
        return mapOf(
            "currentTime" to LocalDateTime.now().toString(),
            "availableBaseTime" to kmaWeatherClient.getCurrentAvailableBaseTime(),
            "timezone" to "Asia/Seoul"
        )
    }

    @GetMapping("/status")
    fun getBatchStatus(): Map<String, Any> {
        return mapOf(
            "applicationName" to "weather-alarm-batch",
            "currentTime" to LocalDateTime.now().toString(),
            "timezone" to "Asia/Seoul",
            "availableEndpoints" to listOf(
                "POST /api/batch/execute/complete - 전체 프로세스 실행",
                "POST /api/batch/execute/weather-fetch - 기상 데이터 수집",
                "POST /api/batch/execute/notification - 알림 발송",
                "GET /api/batch/test/kma-api - KMA API 테스트",
                "GET /api/batch/test/base-time - 사용 가능한 baseTime 확인"
            )
        )
    }
}