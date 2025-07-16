package com.weather.alarm.test

import com.weather.alarm.domain.notification.util.CoordinateConverter

/**
 * 기상청 엑셀 데이터와 좌표 변환 로직 검증
 */
fun main() {
    // 엑셀에서 제공된 샘플 데이터로 검증
    val testCases = listOf(
        // 행정구역, 위도, 경도, 기대되는 격자X, 기대되는 격자Y
        TestCase("서울특별시", 37.5635694444444, 126.980008333333, 60, 127),
        TestCase("종로구", 37.5703777777777, 126.981641666666, 60, 127),
        TestCase("청운효자동", 37.5841367, 126.9706519, 60, 127),
        TestCase("사직동", 37.5732694444444, 126.970955555555, 60, 127),
        TestCase("삼청동", 37.582425, 126.983977777777, 60, 127),
        TestCase("부암동", 37.5898555555555, 126.966444444444, 60, 127),
        TestCase("평창동", 37.6025222222222, 126.968877777777, 60, 127),
        TestCase("무악동", 37.5715388888888, 126.961208333333, 60, 127),
        TestCase("교남동", 37.569075, 126.9641, 60, 127),
        TestCase("가회동", 37.5772527777777, 126.986911111111, 60, 127),
        TestCase("종로1.2.3.4가동", 37.5678861111111, 126.991066666666, 60, 127),
        TestCase("종로5.6가동", 37.5692111111111, 127.007155555555, 61, 127), // 경도가 조금 달라서 X가 61
        TestCase("이화동", 37.5742138888888, 127.006397222222, 61, 127)
    )

    println("=== 기상청 격자 좌표 변환 검증 ===")
    println("%-20s | %s | %s | %s | %s | %s".format(
        "행정구역", "위도", "경도", "기대X,Y", "계산X,Y", "결과"
    ))
    println("-".repeat(100))

    var correctCount = 0
    var totalCount = testCases.size

    testCases.forEach { testCase ->
        val (calculatedX, calculatedY) = CoordinateConverter.convertToGrid(
            testCase.latitude, 
            testCase.longitude
        )
        
        val isCorrect = calculatedX == testCase.expectedX && calculatedY == testCase.expectedY
        if (isCorrect) correctCount++
        
        val result = if (isCorrect) "✅ 정확" else "❌ 오차"
        
        println("%-20s | %.6f | %.6f | %d,%d | %d,%d | %s".format(
            testCase.name,
            testCase.latitude,
            testCase.longitude,
            testCase.expectedX,
            testCase.expectedY,
            calculatedX,
            calculatedY,
            result
        ))
    }

    println("-".repeat(100))
    println("정확도: $correctCount/$totalCount (${(correctCount.toDouble() / totalCount * 100).toInt()}%)")
    
    // 역변환 테스트
    println("\n=== 역변환 테스트 ===")
    val reverseTestCase = testCases[0] // 서울특별시
    val (reverseLat, reverseLon) = CoordinateConverter.convertToLatLon(60, 127)
    println("원본 좌표: (${reverseTestCase.latitude}, ${reverseTestCase.longitude})")
    println("역변환 좌표: ($reverseLat, $reverseLon)")
    println("위도 오차: ${kotlin.math.abs(reverseLat - reverseTestCase.latitude)}")
    println("경도 오차: ${kotlin.math.abs(reverseLon - reverseTestCase.longitude)}")
}

data class TestCase(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val expectedX: Int,
    val expectedY: Int
)
