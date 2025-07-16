package com.weather.alarm.domain.notification.util

import kotlin.math.*

/**
 * 위경도 좌표를 기상청 격자좌표(X,Y)로 변환하는 유틸리티
 * 기상청 단기예보 API에서 사용하는 Lambert Conformal Conic 투영법 기반
 */
object CoordinateConverter {

    // 기상청 단기예보 격자 정보 (공식 명세서 기준)
    private const val RE = 6371.00877 // 지구 반지름(km)
    private const val GRID = 5.0       // 격자 간격(km)
    private const val SLAT1 = 30.0     // 표준위도1(degree)
    private const val SLAT2 = 60.0     // 표준위도2(degree)
    private const val OLON = 126.0     // 기준점 경도(degree)
    private const val OLAT = 38.0      // 기준점 위도(degree)
    private const val XO = 210.0 / GRID  // 기준점 X좌표(격자) = 42.0
    private const val YO = 675.0 / GRID  // 기준점 Y좌표(격자) = 135.0

    /**
     * 위경도를 기상청 격자 좌표로 변환
     */
    fun convertToGrid(latitude: Double, longitude: Double): Pair<Int, Int> {
        val degrad = PI / 180.0
        val re = RE / GRID
        val slat1 = SLAT1 * degrad
        val slat2 = SLAT2 * degrad
        val olon = OLON * degrad
        val olat = OLAT * degrad

        var sn = tan(PI * 0.25 + slat2 * 0.5) / tan(PI * 0.25 + slat1 * 0.5)
        sn = ln(cos(slat1) / cos(slat2)) / ln(sn)
        var sf = tan(PI * 0.25 + slat1 * 0.5)
        sf = sf.pow(sn) * cos(slat1) / sn
        var ro = tan(PI * 0.25 + olat * 0.5)
        ro = re * sf / ro.pow(sn)

        var ra = tan(PI * 0.25 + latitude * degrad * 0.5)
        ra = re * sf / ra.pow(sn)

        var theta = longitude * degrad - olon
        if (theta > PI) theta -= 2.0 * PI
        if (theta < -PI) theta += 2.0 * PI
        theta *= sn

        val x = (ra * sin(theta) + XO + 0.5).toInt()
        val y = (ro - ra * cos(theta) + YO + 0.5).toInt()

        return Pair(x, y)
    }

    /**
     * 기상청 격자 좌표를 위경도로 변환
     */
    fun convertToLatLon(nx: Int, ny: Int): Pair<Double, Double> {
        val degrad = PI / 180.0
        val raddeg = 180.0 / PI
        val re = RE / GRID
        val slat1 = SLAT1 * degrad
        val slat2 = SLAT2 * degrad
        val olon = OLON * degrad
        val olat = OLAT * degrad

        var sn = tan(PI * 0.25 + slat2 * 0.5) / tan(PI * 0.25 + slat1 * 0.5)
        sn = ln(cos(slat1) / cos(slat2)) / ln(sn)
        var sf = tan(PI * 0.25 + slat1 * 0.5)
        sf = sf.pow(sn) * cos(slat1) / sn
        var ro = tan(PI * 0.25 + olat * 0.5)
        ro = re * sf / ro.pow(sn)

        val xn = nx.toDouble() - XO
        val yn = ro - ny.toDouble() + YO
        val ra = sqrt(xn * xn + yn * yn)
        val alat = 2.0 * atan((re * sf / ra).pow(1.0 / sn)) - PI * 0.5

        val theta = if (abs(xn) <= 0.0) {
            0.0
        } else {
            if (abs(yn) <= 0.0) {
                if (xn < 0.0) -PI * 0.5 else PI * 0.5
            } else {
                atan2(xn, yn)
            }
        }

        val alon = theta / sn + olon

        return Pair(alat * raddeg, alon * raddeg)
    }

    /**
     * 두 좌표 간의 거리 계산 (Haversine 공식, 단위: km)
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // 지구 반지름 (km)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    /**
     * 한국 영역 내 좌표인지 확인
     */
    fun isInKorea(latitude: Double, longitude: Double): Boolean {
        return latitude in 33.0..43.0 && longitude in 124.0..132.0
    }

    /**
     * 유효한 격자 좌표인지 확인
     */
    fun isValidGrid(nx: Int, ny: Int): Boolean {
        return nx in 1..149 && ny in 1..253
    }
}
