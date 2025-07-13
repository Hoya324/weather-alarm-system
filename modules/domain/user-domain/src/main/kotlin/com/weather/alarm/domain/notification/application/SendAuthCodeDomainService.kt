package com.weather.alarm.domain.notification.application

import com.weather.alarm.domain.port.out.NotificationPort
import com.weather.alarm.domain.port.out.dto.NotificationRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SendAuthCodeDomainService(
    private val notificationPort: NotificationPort
) {
    private val logger = LoggerFactory.getLogger(SendAuthCodeDomainService::class.java)

    /**
     * 인증 코드를 Slack으로 전송
     */
    fun sendAuthCode(authSlackUrl: String, authCode: String): Boolean {
        return try {
            val success = notificationPort.send(
                NotificationRequest(
                    target = authSlackUrl,
                    message = "🔑 [인증 코드] $authCode\n\n1시간 이내에 입력해주세요.",
                    title = "날씨 알림 시스템 - 인증 코드"
                )
            )

            if (success) {
                logger.info("인증 코드 전송 성공 - URL: ${authSlackUrl.substring(0, 30)}...")
            } else {
                logger.warn("인증 코드 전송 실패 - URL: ${authSlackUrl.substring(0, 30)}...")
            }

            success
        } catch (e: Exception) {
            logger.error("인증 코드 전송 중 오류 발생", e)
            false
        }
    }
}