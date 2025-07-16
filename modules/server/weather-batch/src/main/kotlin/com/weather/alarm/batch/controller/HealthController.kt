package com.weather.alarm.batch.controller

import org.springframework.batch.core.explore.JobExplorer
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/health")
class HealthController(
    private val jobExplorer: JobExplorer
) {

    @GetMapping
    fun health(): Map<String, Any> {
        return mapOf(
            "status" to "UP",
            "timestamp" to LocalDateTime.now(),
            "service" to "weather-batch",
            "version" to "1.0.0"
        )
    }

    @GetMapping("/batch")
    fun batchHealth(): Map<String, Any> {
        val jobNames = jobExplorer.jobNames
        val jobStatus = mutableMapOf<String, Any>()

        jobNames.forEach { jobName ->
            try {
                val jobInstances = jobExplorer.getJobInstances(jobName, 0, 1)
                if (jobInstances.isNotEmpty()) {
                    val latestExecution = jobExplorer.getJobExecutions(jobInstances[0]).firstOrNull()
                    jobStatus[jobName] = mapOf(
                        "lastExecution" to (latestExecution?.endTime ?: "Never executed"),
                        "status" to (latestExecution?.status?.toString() ?: "UNKNOWN")
                    )
                } else {
                    jobStatus[jobName] = mapOf(
                        "lastExecution" to "Never executed",
                        "status" to "NOT_STARTED"
                    )
                }
            } catch (e: Exception) {
                jobStatus[jobName] = mapOf(
                    "error" to e.message,
                    "status" to "ERROR"
                )
            }
        }

        return mapOf(
            "status" to "UP",
            "timestamp" to LocalDateTime.now(),
            "jobs" to jobStatus
        )
    }
}
