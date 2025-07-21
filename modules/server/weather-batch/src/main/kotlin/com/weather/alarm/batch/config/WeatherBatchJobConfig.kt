package com.weather.alarm.batch.config

import com.weather.alarm.batch.data.tasklet.WeatherDataFetchTasklet
import com.weather.alarm.batch.dto.NotificationItem
import com.weather.alarm.batch.dto.ProcessedNotificationItem
import com.weather.alarm.batch.listener.WeatherBatchJobExecutionListener
import com.weather.alarm.batch.notification.processor.NotificationItemProcessor
import com.weather.alarm.batch.notification.reader.NotificationItemReader
import com.weather.alarm.batch.notification.writer.NotificationItemWriter
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class WeatherBatchJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val weatherDataFetchTasklet: WeatherDataFetchTasklet,
    private val notificationItemReader: NotificationItemReader,
    private val notificationItemProcessor: NotificationItemProcessor,
    private val notificationItemWriter: NotificationItemWriter,
    private val jobExecutionListener: WeatherBatchJobExecutionListener
) {

    @Bean
    fun weatherDataFetchJob(): Job {
        return JobBuilder("weatherDataFetchJob", jobRepository)
            .listener(jobExecutionListener)
            .start(weatherDataFetchStep())
            .build()
    }

    @Bean
    fun weatherDataFetchStep(): Step {
        return StepBuilder("weatherDataFetchStep", jobRepository)
            .tasklet(weatherDataFetchTasklet, transactionManager)
            .build()
    }

    @Bean
    fun notificationSendJob(): Job {
        return JobBuilder("notificationSendJob", jobRepository)
            .listener(jobExecutionListener)
            .start(notificationSendStep())
            .build()
    }

    @Bean
    fun notificationSendStep(): Step {
        return StepBuilder("notificationSendStep", jobRepository)
            .chunk<NotificationItem, ProcessedNotificationItem>(10, transactionManager)
            .reader(notificationItemReader)
            .processor(notificationItemProcessor)
            .writer(notificationItemWriter)
            .build()
    }

    @Bean
    fun weatherProcessCompleteJob(): Job {
        return JobBuilder("weatherProcessCompleteJob", jobRepository)
            .listener(jobExecutionListener)
            .start(weatherDataFetchStep())
            .next(notificationSendStep())
            .build()
    }
}
