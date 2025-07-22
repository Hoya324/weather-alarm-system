package com.weather.alarm.batch.core.tasklet

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
import java.time.format.DateTimeFormatter

@Component
@StepScope
class WeatherDataFetchTasklet(
    private val notificationInfoRepository: NotificationInfoRepository,
    private val weatherInfoRepository: WeatherInfoRepository,
    private val kmaWeatherClient: KmaWeatherClient,
    @Value("#{jobParameters['timestamp']}") private val timestamp: String?,
    @Value("#{jobParameters['fetchType'] ?: 'FORECAST'}") private val fetchType: String
) : Tasklet {

    private val logger = LoggerFactory.getLogger(WeatherDataFetchTasklet::class.java)

    @Transactional
    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        logger.info("=== Weather Data Fetch Tasklet 시작 ===")
        logger.info("Job timestamp: $timestamp, fetchType: $fetchType")

        try {
            val coordinates = getUniqueCoordinates()
            logger.info("처리할 좌표 개수: ${coordinates.size}")

            if (coordinates.isEmpty()) {
                logger.info("처리할 좌표가 없습니다.")
                return RepeatStatus.FINISHED
            }

            val mode = try {
                FetchMode.valueOf(fetchType.uppercase())
            } catch (e: Exception) {
                logger.warn("잘못된 fetchType: $fetchType, 기본값 FORECAST 사용")
                FetchMode.FORECAST
            }

            val results = when (mode) {
                FetchMode.COMPREHENSIVE -> fetchComprehensiveWeatherData(coordinates)
                FetchMode.CURRENT -> fetchCurrentWeatherOnly(coordinates)
                FetchMode.FORECAST -> fetchForecastOnly(coordinates)
            }

            logResults(results)
            return RepeatStatus.FINISHED

        } catch (e: Exception) {
            logger.error("Weather Data Fetch Tasklet 실행 중 치명적 오류", e)
            throw e
        }
    }

    /**
     * 종합 날씨 데이터 수집 (모든 API 활용)
     */
    private fun fetchComprehensiveWeatherData(coordinates: List<CoordinateInfo>): ProcessResults {
        logger.info("종합 날씨 데이터 수집 시작")
        val today = LocalDate.now()

        var successCount = 0
        var skipCount = 0
        var errorCount = 0

        coordinates.forEach { coordinate ->
            try {
                val result = processCoordinateComprehensive(coordinate, today)
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

        return ProcessResults(successCount, skipCount, errorCount)
    }

    /**
     * 현재 날씨만 수집 (초단기실황)
     */
    private fun fetchCurrentWeatherOnly(coordinates: List<CoordinateInfo>): ProcessResults {
        logger.info("현재 날씨 데이터 수집 시작")
        val today = LocalDate.now()

        var successCount = 0
        var skipCount = 0
        var errorCount = 0

        coordinates.forEach { coordinate ->
            try {
                val result = processCurrentWeatherOnly(coordinate, today)
                when (result) {
                    ProcessResult.SUCCESS -> successCount++
                    ProcessResult.SKIPPED -> skipCount++
                    ProcessResult.ERROR -> errorCount++
                }
            } catch (e: Exception) {
                logger.error("현재 날씨 처리 중 예외 발생: ${coordinate.nx}_${coordinate.ny}", e)
                errorCount++
            }
        }

        return ProcessResults(successCount, skipCount, errorCount)
    }

    /**
     * 예보 데이터만 수집 (단기예보)
     */
    private fun fetchForecastOnly(coordinates: List<CoordinateInfo>): ProcessResults {
        logger.info("예보 데이터 수집 시작")
        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val baseTime = getCurrentAvailableBaseTime()

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
                logger.error("예보 데이터 처리 중 예외 발생: ${coordinate.nx}_${coordinate.ny}", e)
                errorCount++
            }
        }

        return ProcessResults(successCount, skipCount, errorCount)
    }

    /**
     * 종합 데이터 처리 (간소화 버전)
     */
    private fun processCoordinateComprehensive(
        coordinate: CoordinateInfo,
        targetDate: LocalDate
    ): ProcessResult = runBlocking {

        try {
            // 1. 기존 데이터 확인
            val existingWeather = weatherInfoRepository.findByWeatherDateAndGrid(
                targetDate, coordinate.nx, coordinate.ny
            )

            if (existingWeather != null) {
                logger.debug("이미 존재하는 날씨 데이터: ${coordinate.nx}_${coordinate.ny}")
                return@runBlocking ProcessResult.SKIPPED
            }

            // 2. 단기예보 데이터 수집 (기본)
            val todayStr = targetDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            val baseTime = getCurrentAvailableBaseTime()

            val weatherResponse = kmaWeatherClient.getVilageFcst(
                nx = coordinate.nx,
                ny = coordinate.ny,
                baseDate = todayStr,
                baseTime = baseTime
            )

            if (weatherResponse.response.header.resultCode != "00") {
                logger.warn("KMA API 오류 응답: ${weatherResponse.response.header.resultMsg}")
                return@runBlocking ProcessResult.ERROR
            }

            val success = parseAndSaveWeatherData(weatherResponse, coordinate, targetDate)
            if (success) {
                ProcessResult.SUCCESS
            } else {
                ProcessResult.ERROR
            }

        } catch (e: Exception) {
            logger.error("종합 데이터 처리 중 오류: ${coordinate.nx}_${coordinate.ny}", e)
            ProcessResult.ERROR
        }
    }

    /**
     * 현재 날씨만 처리 (간소화 버전)
     */
    private fun processCurrentWeatherOnly(
        coordinate: CoordinateInfo,
        targetDate: LocalDate
    ): ProcessResult = runBlocking {

        try {
            // 초단기실황은 매번 업데이트 가능하므로 기존 데이터 체크 생략
            val todayStr = targetDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            val baseTime = getCurrentAvailableBaseTime()

            val weatherResponse = kmaWeatherClient.getUltraSrtNcst(
                nx = coordinate.nx,
                ny = coordinate.ny,
                baseDate = todayStr,
                baseTime = baseTime
            )

            if (weatherResponse.response.header.resultCode != "00") {
                logger.warn("초단기실황 API 오류: ${weatherResponse.response.header.resultMsg}")
                return@runBlocking ProcessResult.ERROR
            }

            // 기존 데이터가 있으면 업데이트, 없으면 새로 생성
            val existingWeather = weatherInfoRepository.findByWeatherDateAndGrid(
                targetDate, coordinate.nx, coordinate.ny
            ) ?: createNewWeatherInfo(coordinate, targetDate)

            val success = updateCurrentWeatherData(existingWeather, weatherResponse)
            if (success) {
                weatherInfoRepository.save(existingWeather)
                ProcessResult.SUCCESS
            } else {
                ProcessResult.ERROR
            }

        } catch (e: Exception) {
            logger.error("현재 날씨 처리 중 오류: ${coordinate.nx}_${coordinate.ny}", e)
            ProcessResult.ERROR
        }
    }

    /**
     * 활성화된 알림의 고유 좌표 목록 조회
     */
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

    /**
     * 예보 데이터 처리
     */
    private fun processCoordinate(
        coordinate: CoordinateInfo,
        targetDate: LocalDate,
        todayStr: String,
        baseTime: String
    ): ProcessResult = runBlocking {

        try {
            // 기존 데이터 확인
            val existingWeather = weatherInfoRepository.findByWeatherDateAndGrid(
                targetDate, coordinate.nx, coordinate.ny
            )

            if (existingWeather != null) {
                logger.debug("이미 존재하는 날씨 데이터: ${coordinate.nx}_${coordinate.ny}")
                return@runBlocking ProcessResult.SKIPPED
            }

            // KMA API 호출
            val weatherResponse = kmaWeatherClient.getVilageFcst(
                nx = coordinate.nx,
                ny = coordinate.ny,
                baseDate = todayStr,
                baseTime = baseTime
            )

            if (weatherResponse.response.header.resultCode != "00") {
                logger.warn("KMA API 오류 응답: ${weatherResponse.response.header.resultMsg}")
                return@runBlocking ProcessResult.ERROR
            }

            val items = weatherResponse.response.body?.items?.item
            if (items.isNullOrEmpty()) {
                logger.warn("KMA API에서 데이터를 받지 못함: ${coordinate.nx}_${coordinate.ny}")
                return@runBlocking ProcessResult.ERROR
            }

            val success = parseAndSaveWeatherData(weatherResponse, coordinate, targetDate)
            if (success) {
                ProcessResult.SUCCESS
            } else {
                ProcessResult.ERROR
            }

        } catch (e: Exception) {
            logger.error("좌표 ${coordinate.nx}_${coordinate.ny} 처리 중 오류", e)
            ProcessResult.ERROR
        }
    }

    /**
     * 날씨 데이터 파싱 및 저장
     */
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
            logger.debug("날씨 데이터 저장 완료: ${coordinate.nx}_${coordinate.ny}")
            true
        } catch (e: Exception) {
            logger.error("날씨 데이터 파싱 및 저장 중 오류", e)
            false
        }
    }

    /**
     * 현재 날씨 데이터 업데이트
     */
    private fun updateCurrentWeatherData(
        weatherInfo: WeatherInfo,
        response: KmaWeatherResponse
    ): Boolean {
        return try {
            val items = response.response.body?.items?.item ?: return false

            val currentData = mutableMapOf<String, String?>()
            items.forEach { item ->
                currentData[item.category] = item.obsrValue // 실황 데이터는 obsrValue 사용
            }

            // 현재 날씨 정보 업데이트
            currentData["T1H"]?.toDoubleOrNull()?.let { weatherInfo.updateCurrentTemperature(it) }
            currentData["REH"]?.toIntOrNull()?.let { weatherInfo.updateCurrentHumidity(it) }
            currentData["WSD"]?.toDoubleOrNull()?.let { weatherInfo.updateCurrentWindSpeed(it) }

            logger.debug("현재 날씨 데이터 업데이트 완료")
            true
        } catch (e: Exception) {
            logger.error("현재 날씨 데이터 업데이트 중 오류", e)
            false
        }
    }

    /**
     * 날씨 상태 파싱
     */
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

    /**
     * 강수량 파싱
     */
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
            currentHour >= 2 && (currentHour > 2 || currentMinute >= 15) -> {
                baseHours.first { it <= currentHour }.toString().padStart(2, '0') + "00"
            }

            else -> "2300"
        }
    }

    /**
     * 새 날씨 정보 엔티티 생성
     */
    private fun createNewWeatherInfo(coordinate: CoordinateInfo, targetDate: LocalDate): WeatherInfo {
        return WeatherInfo(
            _userId = coordinate.userId,
            _weatherDate = targetDate,
            _latitude = coordinate.latitude,
            _longitude = coordinate.longitude,
            _nx = coordinate.nx,
            _ny = coordinate.ny,
            _dataSource = WeatherDataSource.KMA_API
        )
    }

    /**
     * 처리 결과 로깅
     */
    private fun logResults(results: ProcessResults) {
        logger.info("=== Weather Data Fetch Tasklet 완료 ===")
        logger.info("처리 결과 - 성공: ${results.success}, 스킵: ${results.skip}, 오류: ${results.error}")
    }

    // ===================== 내부 데이터 클래스 =====================

    private data class CoordinateInfo(
        val userId: Long,
        val latitude: Double,
        val longitude: Double,
        val nx: Int,
        val ny: Int
    )

    private data class ProcessResults(
        val success: Int,
        val skip: Int,
        val error: Int
    )

    private enum class ProcessResult {
        SUCCESS, SKIPPED, ERROR
    }

    private enum class FetchMode {
        COMPREHENSIVE,  // 모든 데이터
        CURRENT,        // 현재 날씨만 (CURRENT_ONLY → CURRENT)
        FORECAST        // 예보만 (FORECAST_ONLY → FORECAST)
    }
}