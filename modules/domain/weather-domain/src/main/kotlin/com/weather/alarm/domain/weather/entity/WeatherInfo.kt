package com.weather.alarm.domain.weather.entity

import com.weather.alarm.domain.weather.type.WeatherCondition
import com.weather.alarm.domain.weather.type.WeatherDataSource
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "weather_info",
    indexes = [
        Index(name = "idx_user_date", columnList = "user_id, weather_date"),
        Index(name = "idx_weather_date", columnList = "weather_date"),
        Index(name = "idx_coordinates", columnList = "latitude, longitude"),
        Index(name = "idx_grid_coordinates", columnList = "nx, ny")
    ]
)
class WeatherInfo(
    _id: Long = 0,
    _createdAt: LocalDateTime = LocalDateTime.now(),
    _updatedAt: LocalDateTime = LocalDateTime.now(),
    _userId: Long,
    _weatherDate: LocalDate,
    _latitude: Double,
    _longitude: Double,
    _nx: Int,
    _ny: Int,
    _temperature: Double? = null,
    _temperatureMin: Double? = null,
    _temperatureMax: Double? = null,
    _humidity: Int? = null,
    _weatherCondition: WeatherCondition? = null,
    _weatherDescription: String? = null,
    _precipitation: Double? = null,
    _precipitationProbability: Int? = null,
    _windSpeed: Double? = null,
    _windDirection: String? = null,
    _visibilityKm: Double? = null,
    _uvIndex: Int? = null,
    _airPressure: Double? = null,
    _dataSource: WeatherDataSource = WeatherDataSource.KMA_API
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = _id

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = _createdAt

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = _updatedAt
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


    @Column(name = "weather_description", length = 200)
    var weatherDescription: String? = _weatherDescription
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


    @Column(name = "wind_direction")
    var windDirection: String? = _windDirection
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


    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false, length = 20)
    var dataSource: WeatherDataSource = _dataSource
        private set
    
    fun getFeelsLikeTemperature(): Double? {
        return temperature?.let { temp ->
            windSpeed?.let { wind ->
                when {
                    temp <= 10 && wind >= 4.8 -> {
                        13.12 + 0.6215 * temp - 11.37 * Math.pow(
                            wind * 3.6,
                            0.16
                        ) + 0.3965 * temp * Math.pow(wind * 3.6, 0.16)
                    }

                    else -> temp
                }
            } ?: temperature
        }
    }

    fun isWeatherAlert(): Boolean {
        return when {
            precipitationProbability?.let { it >= 70 } == true -> true
            windSpeed?.let { it >= 15.0 } == true -> true
            temperature?.let { it <= 0 || it >= 35 } == true -> true
            weatherCondition in listOf(
                WeatherCondition.HEAVY_RAIN,
                WeatherCondition.SNOW,
                WeatherCondition.THUNDERSTORM
            ) -> true

            uvIndex?.let { it >= 8 } == true -> true
            else -> false
        }
    }

    fun isGoodWeatherForOutdoor(): Boolean {
        return weatherCondition in listOf(WeatherCondition.CLEAR, WeatherCondition.PARTLY_CLOUDY) &&
                precipitationProbability?.let { it <= 20 } != false &&
                temperature?.let { it in 15.0..28.0 } != false &&
                windSpeed?.let { it <= 10.0 } != false
    }
}
