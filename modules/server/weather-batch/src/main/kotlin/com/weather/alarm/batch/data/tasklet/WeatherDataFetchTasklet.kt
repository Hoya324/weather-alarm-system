package com.weather.alarm.batch.data.tasklet

import com.weather.alarm.domain.notification.repository.NotificationInfoRepository
import com.weather.alarm.domain.weather.entity.WeatherInfo
import com.weather.alarm.domain.weather.repository.WeatherInfoRepository
import com.weather.alarm.domain.weather.type.WeatherCondition
import com.weather.alarm.domain.weather.type.WeatherDataSource
import com.weather.alarm.infrastructure.weather.client.KmaWeatherClient
import com.weather.alarm.infrastructure.weather.dto.KmaWeatherResponse
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Component
@StepScope
class WeatherDataFetchTasklet(
    private val notificationInfoRepository: NotificationInfoRepository,
    private val weatherInfoRepository: WeatherInfoRepository,
    private val kmaWeatherClient: KmaWeatherClient,
    @Value("#{jobParameters['timestamp']}") private val timestamp: String?
) : Tasklet {

    private val logger = LoggerFactory.getLogger(WeatherDataFetchTasklet::class.java)

    @Transactional
    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        logger.info("=== Weather Data Fetch Tasklet 시작 ===")
        logger.info("Job timestamp: $timestamp")

        try {
            // 활성화된 알림 정보들의 고유한 좌표 수집
            val coordinates = getUniqueCoordinates()
            logger.info("처리할 좌표 개수: ${coordinates.size}")

            if (coordinates.isEmpty()) {
                logger.info("처리할 좌표가 없습니다.")
                return RepeatStatus.FINISHED
            }

            val today = LocalDate.now()
            val todayStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            val baseTime = getCurrentAvailableBaseTime()

            logger.info("API 호출 파라미터 - baseDate: $todayStr, baseTime: $baseTime")

            var successCount = 0
            var skipCount = 0
            var errorCount = 0

            coordinates.forEach { coordinate ->
                try {
                    val result = processCoordinate(coordinate, today, todayStr, baseTime)
                    when (result) {
                        ProcessResult.SUCCESS -> successCount++
                        ProcessResult.SKIPPED -> skipCount++
                        ProcessResult.ERROR -> errorCount++
                    }
                } catch (e: Exception) {
                    logger.error("좌표 ${coordinate.nx}_${coordinate.ny} 처리 중 예외 발생", e)
                    errorCount++
                }
            }

            logger.info("=== Weather Data Fetch Tasklet 완료 ===")
            logger.info("처리 결과 - 성공: $successCount, 스킵: $skipCount, 오류: $errorCount")
            
            return RepeatStatus.FINISHED
        } catch (e: Exception) {
            logger.error("Weather Data Fetch Tasklet 실행 중 치명적 오류", e)
            throw e
        }
    }

    private fun getUniqueCoordinates(): List<CoordinateInfo> {
        return try {
            notificationInfoRepository.findByNotificationEnabledTrue()
                .mapNotNull { notification ->
                    notification.coordinate?.let { coord ->
                        CoordinateInfo(
                            notification.user.id,
                            coord.latitude,
                            coord.longitude,
                            coord.nx,
                            coord.ny
                        )
                    }
                }
                .distinctBy { "${it.nx}_${it.ny}" }
        } catch (e: Exception) {
            logger.error("좌표 정보 조회 중 오류", e)
            emptyList()
        }
    }

    private fun processCoordinate(
        coordinate: CoordinateInfo,
        targetDate: LocalDate,
        todayStr: String,
        baseTime: String
    ): ProcessResult {
        logger.debug("좌표 처리 중: nx=${coordinate.nx}, ny=${coordinate.ny}")

        return runBlocking {
            try {
                // 기존 데이터 확인
                val existingWeather = weatherInfoRepository.findByWeatherDateAndGrid(
                    targetDate,
                    coordinate.nx,
                    coordinate.ny
                )

                if (existingWeather != null) {
                    logger.debug("이미 존재하는 날씨 데이터: ${coordinate.nx}_${coordinate.ny}")
                    return@runBlocking ProcessResult.SKIPPED
                }

                // KMA API 호출
                logger.debug("KMA API 호출: nx=${coordinate.nx}, ny=${coordinate.ny}, baseDate=$todayStr, baseTime=$baseTime")
                
                val weatherResponse = kmaWeatherClient.getVilageFcst(
                    nx = coordinate.nx,
                    ny = coordinate.ny,
                    baseDate = todayStr,
                    baseTime = baseTime
                )

                logger.debug("KMA API 응답 수신: resultCode=${weatherResponse.response.header.resultCode}")

                if (weatherResponse.response.header.resultCode != "00") {
                    logger.warn("KMA API 오류 응답: ${weatherResponse.response.header.resultMsg}")
                    return@runBlocking ProcessResult.ERROR
                }

                val items = weatherResponse.response.body?.items?.item
                if (items.isNullOrEmpty()) {
                    logger.warn("KMA API에서 데이터를 받지 못함: ${coordinate.nx}_${coordinate.ny}")
                    return@runBlocking ProcessResult.ERROR
                }

                logger.debug("받은 데이터 개수: ${items.size}")

                val success = parseAndSaveWeatherData(weatherResponse, coordinate, targetDate)
                if (success) {
                    logger.debug("날씨 데이터 저장 성공: ${coordinate.nx}_${coordinate.ny}")
                    ProcessResult.SUCCESS
                } else {
                    logger.warn("날씨 데이터 저장 실패: ${coordinate.nx}_${coordinate.ny}")
                    ProcessResult.ERROR
                }
            } catch (e: Exception) {
                logger.error("좌표 ${coordinate.nx}_${coordinate.ny} 처리 중 오류", e)
                ProcessResult.ERROR
            }
        }
    }

    private fun parseAndSaveWeatherData(
        response: KmaWeatherResponse,
        coordinate: CoordinateInfo,
        targetDate: LocalDate
    ): Boolean {
        return try {
            val items = response.response.body?.items?.item ?: return false
            val targetDateStr = targetDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))

            // 해당 날짜의 데이터만 필터링
            val todayItems = items.filter { it.fcstDate == targetDateStr }

            if (todayItems.isEmpty()) {
                logger.warn("해당 날짜의 날씨 데이터가 없음: ${coordinate.nx}_${coordinate.ny}, targetDate: $targetDateStr")
                return false
            }

            logger.debug("오늘 날짜 데이터 개수: ${todayItems.size}")

            // 데이터 파싱
            val weatherData = mutableMapOf<String, String?>()
            todayItems.forEach { item ->
                weatherData[item.category] = item.fcstValue
            }

            logger.debug("파싱된 데이터 카테고리: ${weatherData.keys}")

            val weatherInfo = WeatherInfo(
                _userId = coordinate.userId,
                _weatherDate = targetDate,
                _latitude = coordinate.latitude,
                _longitude = coordinate.longitude,
                _nx = coordinate.nx,
                _ny = coordinate.ny,
                _temperature = weatherData["TMP"]?.toDoubleOrNull(),
                _temperatureMin = weatherData["TMN"]?.toDoubleOrNull(),
                _temperatureMax = weatherData["TMX"]?.toDoubleOrNull(),
                _humidity = weatherData["REH"]?.toIntOrNull(),
                _weatherCondition = parseWeatherCondition(weatherData["SKY"], weatherData["PTY"]),
                _precipitation = weatherData["PCP"]?.let { parsePrecipitation(it) },
                _precipitationProbability = weatherData["POP"]?.toIntOrNull(),
                _windSpeed = weatherData["WSD"]?.toDoubleOrNull(),
                _dataSource = WeatherDataSource.KMA_API
            )

            weatherInfoRepository.save(weatherInfo)
            logger.debug("날씨 데이터 저장 완료: ${coordinate.nx}_${coordinate.ny}")
            true
        } catch (e: Exception) {
            logger.error("날씨 데이터 파싱 및 저장 중 오류", e)
            false
        }
    }

    private fun parseWeatherCondition(sky: String?, pty: String?): WeatherCondition? {
        val skyCode = sky?.toIntOrNull()
        val ptyCode = pty?.toIntOrNull()

        return when {
            ptyCode == 1 -> WeatherCondition.LIGHT_RAIN
            ptyCode == 2 -> WeatherCondition.SLEET
            ptyCode == 3 -> WeatherCondition.SNOW
            ptyCode == 4 -> WeatherCondition.HEAVY_RAIN
            skyCode == 1 -> WeatherCondition.CLEAR
            skyCode == 3 -> WeatherCondition.PARTLY_CLOUDY
            skyCode == 4 -> WeatherCondition.CLOUDY
            else -> null
        }
    }

    private fun parsePrecipitation(pcp: String): Double? {
        return when {
            pcp == "강수없음" || pcp == "0.0" -> 0.0
            pcp.contains("mm 미만") -> 0.5
            pcp.contains("mm 이상") -> pcp.replace("mm 이상", "").trim().toDoubleOrNull()
            pcp.endsWith("mm") -> pcp.replace("mm", "").trim().toDoubleOrNull()
            else -> pcp.toDoubleOrNull()
        }
    }

    /**
     * 현재 시점에서 사용할 수 있는 기상청 API baseTime 계산
     */
    private fun getCurrentAvailableBaseTime(): String {
        val now = LocalDateTime.now()
        val currentHour = now.hour
        val currentMinute = now.minute

        // 기상청 API 발표 시간: 02, 05, 08, 11, 14, 17, 20, 23시 (15분 후부터 데이터 사용 가능)
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

    private data class CoordinateInfo(
        val userId: Long,
        val latitude: Double,
        val longitude: Double,
        val nx: Int,
        val ny: Int
    )

    private enum class ProcessResult {
        SUCCESS, SKIPPED, ERROR
    }
}