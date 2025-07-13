package com.weather.alarm.domain.user.vo

import jakarta.persistence.Embeddable
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Embeddable
data class Coordinate(
    val latitude: Double,
    val longitude: Double
) {
    init {
        require(latitude in -90.0..90.0) { "위도는 -90.0에서 90.0 사이여야 합니다." }
        require(longitude in -180.0..180.0) { "경도는 -180.0에서 180.0 사이여야 합니다." }
    }

    /**
     * 한국 영역 내 좌표인지 확인
     */
    fun isInKorea(): Boolean {
        return latitude in 33.0..43.0 && longitude in 124.0..132.0
    }

    /**
     * 두 좌표 간의 거리 계산 (Haversine 공식, 단위: km)
     */
    fun distanceTo(other: Coordinate): Double {
        val R = 6371.0 // 지구 반지름 (km)

        val lat1Rad = Math.toRadians(latitude)
        val lat2Rad = Math.toRadians(other.latitude)
        val deltaLatRad = Math.toRadians(other.latitude - latitude)
        val deltaLonRad = Math.toRadians(other.longitude - longitude)

        val a = sin(deltaLatRad / 2) * sin(deltaLatRad / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLonRad / 2) * sin(deltaLonRad / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c
    }
}
