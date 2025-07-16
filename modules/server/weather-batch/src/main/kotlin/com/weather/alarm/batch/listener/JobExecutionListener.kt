package com.weather.alarm.batch.listener

import org.slf4j.LoggerFactory
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.stereotype.Component

@Component
class WeatherBatchJobExecutionListener : JobExecutionListener {

    private val logger = LoggerFactory.getLogger(WeatherBatchJobExecutionListener::class.java)

    override fun beforeJob(jobExecution: JobExecution) {
        logger.info("=== 배치 작업 시작 ===")
        logger.info("Job Name: ${jobExecution.jobInstance.jobName}")
        logger.info("Job Parameters: ${jobExecution.jobParameters}")
        logger.info("Job Start Time: ${jobExecution.startTime}")
    }

    override fun afterJob(jobExecution: JobExecution) {
        logger.info("=== 배치 작업 완료 ===")
        logger.info("Job Name: ${jobExecution.jobInstance.jobName}")
        logger.info("Job Status: ${jobExecution.status}")
        logger.info("Job End Time: ${jobExecution.endTime}")

        if (jobExecution.allFailureExceptions.isNotEmpty()) {
            logger.error("Job failed with exceptions:")
            jobExecution.allFailureExceptions.forEach { exception ->
                logger.error("Exception: ${exception.message}", exception)
            }
        }

        jobExecution.stepExecutions.forEach { stepExecution ->
            logger.info("Step: ${stepExecution.stepName}")
            logger.info("  - Status: ${stepExecution.status}")
            logger.info("  - Read Count: ${stepExecution.readCount}")
            logger.info("  - Write Count: ${stepExecution.writeCount}")
            logger.info("  - Skip Count: ${stepExecution.skipCount}")
            if (stepExecution.failureExceptions.isNotEmpty()) {
                logger.error("  - Step Failures: ${stepExecution.failureExceptions.size}")
            }
        }
    }
}
