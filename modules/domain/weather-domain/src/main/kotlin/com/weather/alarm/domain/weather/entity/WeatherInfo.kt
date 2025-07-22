package com.weather.alarm.domain.weather.entity

import com.weather.alarm.domain.weather.type.WeatherCondition
import com.weather.alarm.domain.weather.type.WeatherDataSource
import com.weather.alarm.domain.weather.type.WeatherStatus
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "weather_info",
    indexes = [
        Index(name = "idx_user_date", columnList = "user_id, weather_date"),
        Index(name = "idx_weather_date", columnList = "weather_date"),
        Index(name = "idx_coordinates", columnList = "latitude, longitude"),
        Index(name = "idx_grid_coordinates", columnList = "nx, ny"),
        Index(name = "idx_weather_date_grid", columnList = "weather_date, nx, ny") // 추가된 복합 인덱스
    ]
)
@EntityListeners(AuditingEntityListener::class)
class WeatherInfo(
    _id: Long = 0,
    _userId: Long,
    _weatherDate: LocalDate,
    _latitude: Double,
    _longitude: Double,
    _nx: Int,
    _ny: Int,

    // 기본 온도 정보 (단기예보)
    _temperature: Double? = null,
    _temperatureMin: Double? = null,
    _temperatureMax: Double? = null,

    // 실시간 정보 (초단기실황)
    _currentTemperature: Double? = null,
    _currentHumidity: Int? = null,
    _currentWindSpeed: Double? = null,
    _currentWindDirection: Int? = null,
    _currentPrecipitation: Double? = null,
    _currentPrecipitationType: String? = null,

    // 기본 날씨 정보
    _humidity: Int? = null,
    _weatherCondition: WeatherCondition? = null,
    _precipitation: Double? = null,
    _precipitationProbability: Int? = null,
    _windSpeed: Double? = null,
    _windDirection: String? = null,
    _visibilityKm: Double? = null,
    _uvIndex: Int? = null,
    _airPressure: Double? = null,

    // 상세 날씨 정보 (초단기예보)
    _skyCondition: Int? = null,  // SKY: 맑음(1), 구름많음(3), 흐림(4)
    _precipitationType: Int? = null,  // PTY: 없음(0), 비(1), 비/눈(2), 눈(3), 소나기(4)
    _lightning: Double? = null,  // LGT: 낙뢰

    _dataSource: WeatherDataSource = WeatherDataSource.KMA_API,
    _hasCurrentData: Boolean = false,
    _hasHourlyForecast: Boolean = false
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = _id

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        private set

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
        private set

    @Column(name = "user_id", nullable = false)
    var userId: Long = _userId
        private set

    @Column(name = "weather_date", nullable = false)
    var weatherDate: LocalDate = _weatherDate
        private set

    @Column(precision = 10)
    var latitude: Double = _latitude
        private set

    @Column(precision = 10)
    var longitude: Double = _longitude
        private set

    @Column(nullable = false)
    var nx: Int = _nx
        private set

    @Column(nullable = false)
    var ny: Int = _ny
        private set

    @Column(precision = 5)
    var temperature: Double? = _temperature
        private set

    @Column(name = "temperature_min", precision = 5)
    var temperatureMin: Double? = _temperatureMin
        private set

    @Column(name = "temperature_max", precision = 5)
    var temperatureMax: Double? = _temperatureMax
        private set

    @Column
    var humidity: Int? = _humidity
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "weather_condition", length = 50)
    var weatherCondition: WeatherCondition? = _weatherCondition
        private set

    @Column(precision = 5)
    var precipitation: Double? = _precipitation
        private set

    @Column(name = "precipitation_probability")
    var precipitationProbability: Int? = _precipitationProbability
        private set

    @Column(name = "wind_speed", precision = 5)
    var windSpeed: Double? = _windSpeed
        private set

    @Column(name = "wind_direction", length = 10)
    var windDirection: String? = _windDirection
        private set

    @Column(name = "current_temperature", precision = 5)
    var currentTemperature: Double? = _currentTemperature
        private set

    @Column(name = "current_humidity")
    var currentHumidity: Int? = _currentHumidity
        private set

    @Column(name = "current_wind_speed", precision = 5)
    var currentWindSpeed: Double? = _currentWindSpeed
        private set

    @Column(name = "current_wind_direction")
    var currentWindDirection: Int? = _currentWindDirection
        private set

    @Column(name = "current_precipitation", precision = 5)
    var currentPrecipitation: Double? = _currentPrecipitation
        private set

    @Column(name = "current_precipitation_type", length = 10)
    var currentPrecipitationType: String? = _currentPrecipitationType
        private set

    @Column(name = "visibility_km", precision = 5)
    var visibilityKm: Double? = _visibilityKm
        private set

    @Column(name = "uv_index")
    var uvIndex: Int? = _uvIndex
        private set

    @Column(name = "air_pressure", precision = 7)
    var airPressure: Double? = _airPressure
        private set

    @Column(name = "sky_condition")
    var skyCondition: Int? = _skyCondition
        private set

    @Column(name = "precipitation_type")
    var precipitationType: Int? = _precipitationType
        private set

    @Column(name = "lightning", precision = 5)
    var lightning: Double? = _lightning
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false, length = 20)
    var dataSource: WeatherDataSource = _dataSource
        private set

    @Column(name = "has_current_data", nullable = false)
    var hasCurrentData: Boolean = _hasCurrentData
        private set

    @Column(name = "has_hourly_forecast", nullable = false)
    var hasHourlyForecast: Boolean = _hasHourlyForecast
        private set

    fun updateCurrentTemperature(temp: Double) {
        this.currentTemperature = temp
        this.hasCurrentData = true
        this.updatedAt = LocalDateTime.now()
    }

    fun updateCurrentHumidity(humidity: Int) {
        this.currentHumidity = humidity
        this.hasCurrentData = true
        this.updatedAt = LocalDateTime.now()
    }

    fun updateCurrentWindSpeed(windSpeed: Double) {
        this.currentWindSpeed = windSpeed
        this.hasCurrentData = true
        this.updatedAt = LocalDateTime.now()
    }

    fun getFeelsLikeTemperature(): Double? {
        val temp = getCurrentTemp()
        val wind = getCurrentWindSpeedValue()

        return temp?.let { t ->
            wind?.let { w ->
                when {
                    t <= 10 && w >= 4.8 -> {
                        // 체감온도 공식 (바람 한랭지수)
                        13.12 + 0.6215 * t - 11.37 * Math.pow(w * 3.6, 0.16) + 0.3965 * t * Math.pow(w * 3.6, 0.16)
                    }

                    t >= 27 -> {
                        // 열지수 계산
                        t + ((currentHumidity ?: humidity ?: 50) - 40) * 0.1
                    }

                    else -> t
                }
            } ?: t
        }
    }

    fun getCurrentTemp(): Double? = currentTemperature ?: temperature
    fun getCurrentHumidityValue(): Int? = currentHumidity ?: humidity
    fun getCurrentWindSpeedValue(): Double? = currentWindSpeed ?: windSpeed

    fun hasCurrentPrecipitation(): Boolean {
        return currentPrecipitation?.let { it > 0 } == true ||
                currentPrecipitationType?.let { it != "0" && it.isNotEmpty() } == true
    }

    fun getSkyDescription(): String? {
        return skyCondition?.let { sky ->
            when (sky) {
                1 -> "맑음"
                3 -> "구름많음"
                4 -> "흐림"
                else -> null
            }
        }
    }

    fun getPrecipitationTypeDescription(): String? {
        return precipitationType?.let { type ->
            when (type) {
                0 -> "강수없음"
                1 -> "비"
                2 -> "비/눈"
                3 -> "눈"
                4 -> "소나기"
                else -> null
            }
        }
    }


    fun isWeatherAlert(): Boolean {
        return when {
            // 강수확률 높음
            precipitationProbability?.let { it >= 70 } == true -> true
            // 강풍
            getCurrentWindSpeedValue()?.let { it >= 15.0 } == true -> true
            // 강추위 / 무더위
            getCurrentTemp()?.let { it <= 0 || it >= 35 } == true -> true
            // 위험한 날씨 상태
            weatherCondition in listOf(
                WeatherCondition.HEAVY_RAIN,
                WeatherCondition.SNOW,
                WeatherCondition.THUNDERSTORM
            ) -> true
            // 높은 자외선
            uvIndex?.let { it >= 8 } == true -> true
            // 낙뢰 감지
            lightning?.let { it > 0 } == true -> true
            // 눈 또는 소나기
            precipitationType in listOf(3, 4) -> true
            // 현재 강수중
            hasCurrentPrecipitation() -> true
            else -> false
        }
    }

    private fun isGoodWeatherForOutdoor(): Boolean {
        val currentTemp = getCurrentTemp()
        val currentWind = getCurrentWindSpeedValue()
        val hasRain = hasCurrentPrecipitation()

        return when {
            hasRain -> false
            isWeatherAlert() -> false
            else -> {
                val goodWeather = weatherCondition in listOf(
                    WeatherCondition.CLEAR,
                    WeatherCondition.PARTLY_CLOUDY
                )
                val lowRainChance = precipitationProbability?.let { it <= 20 } != false
                val goodTemp = currentTemp?.let { it in 15.0..28.0 } != false
                val calmWind = currentWind?.let { it <= 10.0 } != false
                val noLightning = lightning?.let { it <= 0.0 } != false

                goodWeather && lowRainChance && goodTemp && calmWind && noLightning
            }
        }
    }

    fun getOverallWeatherStatus(): WeatherStatus {
        return when {
            isWeatherAlert() -> WeatherStatus.ALERT
            !isGoodWeatherForOutdoor() -> WeatherStatus.CAUTION
            else -> WeatherStatus.GOOD
        }
    }

    fun getWeatherAlerts(): List<String> {
        val alerts = mutableListOf<String>()

        // 강수확률 경보
        precipitationProbability?.let { prob ->
            if (prob >= 70) alerts.add("높은 강수확률: ${prob}%")
        }

        // 풍속 경보
        getCurrentWindSpeedValue()?.let { wind ->
            if (wind >= 21.0) {
                alerts.add("강풍경보: ${wind}m/s (매우 위험)")
            } else if (wind >= 14.0) {
                alerts.add("강풍주의보: ${wind}m/s (주의 필요)")
            } else {
            }
        }

        // 온도 경보
        getCurrentTemp()?.let { temp ->
            if (temp <= -12) {
                alerts.add("한파경보: ${temp.toInt()}°C (매우 위험)")
            } else if (temp <= -5) {
                alerts.add("한파주의보: ${temp.toInt()}°C (주의 필요)")
            } else if (temp >= 38) {
                alerts.add("폭염경보: ${temp.toInt()}°C (매우 위험)")
            } else if (temp >= 35) {
                alerts.add("폭염주의보: ${temp.toInt()}°C (주의 필요)")
            } else {
            }
        }

        // 강수 경보
        if (hasCurrentPrecipitation()) {
            currentPrecipitation?.let { rain ->
                if (rain >= 50) {
                    alerts.add("매우 많은 비: ${rain}mm/h (위험)")
                } else if (rain >= 20) {
                    alerts.add("많은 비: ${rain}mm/h (주의)")
                } else if (rain > 0) {
                    alerts.add("현재 강수중: ${rain}mm/h")
                } else {
                }
            }
        }

        // 낙뢰 경보
        lightning?.let { lightning ->
            if (lightning > 0) alerts.add("낙뢰 위험: 야외활동 자제")
        }

        // 자외선 경보
        uvIndex?.let { uv ->
            if (uv >= 11) {
                alerts.add("자외선 위험: ${uv} (외출 금지 권고)")
            } else if (uv >= 8) {
                alerts.add("높은 자외선: ${uv} (차단 필수)")
            } else {
            }
        }

        return alerts
    }

    fun getAllRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()

        if (isGoodWeatherForOutdoor()) {
            recommendations.add("✨ 외출하기 좋은 날씨입니다!")
        } else if (isWeatherAlert()) {
            recommendations.add("⚠️ 위험한 날씨입니다. 외출 시 각별히 주의하세요!")
        }

        getTemperatureRecommendation()?.let { recommendations.add(it) }
        getPrecipitationRecommendation()?.let { recommendations.add(it) }
        getWindRecommendation()?.let { recommendations.add(it) }
        getHumidityRecommendation()?.let { recommendations.add(it) }
        getUvRecommendation()?.let { recommendations.add(it) }
        getSpecialRecommendation()?.let { recommendations.add(it) }

        return recommendations
    }

    private fun getTemperatureRecommendation(): String? {
        val feelsLikeTemp = getFeelsLikeTemperature() ?: getCurrentTemp()

        return feelsLikeTemp?.let { temp ->
            when {
                temp <= -10 -> "🧥 방한복, 두꺼운 외투, 장갑, 목도리 착용 필수"
                temp <= 0 -> "🧥 두꺼운 외투와 방한용품을 준비하세요"
                temp <= 5 -> "🧥 따뜻한 외투를 착용하세요"
                temp <= 10 -> "🧥 가벼운 외투나 카디건을 준비하세요"
                temp <= 19 -> "👔 긴팔 셔츠나 얇은 겉옷이 적당합니다"
                temp <= 24 -> "👕 반팔이나 얇은 긴팔이 적당합니다"
                temp <= 29 -> "👕 가벼운 여름옷차림이 좋습니다"
                temp <= 34 -> "🧴 가벼운 옷과 자외선 차단제를 준비하세요"
                else -> "🚨 극도로 더위가 심합니다. 야외활동을 자제하고 충분한 수분을 섭취하세요"
            }
        }
    }

    private fun getPrecipitationRecommendation(): String? {
        val hasRain = hasCurrentPrecipitation()
        val rainProbability = precipitationProbability ?: 0

        return when {
            hasRain -> {
                currentPrecipitation?.let { rain ->
                    when {
                        rain >= 50 -> "🚨 매우 많은 비! 외출을 삼가고 안전한 곳에 머무르세요"
                        rain >= 20 -> "☂️ 많은 비가 내립니다. 우산과 우비를 준비하세요"
                        else -> "☂️ 우산을 챙기세요"
                    }
                }
            }

            rainProbability >= 70 -> "☂️ 우산을 챙기세요 (강수확률 ${rainProbability}%)"
            rainProbability >= 40 -> "☂️ 접이식 우산을 준비해두세요 (강수확률 ${rainProbability}%)"
            else -> null
        }
    }

    private fun getWindRecommendation(): String? {
        return getCurrentWindSpeedValue()?.let { wind ->
            when {
                wind >= 21.0 -> "🚨 매우 강한 바람! 외출을 삼가고 야외 물품을 고정하세요"
                wind >= 14.0 -> "💨 강한 바람이 붑니다. 모자나 가벼운 물건 주의하세요"
                wind >= 9.0 -> "💨 바람이 다소 강합니다. 가벼운 옷차림 주의하세요"
                else -> null
            }
        }
    }

    private fun getHumidityRecommendation(): String? {
        return getCurrentHumidityValue()?.let { humidity ->
            when {
                humidity < 20 -> "💧 매우 건조합니다. 충분한 수분 섭취와 보습에 신경쓰세요"
                humidity < 30 -> "💧 건조합니다. 수분 섭취를 늘리고 보습제를 사용하세요"
                humidity < 40 -> "💧 다소 건조합니다. 수분 섭취에 신경쓰세요"
                humidity <= 60 -> "😊 쾌적한 습도입니다"
                humidity <= 70 -> "🌫️ 다소 습합니다"
                humidity <= 80 -> "🌫️ 습합니다. 통풍에 신경쓰세요"
                else -> "🌫️ 매우 습합니다. 불쾌감을 느낄 수 있어요"
            }
        }
    }

    private fun getUvRecommendation(): String? {
        return uvIndex?.let { uv ->
            when {
                uv >= 11 -> "🚨 자외선이 극도로 강합니다! 가급적 외출을 피하세요"
                uv >= 8 -> "🕶️ 자외선이 매우 강합니다. 선크림, 모자, 선글라스 필수"
                uv >= 6 -> "🧴 자외선이 강합니다. 선크림 사용을 권합니다"
                uv >= 3 -> "☀️ 적당한 자외선입니다. 장시간 노출 시 주의하세요"
                else -> null
            }
        }
    }

    private fun getSpecialRecommendation(): String? {
        return lightning?.let { lightning ->
            if (lightning > 0) {
                "⚡ 낙뢰가 예상됩니다. 야외활동을 피하고 안전한 건물 내부로 대피하세요"
            } else null
        }
    }

    fun getWindDirectionDescription(): String? {
        return currentWindDirection?.let { degree ->
            when (degree) {
                in 0..22 -> "북"
                in 23..67 -> "북동"
                in 68..112 -> "동"
                in 113..157 -> "남동"
                in 158..202 -> "남"
                in 203..247 -> "남서"
                in 248..292 -> "서"
                in 293..337 -> "북서"
                in 338..360 -> "북"
                else -> "?"
            }
        }
    }

    fun getWindStrengthDescription(): String? {
        return getCurrentWindSpeedValue()?.let { speed ->
            when {
                speed < 4 -> "약함"
                speed < 9 -> "보통"
                speed < 14 -> "강함"
                else -> "매우강함"
            }
        }
    }
}
