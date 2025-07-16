package com.weather.alarm.infrastructure.weather.client

import com.weather.alarm.infrastructure.weather.dto.KmaWeatherResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Component
class KmaWeatherClient(
    private val webClient: WebClient,
    @Value("\${weather.api.kma.base-url:http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0}")
    private val baseUrl: String,
    @Value("\${weather.api.kma.service-key}")
    private val serviceKey: String
) {

    /**
     * 단기예보 조회 (3일 예보)
     */
    suspend fun getVilageFcst(
        nx: Int,
        ny: Int,
        baseDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
        baseTime: String = getOptimalBaseTime()
    ): KmaWeatherResponse {
        return webClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/getVilageFcst")
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 1000)
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", baseTime)
                    .queryParam("nx", nx)
                    .queryParam("ny", ny)
                    .build()
            }
            .retrieve()
            .awaitBody<KmaWeatherResponse>()
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
        return webClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/getUltraSrtNcst")
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
            .awaitBody<KmaWeatherResponse>()
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
        return webClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/getUltraSrtFcst")
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
            .awaitBody<KmaWeatherResponse>()
    }

    /**
     * 단기예보 최적 발표시각 계산
     * 02, 05, 08, 11, 14, 17, 20, 23시 발표
     */
    private fun getOptimalBaseTime(): String {
        val now = LocalTime.now()
        val baseTimes = listOf(2, 5, 8, 11, 14, 17, 20, 23)
        
        // 현재 시간보다 이전인 가장 최근 발표시각 찾기
        val currentHour = now.hour
        val baseHour = baseTimes.findLast { it <= currentHour } ?: 23
        
        return String.format("%02d00", baseHour)
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
}
