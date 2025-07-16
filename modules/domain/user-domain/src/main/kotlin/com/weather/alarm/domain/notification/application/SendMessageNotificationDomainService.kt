package com.weather.alarm.domain.notification.application

import com.weather.alarm.domain.port.out.NotificationPort
import com.weather.alarm.domain.port.out.dto.NotificationRequest
import com.weather.alarm.domain.user.entity.User
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class SendMessageNotificationDomainService(
    private val notificationPort: NotificationPort
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Async("notificationTaskExecutor")
    fun sendAuthCodeAsync(user: User) {
        try {
            val success = notificationPort.send(
                NotificationRequest(
                    target = user.authSlackUrl,
                    message = "🔑 [인증 코드] ${user.authCode}\n\n1시간 이내에 입력해주세요.",
                    title = "날씨 알림 시스템 - 인증 코드"
                )
            )

            if (success) {
                logger.info("인증 코드 전송 성공 - 사용자: ${user.name}")
            } else {
                logger.warn("인증 코드 전송 실패 - 사용자: ${user.name}")
            }
        } catch (e: Exception) {
            logger.error("인증 코드 전송 중 오류 발생 - 사용자: ${user.name}", e)
        }
    }

    @Async("notificationTaskExecutor")
    fun sendWelcomeMessageAsync(user: User) {
        try {
            val success = notificationPort.send(
                NotificationRequest(
                    target = user.authSlackUrl,
                    message = "🌤️ 날씨 알림 시스템에 가입해주셔서 감사합니다!",
                    title = "날씨 알림 시스템 - 환영합니다"
                )
            )

            if (success) {
                logger.info("환영 메시지 전송 성공 - 사용자: ${user.name}")
            } else {
                logger.warn("환영 메시지 전송 실패 - 사용자: ${user.name}")
            }
        } catch (e: Exception) {
            logger.error("환영 메시지 전송 중 오류 발생 - 사용자: ${user.name}", e)
        }
    }

    fun validateWebhookUrl(webhookUrl: String): Boolean {
        return webhookUrl.startsWith("https://hooks.slack.com/services/") && webhookUrl.length > 50
    }
}
