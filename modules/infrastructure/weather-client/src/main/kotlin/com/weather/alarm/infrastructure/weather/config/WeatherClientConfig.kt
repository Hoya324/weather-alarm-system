package com.weather.alarm.infrastructure.weather.config

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration
import java.util.concurrent.TimeUnit

@Configuration
class WeatherClientConfig {

    @Bean
    fun weatherWebClient(): WebClient {
        // Connection Pool 설정
        val connectionProvider = ConnectionProvider.builder("kma-weather-client")
            .maxConnections(50)
            .maxIdleTime(Duration.ofSeconds(30))
            .maxLifeTime(Duration.ofMinutes(5))
            .pendingAcquireTimeout(Duration.ofSeconds(30))
            .evictInBackground(Duration.ofSeconds(120))
            .build()

        // HTTP Client 설정
        val httpClient = HttpClient.create(connectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
            .responseTimeout(Duration.ofSeconds(60))
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(60, TimeUnit.SECONDS))
                    .addHandlerLast(WriteTimeoutHandler(60, TimeUnit.SECONDS))
            }

        // Exchange Strategies 설정 (응답 크기 제한)
        val exchangeStrategies = ExchangeStrategies.builder()
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(1024 * 1024) // 1MB
            }
            .build()

        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(exchangeStrategies)
            .build()
    }
}