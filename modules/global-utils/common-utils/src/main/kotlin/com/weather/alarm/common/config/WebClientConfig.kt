package com.weather.alarm.common.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

@Configuration
class WebClientConfig {

    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
    }

    @Bean
    fun webClient(objectMapper: ObjectMapper): WebClient {
        val exchangeStrategies = ExchangeStrategies.builder()
            .codecs { codecs ->
                codecs.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper))
                codecs.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper))
                codecs.defaultCodecs().maxInMemorySize(1024 * 1024) // 1MB
            }
            .build()

        return WebClient.builder()
            .exchangeStrategies(exchangeStrategies)
            .build()
    }
}
