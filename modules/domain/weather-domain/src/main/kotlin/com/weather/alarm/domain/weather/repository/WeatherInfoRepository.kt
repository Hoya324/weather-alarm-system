package com.weather.alarm.domain.weather.repository

import com.weather.alarm.domain.weather.entity.WeatherInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface WeatherInfoRepository : JpaRepository<WeatherInfo, Long> {

    fun findByUserIdAndWeatherDate(userId: Long, weatherDate: LocalDate): WeatherInfo?

    fun findByUserIdAndWeatherDateAndLatitudeAndLongitude(
        userId: Long,
        weatherDate: LocalDate,
        latitude: Double,
        longitude: Double
    ): WeatherInfo?

    fun findByUserIdAndWeatherDateAndNxAndNy(
        userId: Long,
        weatherDate: LocalDate,
        nx: Int,
        ny: Int
    ): WeatherInfo?

    @Query("""
        SELECT w FROM WeatherInfo w 
        WHERE w.userId = :userId 
        AND w.weatherDate BETWEEN :startDate AND :endDate
        ORDER BY w.weatherDate
    """)
    fun findByUserIdAndWeatherDateBetween(
        @Param("userId") userId: Long,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<WeatherInfo>

    @Query("""
        SELECT w FROM WeatherInfo w 
        WHERE w.weatherDate = :weatherDate
        AND w.latitude BETWEEN :minLat AND :maxLat
        AND w.longitude BETWEEN :minLon AND :maxLon
    """)
    fun findByWeatherDateAndCoordinateRange(
        @Param("weatherDate") weatherDate: LocalDate,
        @Param("minLat") minLat: Double,
        @Param("maxLat") maxLat: Double,
        @Param("minLon") minLon: Double,
        @Param("maxLon") maxLon: Double
    ): List<WeatherInfo>

    @Query("""
        SELECT w FROM WeatherInfo w 
        WHERE w.weatherDate = :weatherDate
        AND w.nx = :nx AND w.ny = :ny
    """)
    fun findByWeatherDateAndGrid(
        @Param("weatherDate") weatherDate: LocalDate,
        @Param("nx") nx: Int,
        @Param("ny") ny: Int
    ): WeatherInfo?

    fun deleteByWeatherDateBefore(weatherDate: LocalDate): Long
}
