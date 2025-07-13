package com.weather.alarm.infrastructure.geocoding.application

import com.weather.alarm.domain.port.out.GeocodingPort
import com.weather.alarm.domain.user.vo.Coordinate
import com.weather.alarm.infrastructure.geocoding.adapter.GeocodingAdapter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GeocodingService(
    private val geocodingAdapter: GeocodingAdapter
) : GeocodingPort {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun getCoordinatesByAddress(address: String): Coordinate? {
        return try {
            val coordinate = geocodingAdapter.getCoordinatesByAddress(address)
            if (coordinate != null) {
                logger.debug("주소 좌표 변환 성공: $address -> ${coordinate.latitude}, ${coordinate.longitude}")
            } else {
                logger.warn("주소 좌표 변환 실패: $address")
            }
            coordinate
        } catch (e: Exception) {
            logger.error("주소 좌표 변환 중 오류 발생: $address", e)
            null
        }
    }

    override fun isValidAddress(address: String): Boolean {
        return geocodingAdapter.isValidAddress(address)
    }
}