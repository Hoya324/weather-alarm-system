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
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * TODO: tasklet 기반 날씨 수집 및 저장 개선
 *  - 트랜잭션 범위가 너무 길고, 외부 API와 같은 트랜잭션에서 작동됨
 */

@Component
class WeatherDataFetchTasklet(
    private val notificationInfoRepository: NotificationInfoRepository,
    private val weatherInfoRepository: WeatherInfoRepository,
    private val kmaWeatherClient: KmaWeatherClient
) : Tasklet {

    private val logger = LoggerFactory.getLogger(WeatherDataFetchTasklet::class.java)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        logger.info("=== Weather Data Fetch Tasklet 시작 ===")

        try {
            // 활성화된 알림 정보들의 고유한 좌표 수집
            val coordinates = notificationInfoRepository.findByNotificationEnabledTrue()
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

            logger.info("처리할 좌표 개수: ${coordinates.size}")

            val today = LocalDate.now()
            val todayStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))

            coordinates.forEach { coordinate ->
                try {
                    logger.info("좌표 처리 중: nx=${coordinate.nx}, ny=${coordinate.ny}")

                    runBlocking {
                        // 기존 데이터가 있는지 확인 (격자 좌표 기준으로)
                        val existingWeather = weatherInfoRepository.findByWeatherDateAndGrid(
                            today,
                            coordinate.nx,
                            coordinate.ny
                        )

                        if (existingWeather != null) {
                            logger.info("이미 존재하는 날씨 데이터: ${coordinate.nx}_${coordinate.ny}")
                            return@runBlocking
                        }

                        // 단기예보 데이터 가져오기
                        val weatherResponse = kmaWeatherClient.getVilageFcst(
                            nx = coordinate.nx,
                            ny = coordinate.ny,
                            baseDate = todayStr
                        )

                        parseAndSaveWeatherData(weatherResponse, coordinate, today)
                    }
                } catch (e: Exception) {
                    logger.error("좌표 ${coordinate.nx}_${coordinate.ny} 처리 중 오류", e)
                }
            }

            logger.info("=== Weather Data Fetch Tasklet 완료 ===")
            return RepeatStatus.FINISHED
        } catch (e: Exception) {
            logger.error("Weather Data Fetch Tasklet 실행 중 오류", e)
            throw e
        }
    }

    private fun parseAndSaveWeatherData(
        response: KmaWeatherResponse,
        coordinate: CoordinateInfo,
        targetDate: LocalDate
    ) {
        if (response.response.header.resultCode != "00") {
            logger.warn("API 응답 오류: ${response.response.header.resultMsg}")
            return
        }

        val items = response.response.body?.items?.item ?: emptyList()
        val targetDateStr = targetDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))

        // 해당 날짜의 데이터만 필터링
        val todayItems = items.filter { it.fcstDate == targetDateStr }

        if (todayItems.isEmpty()) {
            logger.warn("날씨 데이터가 없음: ${coordinate.nx}_${coordinate.ny}")
            return
        }

        // 데이터 파싱
        val weatherData = mutableMapOf<String, String?>()
        todayItems.forEach { item ->
            weatherData[item.category] = item.fcstValue
        }

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
        logger.info("날씨 데이터 저장 완료: ${coordinate.nx}_${coordinate.ny}")
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
            pcp.contains("mm 이상") -> pcp.replace("mm 이상", "").toDoubleOrNull()
            pcp.endsWith("mm") -> pcp.replace("mm", "").toDoubleOrNull()
            else -> pcp.toDoubleOrNull()
        }
    }

    private data class CoordinateInfo(
        val userId: Long,
        val latitude: Double,
        val longitude: Double,
        val nx: Int,
        val ny: Int
    )
}
