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
}
