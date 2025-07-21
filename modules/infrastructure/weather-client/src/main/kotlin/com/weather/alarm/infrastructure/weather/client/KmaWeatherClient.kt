package com.weather.alarm.infrastructure.weather.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.weather.alarm.infrastructure.weather.dto.KmaWeatherResponse
import com.weather.alarm.infrastructure.weather.exception.KmaWeatherApiException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.util.UriUtils
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Component
class KmaWeatherClient(
    private val weatherWebClient: WebClient,
    @Value("\${weather.api.kma.service-key}")
    private val serviceKey: String
) {

    private val logger = LoggerFactory.getLogger(KmaWeatherClient::class.java)

    /**
     * 단기예보 조회 (3일 예보)
     */
    suspend fun getVilageFcst(
        nx: Int,
        ny: Int,
        baseDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
        baseTime: String = getOptimalBaseTime(),
        numOfRows: Int = 1000
    ): KmaWeatherResponse {

        logger.debug("KMA 단기예보 API 호출 시작 - nx: $nx, ny: $ny, baseDate: $baseDate, baseTime: $baseTime")

        return try {
            logger.debug("서비스키 (원본): $serviceKey")
            val encodedKey = UriUtils.encodeQueryParam(serviceKey, StandardCharsets.UTF_8)
            logger.debug("서비스키 (WebClient에 들어갈 최종 인코딩 예상): $encodedKey")

            val rawResponse = weatherWebClient.get()
                .uri { uriBuilder ->
                    uriBuilder
                        .scheme("https")
                        .host("apis.data.go.kr")
                        .path("/1360000/VilageFcstInfoService_2.0/getVilageFcst")
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

            logger.debug("KMA API 원본 응답: ${rawResponse.take(500)}...")

            val objectMapper = ObjectMapper()
            val response = objectMapper.readValue(rawResponse, KmaWeatherResponse::class.java)

            logger.debug("KMA API 응답 수신: resultCode=${response.response.header.resultCode}")

            if (response.response.header.resultCode != "00") {
                logger.warn("KMA API 오류 응답: ${response.response.header.resultMsg}")
                throw KmaWeatherApiException("KMA API 오류: ${response.response.header.resultMsg}")
            } else {
                val itemCount = response.response.body?.items?.item?.size ?: 0
                logger.debug("수신된 데이터 개수: $itemCount")
            }

            response

        } catch (e: WebClientResponseException) {
            logger.error("KMA API HTTP 오류 - Status: ${e.statusCode}, Body: ${e.responseBodyAsString}", e)
            throw KmaWeatherApiException("KMA API HTTP 오류: ${e.statusCode}", e)
        } catch (e: Exception) {
            logger.error("KMA API 호출 중 예상치 못한 오류", e)
            throw KmaWeatherApiException("KMA API 호출 실패", e)
        }
    }

    /**
     * 초단기실황 조회 (현재 날씨)
     */
    suspend fun getUltraSrtNcst(
        nx: Int,
        ny: Int,
        baseDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
        baseTime: String = getCurrentBaseTime()
    ): KmaWeatherResponse {

        logger.debug("KMA 초단기실황 API 호출 - nx: $nx, ny: $ny, baseDate: $baseDate, baseTime: $baseTime")

        return try {
            val rawResponse = weatherWebClient.get()
                .uri { uriBuilder ->
                    uriBuilder
                        .scheme("https")
                        .host("apis.data.go.kr")
                        .path("/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("pageNo", 1)
                        .queryParam("numOfRows", 10)
                        .queryParam("dataType", "JSON")
                        .queryParam("base_date", baseDate)
                        .queryParam("base_time", baseTime)
                        .queryParam("nx", nx)
                        .queryParam("ny", ny)
                        .build()
                }
                .retrieve()
                .awaitBody<String>()

            val objectMapper = ObjectMapper()
            objectMapper.readValue(rawResponse, KmaWeatherResponse::class.java)
        } catch (e: Exception) {
            logger.error("KMA 초단기실황 API 호출 실패", e)
            throw KmaWeatherApiException("초단기실황 조회 실패", e)
        }
    }

    /**
     * 초단기예보 조회 (6시간 예보)
     */
    suspend fun getUltraSrtFcst(
        nx: Int,
        ny: Int,
        baseDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
        baseTime: String = getUltraSrtBaseTime()
    ): KmaWeatherResponse {
        return try {
            val rawResponse = weatherWebClient.get()
                .uri { uriBuilder ->
                    uriBuilder
                        .scheme("http")
                        .host("apis.data.go.kr")
                        .path("/1360000/VilageFcstInfoService_2.0/getUltraSrtFcst")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("pageNo", 1)
                        .queryParam("numOfRows", 60)
                        .queryParam("dataType", "JSON")
                        .queryParam("base_date", baseDate)
                        .queryParam("base_time", baseTime)
                        .queryParam("nx", nx)
                        .queryParam("ny", ny)
                        .build()
                }
                .retrieve()
                .awaitBody<String>()

            val objectMapper = ObjectMapper()
            objectMapper.readValue(rawResponse, KmaWeatherResponse::class.java)
        } catch (e: Exception) {
            logger.error("KMA 초단기예보 API 호출 실패", e)
            throw KmaWeatherApiException("초단기예보 조회 실패", e)
        }
    }

    /**
     * 단기예보 최적 발표시각 계산
     * 02, 05, 08, 11, 14, 17, 20, 23시 발표 (15분 후부터 사용 가능)
     */
    private fun getOptimalBaseTime(): String {
        val now = LocalDateTime.now()
        val currentHour = now.hour
        val currentMinute = now.minute

        // 기상청 발표시간: 02, 05, 08, 11, 14, 17, 20, 23시 (15분 후 사용 가능)
        val baseHours = listOf(2, 5, 8, 11, 14, 17, 20, 23)

        // 현재 시간보다 이전이면서 가장 가까운 발표시간 찾기
        val availableHour = baseHours
            .filter { hour ->
                if (hour == currentHour) {
                    currentMinute >= 15 // 발표 후 15분이 지났는지 확인
                } else {
                    hour < currentHour
                }
            }
            .maxOrNull() ?: 23 // 없으면 전날 23시 데이터 사용

        return String.format("%02d00", availableHour)
    }

    /**
     * 초단기실황 발표시각 계산
     * 매시 정시 발표, 10분 후 호출 가능
     */
    private fun getCurrentBaseTime(): String {
        val now = LocalTime.now()
        val baseHour = if (now.minute >= 10) now.hour else (now.hour - 1).coerceAtLeast(0)
        return String.format("%02d00", baseHour)
    }

    /**
     * 초단기예보 발표시각 계산
     * 매시 30분 발표, 45분 후 호출 가능
     */
    private fun getUltraSrtBaseTime(): String {
        val now = LocalTime.now()
        val baseHour = if (now.minute >= 45) now.hour else (now.hour - 1).coerceAtLeast(0)
        return String.format("%02d30", baseHour)
    }

    fun getCurrentAvailableBaseTime(): String {
        return getOptimalBaseTime()
    }
}
