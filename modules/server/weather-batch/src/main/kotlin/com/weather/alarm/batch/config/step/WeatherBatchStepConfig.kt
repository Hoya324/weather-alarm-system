package com.weather.alarm.batch.config.step

import com.weather.alarm.batch.core.chunk.processor.NotificationItemProcessor
import com.weather.alarm.batch.core.chunk.reader.NotificationItemReader
import com.weather.alarm.batch.core.chunk.writer.NotificationItemWriter
import com.weather.alarm.batch.core.tasklet.WeatherDataFetchTasklet
import com.weather.alarm.batch.domain.dto.NotificationItem
import com.weather.alarm.batch.domain.dto.ProcessedNotificationItem
import org.springframework.batch.core.Step
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class WeatherBatchStepConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val weatherDataFetchTasklet: WeatherDataFetchTasklet,
    private val notificationItemReader: NotificationItemReader,
    private val notificationItemProcessor: NotificationItemProcessor,
    private val notificationItemWriter: NotificationItemWriter
) {

    @Value("\${batch.notification.chunk-size:10}")
    private val notificationChunkSize: Int = 10

    /**
     * 날씨 데이터 수집 Step
     * fetchType JobParameter에 따라 수집 모드 결정
     * - CURRENT: 초단기실황
     * - FORECAST: 단기예보
     * - COMPREHENSIVE: 모든 API 종합
     */
    @Bean
    fun weatherDataFetchStep(): Step {
        return StepBuilder("weatherDataFetchStep", jobRepository)
            .tasklet(weatherDataFetchTasklet, transactionManager)
            .build()
    }

    /**
     * 알림 발송 Step
     * Chunk 기반으로 대량 알림 처리
     * - Reader: 알림 대상 조회
     * - Processor: 메시지 생성 및 조건 확인
     * - Writer: Slack 웹훅 발송
     */
    @Bean
    fun sendNotificationStep(): Step {
        return StepBuilder("sendNotificationStep", jobRepository)
            .chunk<NotificationItem, ProcessedNotificationItem>(notificationChunkSize, transactionManager)
            .reader(notificationItemReader)
            .processor(notificationItemProcessor)
            .writer(notificationItemWriter)
            .build()
    }
}