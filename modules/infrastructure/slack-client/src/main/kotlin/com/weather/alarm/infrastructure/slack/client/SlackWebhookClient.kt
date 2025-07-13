package com.weather.alarm.infrastructure.slack.client

import com.weather.alarm.infrastructure.slack.dto.SlackMessage
import com.weather.alarm.infrastructure.slack.dto.SlackWebhookRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Duration

@Component
class SlackWebhookClient(
    private val webClient: WebClient = WebClient.builder()
        .codecs { configurer ->
            configurer.defaultCodecs().maxInMemorySize(1024 * 1024) // 1MB
        }
        .build()
) {
    private val logger = LoggerFactory.getLogger(SlackWebhookClient::class.java)

    fun sendMessage(webhookUrl: String, message: SlackMessage): Mono<Boolean> {
        return webClient.post()
            .uri(webhookUrl)
            .bodyValue(SlackWebhookRequest.from(message))
            .retrieve()
            .toBodilessEntity()
            .timeout(Duration.ofSeconds(10))
            .map { response ->
                val success = response.statusCode.is2xxSuccessful
                if (success) {
                    logger.debug("Slack 메시지 전송 성공: ${message.text}")
                } else {
                    logger.warn("Slack 메시지 전송 실패: ${response.statusCode}")
                }
                success
            }
            .onErrorResume { throwable ->
                when (throwable) {
                    is WebClientResponseException -> {
                        logger.error("Slack API 응답 오류: ${throwable.statusCode} - ${throwable.responseBodyAsString}")
                    }

                    else -> {
                        logger.error("Slack 메시지 전송 중 오류 발생", throwable)
                    }
                }
                Mono.just(false)
            }
    }

    fun validateWebhookUrl(webhookUrl: String): Boolean {
        return webhookUrl.startsWith("https://hooks.slack.com/services/") &&
                webhookUrl.length > 50
    }
}
