package com.weather.alarm.infrastructure.weather.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.weather.alarm.infrastructure.weather.dto.KmaWeatherResponse
import com.weather.alarm.infrastructure.weather.exception.KmaWeatherApiException
import com.weather.alarm.infrastructure.weather.type.ApiType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class KmaWeatherClient(
    private val weatherWebClient: WebClient,
    @Value("\${weather.api.kma.service-key}")
    private val serviceKey: String,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(KmaWeatherClient::class.java)

    companion object {
        private const val KMA_BASE_URL = "/1360000/VilageFcstInfoService_2.0"
        private const val DEFAULT_NUM_OF_ROWS = 1000
        private const val ULTRA_SRT_ROWS = 10
        private const val ULTRA_FCST_ROWS = 60
        private const val SUCCESS_CODE = "00"
    }

    /**
     * 단기예보 조회 (3일 예보)
     * 발표시간: 02, 05, 08, 11, 14, 17, 20, 23시 (15분 후부터 사용 가능)
     */
    suspend fun getVilageFcst(
        nx: Int,
        ny: Int,
        baseDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
        baseTime: String = getCurrentAvailableBaseTime(),
        numOfRows: Int = DEFAULT_NUM_OF_ROWS
    ): KmaWeatherResponse {

        logger.debug("KMA 단기예보 API 호출 - nx: $nx, ny: $ny, baseDate: $baseDate, baseTime: $baseTime")

        return callKmaApi(
            path = "/getVilageFcst",
            nx = nx,
            ny = ny,
            baseDate = baseDate,
            baseTime = baseTime,
            numOfRows = numOfRows,
            apiName = "단기예보"
        )
    }

    /**
     * 초단기실황 조회 (현재 날씨)
     * 매시 정시 발표, 10분 후부터 호출 가능
     */
    suspend fun getUltraSrtNcst(
        nx: Int,
        ny: Int,
        baseDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
        baseTime: String = getCurrentHourBaseTime()
    ): KmaWeatherResponse {

        logger.debug("KMA 초단기실황 API 호출 - nx: $nx, ny: $ny, baseDate: $baseDate, baseTime: $baseTime")

        return callKmaApi(
            path = "/getUltraSrtNcst",
            nx = nx,
            ny = ny,
            baseDate = baseDate,
            baseTime = baseTime,
            numOfRows = ULTRA_SRT_ROWS,
            apiName = "초단기실황"
        )
    }

    /**
     * 초단기예보 조회 (6시간 예보)
     * 매시 30분 발표, 45분 후부터 호출 가능
     */
    suspend fun getUltraSrtFcst(
        nx: Int,
        ny: Int,
        baseDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
        baseTime: String = getUltraSrtFcstBaseTime()
    ): KmaWeatherResponse {

        logger.debug("KMA 초단기예보 API 호출 - nx: $nx, ny: $ny, baseDate: $baseDate, baseTime: $baseTime")

        return callKmaApi(
            path = "/getUltraSrtFcst",
            nx = nx,
            ny = ny,
            baseDate = baseDate,
            baseTime = baseTime,
            numOfRows = ULTRA_FCST_ROWS,
            apiName = "초단기예보"
        )
    }

    /**
     * 공통 KMA API 호출 메서드
     */
    private suspend fun callKmaApi(
        path: String,
        nx: Int,
        ny: Int,
        baseDate: String,
        baseTime: String,
        numOfRows: Int,
        apiName: String
    ): KmaWeatherResponse {
        return try {
            val rawResponse = weatherWebClient.get()
                .uri { uriBuilder ->
                    uriBuilder
                        .scheme("https")
                        .host("apis.data.go.kr")
                        .path("$KMA_BASE_URL$path")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("pageNo", 1)
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("dataType", "JSON")
                        .queryParam("base_date", baseDate)
                        .queryParam("base_time", baseTime)
                        .queryParam("nx", nx)
                        .queryParam("ny", ny)
                        .build()
                }
                .retrieve()
                .awaitBody<String>()

            logger.debug("$apiName API 응답 수신 (${rawResponse.length} bytes)")

            val response = parseResponse(rawResponse)
            validateResponse(response, apiName)

            response

        } catch (e: WebClientResponseException) {
            logger.error("$apiName API HTTP 오류 - Status: ${e.statusCode}, Body: ${e.responseBodyAsString}", e)
            throw KmaWeatherApiException("$apiName API HTTP 오류: ${e.statusCode}", e)
        } catch (e: KmaWeatherApiException) {
            throw e
        } catch (e: Exception) {
            logger.error("$apiName API 호출 중 예상치 못한 오류", e)
            throw KmaWeatherApiException("$apiName API 호출 실패", e)
        }
    }

    private fun parseResponse(rawResponse: String): KmaWeatherResponse {
        return try {
            objectMapper.readValue(rawResponse, KmaWeatherResponse::class.java)
        } catch (e: Exception) {
            logger.error("KMA API 응답 파싱 실패", e)
            throw KmaWeatherApiException("응답 파싱 실패", e)
        }
    }

    private fun validateResponse(response: KmaWeatherResponse, apiName: String) {
        val resultCode = response.response.header.resultCode
        if (resultCode != SUCCESS_CODE) {
            val errorMsg = response.response.header.resultMsg
            logger.warn("$apiName API 오류 응답: $errorMsg (Code: $resultCode)")
            throw KmaWeatherApiException("$apiName API 오류: $errorMsg")
        }

        val itemCount = response.response.body?.items?.item?.size ?: 0
        logger.debug("$apiName API 데이터 개수: $itemCount")

        if (itemCount == 0) {
            logger.warn("$apiName API에서 데이터를 받지 못함")
        }
    }

    /**
     * 단기예보용 BaseTime 계산
     */
    fun getCurrentAvailableBaseTime(): String {
        val now = LocalDateTime.now()
        val currentHour = now.hour
        val currentMinute = now.minute

        // 기상청 단기예보 발표시간
        val baseHours = listOf(2, 5, 8, 11, 14, 17, 20, 23)

        // 현재 시간에서 사용 가능한 가장 최근 발표시간 찾기
        val availableHour = baseHours.reversed().find { hour ->
            when {
                hour < currentHour -> true
                hour == currentHour -> currentMinute >= 15
                else -> false
            }
        } ?: run {
            logger.debug("당일 사용 가능한 발표시간 없음, 전날 23시 데이터 사용")
            23
        }

        return formatHour(availableHour)
    }

    /**
     * 초단기실황용 BaseTime 계산
     * 매시 정시 발표, 10분 후부터 사용 가능
     */
    private fun getCurrentHourBaseTime(): String {
        val now = LocalDateTime.now()
        val currentMinute = now.minute

        val baseHour = if (currentMinute >= 10) {
            now.hour
        } else {
            // 10분이 안 지났으면 이전 시간 데이터 사용
            if (now.hour == 0) 23 else now.hour - 1
        }

        return formatHour(baseHour)
    }

    /**
     * 초단기예보용 BaseTime 계산
     * 매시 30분 발표, 45분 후부터 사용 가능
     */
    private fun getUltraSrtFcstBaseTime(): String {
        val now = LocalDateTime.now()
        val currentMinute = now.minute

        val baseHour = when {
            currentMinute >= 45 -> now.hour  // 현재 시간의 30분 발표 데이터 사용 가능
            currentMinute >= 30 -> {
                if (now.hour == 0) 23 else now.hour - 1
            }

            else -> {
                if (now.hour == 0) 23 else now.hour - 1
            }
        }

        return formatHour(baseHour)
    }

    private fun formatHour(hour: Int): String = String.format("%02d00", hour)

    fun getRecommendedApiType(): ApiType {
        val now = LocalDateTime.now()
        val currentHour = now.hour

        return when {
            // 활동 시간대: 실시간 정보 우선
            currentHour in 7..22 -> ApiType.CURRENT_WEATHER
            // 새벽/밤: 예보 정보 우선
            else -> ApiType.FORECAST
        }
    }

    fun getOptimalTempBaseTime(): String {
        val recommendedType = getRecommendedApiType()

        return when (recommendedType) {
            ApiType.CURRENT_WEATHER -> getCurrentHourBaseTime()
            ApiType.FORECAST -> getCurrentAvailableBaseTime()
            ApiType.SHORT_FORECAST -> getUltraSrtFcstBaseTime()
        }
    }
}
