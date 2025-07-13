package com.weather.alarm.infrastructure.geocoding.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.weather.alarm.infrastructure.geocoding.dto.VWorldGeocodingResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Duration

@Component
class VWorldGeocodingClient(
    @Value("\${vworld.api.key}")
    private val apiKey: String,
    private val objectMapper: ObjectMapper,
    private val webClient: WebClient = WebClient.builder()
        .codecs { configurer ->
            configurer.defaultCodecs().maxInMemorySize(1024 * 1024) // 1MB
        }
        .build()
) {
    private val logger = LoggerFactory.getLogger(VWorldGeocodingClient::class.java)

    companion object {
        private const val BASE_URL = "https://api.vworld.kr/req/address"
        private const val SERVICE = "address"
        private const val REQUEST = "getCoord"  // 대소문자 주의
        private const val VERSION = "2.0"
        private const val CRS = "epsg:4326"
        private const val FORMAT = "json"
        private const val TYPE_ROAD = "ROAD"
        private const val TYPE_PARCEL = "PARCEL"
        private const val REFINE = "true"
        private const val SIMPLE = "false"
    }

    /**
     * 도로명 주소로 좌표 조회
     */
    fun getCoordinatesByAddress(address: String): VWorldGeocodingResponse? {
        return getCoordinates(address, TYPE_ROAD)
    }

    /**
     * 지번 주소로 좌표 조회
     */
    fun getCoordinatesByParcelAddress(address: String): VWorldGeocodingResponse? {
        return getCoordinates(address, TYPE_PARCEL)
    }

    /**
     * 도로명 주소 우선, 실패 시 지번 주소로 재시도
     */
    fun getCoordinatesWithFallback(address: String): VWorldGeocodingResponse? {
        // 먼저 도로명 주소로 시도
        val roadResult = getCoordinates(address, TYPE_ROAD)
        if (roadResult?.response?.status == "OK") {
            return roadResult
        }

        logger.debug("도로명 주소 조회 실패, 지번 주소로 재시도: $address")

        // 도로명 주소 실패 시 지번 주소로 재시도
        val parcelResult = getCoordinates(address, TYPE_PARCEL)
        if (parcelResult?.response?.status == "OK") {
            return parcelResult
        }

        logger.warn("도로명/지번 주소 모두 조회 실패: $address")
        return roadResult
    }

    private fun getCoordinates(address: String, addressType: String): VWorldGeocodingResponse? {
        return try {

            logger.debug("VWorld API 호출 - 주소: $address, 타입: $addressType")

            val response = webClient.get()
                .uri { uriBuilder ->
                    uriBuilder.scheme("https")
                        .host("api.vworld.kr")
                        .path("/req/address")
                        .queryParam("service", SERVICE)
                        .queryParam("request", REQUEST)
                        .queryParam("version", VERSION)
                        .queryParam("crs", CRS)
                        .queryParam("address", address)
                        .queryParam("refine", REFINE)
                        .queryParam("simple", SIMPLE)
                        .queryParam("format", FORMAT)
                        .queryParam("type", addressType)
                        .queryParam("key", apiKey)
                        .build()
                }
                .retrieve()
                .bodyToMono(String::class.java)
                .timeout(Duration.ofSeconds(10))
                .doOnNext { rawResponse ->
                    logger.debug("VWorld API 원본 응답 (타입: $addressType): $rawResponse")
                }
                .flatMap { rawResponse ->
                    try {
                        val response = objectMapper.readValue(rawResponse, VWorldGeocodingResponse::class.java)
                        Mono.just(response)
                    } catch (e: Exception) {
                        logger.error("JSON 파싱 오류: $rawResponse", e)
                        Mono.empty()
                    }
                }
                .block()

            when (response?.response?.status) {
                "OK" -> {
                    logger.debug("VWorld API 좌표 조회 성공 (타입: $addressType): $address -> ${response.response.result?.point}")
                    response
                }

                "NOT_FOUND" -> {
                    logger.debug("VWorld API 주소를 찾을 수 없음 (타입: $addressType): $address")
                    response
                }

                "ERROR" -> {
                    logger.warn("VWorld API 에러 (타입: $addressType): $address, 에러: ${response.response.error}")
                    response
                }

                else -> {
                    logger.warn("VWorld API 알 수 없는 상태 (타입: $addressType): $address, 상태: ${response?.response?.status}")
                    response
                }
            }
        } catch (e: WebClientResponseException) {
            logger.error("VWorld API 응답 오류: ${e.statusCode} - ${e.responseBodyAsString}")
            null
        } catch (e: Exception) {
            logger.error("VWorld API 호출 중 오류 발생: $address", e)
            null
        }
    }
}
