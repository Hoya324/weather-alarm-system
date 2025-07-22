package com.weather.alarm.domain.weather.repository

import com.weather.alarm.domain.weather.entity.WeatherInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface WeatherInfoRepository : JpaRepository<WeatherInfo, Long> {

    fun findByUserIdAndWeatherDateAndNxAndNy(
        userId: Long,
        weatherDate: LocalDate,
        nx: Int,
        ny: Int
    ): WeatherInfo?

    @Query(
        """
        select w from WeatherInfo w 
        where w.weatherDate = :weatherDate
        and w.nx = :nx AND w.ny = :ny
    """
    )
    fun findByWeatherDateAndGrid(
        @Param("weatherDate") weatherDate: LocalDate,
        @Param("nx") nx: Int,
        @Param("ny") ny: Int
    ): WeatherInfo?

    /**
     * 실시간 데이터가 있는 날씨 정보 조회
     */
    @Query(
        """
        select w from WeatherInfo w 
        where w.weatherDate = :weatherDate
        and w.nx = :nx AND w.ny = :ny
        and w.hasCurrentData = true
        """
    )
    fun findByWeatherDateAndGridWithCurrentData(
        @Param("weatherDate") weatherDate: LocalDate,
        @Param("nx") nx: Int,
        @Param("ny") ny: Int
    ): WeatherInfo?

    /**
     * 사용자의 모든 위치에 대한 오늘 날씨 조회
     */
    @Query(
        """
        select w from WeatherInfo w 
        where w.userId = :userId
        and w.weatherDate = :weatherDate
        """
    )
    fun findByUserIdAndWeatherDate(
        @Param("userId") userId: Long,
        @Param("weatherDate") weatherDate: LocalDate
    ): List<WeatherInfo>

    /**
     * 경보가 필요한 날씨 정보 조회
     */
    @Query(
        """
        select w from WeatherInfo w 
        where w.weatherDate = :weatherDate
        and (w.precipitationProbability >= 70 
             or w.currentWindSpeed >= 15.0 
             or w.windSpeed >= 15.0
             or w.currentTemperature <= 0 
             or w.currentTemperature >= 35
             or w.temperature <= 0 
             or w.temperature >= 35
             or w.lightning > 0
             or w.uvIndex >= 8)
        """
    )
    fun findWeatherAlertsForDate(
        @Param("weatherDate") weatherDate: LocalDate
    ): List<WeatherInfo>

    /**
     * 특정 기간 동안의 날씨 데이터 조회
     */
    fun findByWeatherDateBetweenAndNxAndNyOrderByWeatherDate(
        startDate: LocalDate,
        endDate: LocalDate,
        nx: Int,
        ny: Int
    ): List<WeatherInfo>
}
