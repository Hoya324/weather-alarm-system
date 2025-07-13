package com.weather.alarm.domain.port.out

import com.weather.alarm.domain.user.vo.Coordinate

interface GeocodingPort {

    /**
     * 주소를 좌표로 변환
     * @param address 변환할 주소
     * @return 좌표 정보 (실패 시 null)
     */
    fun getCoordinatesByAddress(address: String): Coordinate?

    /**
     * 주소 유효성 검증
     * @param address 검증할 주소
     * @return 유효한 주소 여부
     */
    fun isValidAddress(address: String): Boolean
}