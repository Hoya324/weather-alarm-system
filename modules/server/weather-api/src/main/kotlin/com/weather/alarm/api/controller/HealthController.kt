package com.weather.alarm.api.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
class HealthController {

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, Any>> {
        val response = mapOf(
            "status" to "UP",
            "timestamp" to LocalDateTime.now(),
            "service" to "weather-alarm-api"
        )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/actuator/health")
    fun actuatorHealth(): ResponseEntity<Map<String, Any>> {
        val response = mapOf(
            "status" to "UP",
            "components" to mapOf(
                "db" to mapOf("status" to "UP"),
                "diskSpace" to mapOf("status" to "UP")
            )
        )
        return ResponseEntity.ok(response)
    }
}
