package com.weather.alarm.batch.config.job

import com.weather.alarm.batch.listener.WeatherBatchJobExecutionListener
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.DefaultJobParametersValidator
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.flow.JobExecutionDecider
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WeatherBatchJobConfig(
    private val jobRepository: JobRepository,
    private val jobExecutionListener: WeatherBatchJobExecutionListener,
    @Qualifier("weatherDataFetchStep") private val weatherDataFetchStep: Step,
    @Qualifier("sendNotificationStep") private val sendNotificationStep: Step,
    @Qualifier("notificationDecider") private val notificationDecider: JobExecutionDecider
) {

    /**
     * 날씨 데이터 수집 전용 Job
     * -> 기상청 API에서 날씨 데이터를 수집하여 DB에 저장
     * -> 스케줄러에 의한 정기 데이터 수집
     *
     * 파라미터:
     * - fetchType (필수): CURRENT, FORECAST, COMPREHENSIVE
     * - timestamp (필수): 중복 실행 방지
     */
    @Bean
    fun weatherDataFetchJob(): Job {
        return JobBuilder("weatherDataFetchJob", jobRepository)
            .listener(jobExecutionListener)
            .start(weatherDataFetchStep)
            .validator(
                DefaultJobParametersValidator(
                    arrayOf("fetchType"),
                    arrayOf("timestamp")
                )
            )
            .build()
    }

    /**
     * 알림 발송 전용 Job
     * -> 저장된 날씨 데이터를 기반으로 사용자에게 알림 발송
     * -> 매시간 사용자 설정 시간에 맞춘 알림 발송
     */
    @Bean
    fun notificationSendJob(): Job {
        return JobBuilder("notificationSendJob", jobRepository)
            .listener(jobExecutionListener)
            .start(sendNotificationStep)
            .build()
    }

    /**
     * 통합 날씨 처리 Job
     * -> 데이터 수집과 알림 발송을 연속으로 실행
     * -> 긴급 상황 대응 (데이터 갱신 + 즉시 알림)
     * 파라미터:
     * - fetchType (필수): CURRENT, FORECAST, COMPREHENSIVE
     * - notificationEnabled (선택): true/false, 기본값 true
     * - timestamp (필수): 중복 실행 방지
     */
    @Bean
    fun weatherProcessJob(): Job {
        return JobBuilder("weatherProcessJob", jobRepository)
            .listener(jobExecutionListener)
            .start(weatherDataFetchStep)
            .next(notificationDecider)
            .on("SEND").to(sendNotificationStep)
            .on("SKIP").end()
            .end()
            .validator(
                DefaultJobParametersValidator(
                    arrayOf("fetchType"),
                    arrayOf("notificationEnabled", "timestamp")
                )
            )
            .build()
    }

    /**
     * 실시간 날씨 업데이트 Job
     *
     * 목적: 낮시간대 실시간 날씨 상황 모니터링
     * 파라미터:
     * - fetchType: 자동으로 CURRENT 설정
     * - timestamp (필수): 중복 실행 방지
     *
     * 사용 시나리오:
     * - 낮시간대 30분마다 실시간 업데이트
     * - 기상 특보 모니터링
     */
    @Bean
    fun realTimeWeatherJob(): Job {
        return JobBuilder("realTimeWeatherJob", jobRepository)
            .listener(jobExecutionListener)
            .start(weatherDataFetchStep)
            .build()
    }
}
