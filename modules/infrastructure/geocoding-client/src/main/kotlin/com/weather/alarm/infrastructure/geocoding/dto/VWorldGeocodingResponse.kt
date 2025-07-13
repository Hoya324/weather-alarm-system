package com.weather.alarm.infrastructure.geocoding.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * VWorld API 최상위 응답 구조
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class VWorldGeocodingResponse(
    val response: VWorldResponse
)

/**
 * VWorld API 실제 응답 내용
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class VWorldResponse(
    val service: VWorldService? = null,
    val status: String? = null,
    val input: VWorldInput? = null,
    val refined: VWorldRefined? = null,
    val result: VWorldResult? = null,
    val error: VWorldError? = null,
    val record: VWorldRecord? = null,
    val page: VWorldPage? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VWorldService(
    val name: String? = null,
    val version: String? = null,
    val operation: String? = null,
    val time: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VWorldInput(
    val type: String? = null,
    val address: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VWorldRefined(
    val text: String? = null,
    val structure: VWorldStructure? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VWorldStructure(
    val level0: String? = null,   // 국가
    val level1: String? = null,   // 시·도
    val level2: String? = null,   // 시·군·구
    val level3: String? = null,   // (일반구)구
    val level4L: String? = null,  // (도로)도로명, (지번)법정읍·면·동 명
    val level4A: String? = null,  // (도로)행정읍·면·동 명
    val level4AC: String? = null, // (도로)행정읍·면·동 코드
    val level5: String? = null,   // (도로)길, (지번)번지
    val detail: String? = null    // 상세주소
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VWorldResult(
    val crs: String? = null,
    val point: VWorldPoint? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VWorldPoint(
    val x: String? = null,  // VWorld API는 문자열로 반환
    val y: String? = null   // VWorld API는 문자열로 반환
) {
    // VWorld API는 x=경도, y=위도로 반환
    val longitude: Double? get() = x?.toDoubleOrNull()
    val latitude: Double? get() = y?.toDoubleOrNull()
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class VWorldError(
    val level: String? = null,
    val code: String? = null,
    val text: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VWorldRecord(
    val total: String? = null,
    val current: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VWorldPage(
    val total: String? = null,
    val current: String? = null,
    val size: String? = null
)
