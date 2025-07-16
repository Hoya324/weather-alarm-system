package com.weather.alarm.infrastructure.geocoding.adapter

import com.weather.alarm.domain.notification.vo.Coordinate
import com.weather.alarm.domain.port.out.GeocodingPort
import com.weather.alarm.infrastructure.geocoding.client.VWorldGeocodingClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class GeocodingAdapter(
    private val vWorldGeocodingClient: VWorldGeocodingClient
) : GeocodingPort {
    private val logger = LoggerFactory.getLogger(GeocodingAdapter::class.java)

    override fun getCoordinatesByAddress(address: String): Coordinate? {
        logger.debug("주소 좌표 변환 시작: $address")

        val response = vWorldGeocodingClient.getCoordinatesWithFallback(address)
            ?: return null

        when (response.response.status) {
            "OK" -> {
                val point = response.response.result?.point
                return if (point?.latitude != null && point.longitude != null) {
                    val coordinate = Coordinate.from(point.latitude!!, point.longitude!!)

                    logger.info("좌표 변환 성공 - 주소: $address -> nx: ${coordinate.nx}, ny: ${coordinate.ny}")

                    response.response.refined?.text?.let { refinedAddress ->
                        logger.debug("정제된 주소: $refinedAddress")
                    }

                    coordinate
                } else {
                    logger.warn("좌표 변환 실패 - 주소: $address, 좌표 정보 없음")
                    null
                }
            }

            "NOT_FOUND" -> {
                logger.warn("주소를 찾을 수 없음: $address")
                logAddressSuggestions(address)
            }

            "ERROR" -> {
                val errorInfo = response.response.error
                logger.error("VWorld API 에러 - 주소: $address, 코드: ${errorInfo?.code}, 메시지: ${errorInfo?.text}")
            }

            else -> {
                logger.warn("알 수 없는 응답 상태 - 주소: $address, 상태: ${response.response.status}")
            }
        }
        return null
    }

    private fun logAddressSuggestions(address: String) {
        logger.info("주소 입력 가이드:")
        logger.info("• 도로명 주소 예시: '서울특별시 강남구 테헤란로 152'")
        logger.info("• 지번 주소 예시: '서울특별시 강남구 역삼동 737'")
        logger.info("• 시/도, 시/군/구, 동/읍/면을 포함한 상세 주소를 입력해주세요")

        // 입력된 주소 분석 및 제안
        val suggestions = mutableListOf<String>()

        if (!address.contains("시") && !address.contains("도")) {
            suggestions.add("시/도 정보 추가")
        }
        if (!address.contains("구") && !address.contains("군")) {
            suggestions.add("시/군/구 정보 추가")
        }
        if (!address.contains("동") && !address.contains("읍") && !address.contains("면") && !address.contains("로") && !address.contains(
                "길"
            )
        ) {
            suggestions.add("동/읍/면 또는 도로명 추가")
        }

        if (suggestions.isNotEmpty()) {
            logger.info("입력하신 주소 '$address'에 다음 정보를 추가해보세요: ${suggestions.joinToString(", ")}")
        }
    }

    override fun isValidAddress(address: String): Boolean {
        return try {
            val coordinate = getCoordinatesByAddress(address)
            coordinate != null
        } catch (e: Exception) {
            logger.error("주소 유효성 검증 중 오류 발생: $address", e)
            false
        }
    }

    /**
     * 주소의 구조화된 정보를 반환
     */
    fun getAddressStructure(address: String): Map<String, String?> {
        val response = vWorldGeocodingClient.getCoordinatesWithFallback(address)

        if (response?.response?.status == "OK" && response.response.refined?.structure != null) {
            val structure = response.response.refined.structure
            return mapOf(
                "country" to structure.level0,
                "province" to structure.level1,
                "city" to structure.level2,
                "district" to structure.level3,
                "area" to structure.level4L,
                "adminArea" to structure.level4A,
                "road" to structure.level5,
                "detail" to structure.detail,
                "refinedText" to response.response.refined.text
            )
        }

        return emptyMap()
    }
}
