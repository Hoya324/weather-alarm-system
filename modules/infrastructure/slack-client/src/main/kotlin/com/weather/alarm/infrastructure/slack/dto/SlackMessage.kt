package com.weather.alarm.infrastructure.slack.dto

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class SlackMessage(
    val text: String,
    val username: String? = null,
    val iconEmoji: String? = null,
    val attachments: List<SlackAttachment>? = null,
    val blocks: List<SlackBlock>? = null
) {
    companion object {
        fun simple(text: String): SlackMessage {
            return SlackMessage(
                text = text,
                username = "날씨 알림봇",
                iconEmoji = ":cloud:"
            )
        }

        fun authCode(userName: String, authCode: String): SlackMessage {
            val currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

            return SlackMessage(
                text = "🔐 날씨 알림 서비스 인증 코드",
                username = "날씨 알림봇",
                iconEmoji = ":key:",
                attachments = listOf(
                    SlackAttachment(
                        color = "good",
                        fields = listOf(
                            SlackField("사용자", userName, true),
                            SlackField("인증 코드", "`$authCode`", true),
                            SlackField("발급 시간", currentTime, true),
                            SlackField("유효 기간", "1시간", true)
                        ),
                        footer = "날씨 알림 서비스",
                        text = "위 인증 코드를 사용하여 로그인하세요.\n인증 코드는 1시간 후 만료됩니다."
                    )
                )
            )
        }

        fun weatherAlert(
            userName: String,
            location: String,
            weather: String,
            temperature: String
        ): SlackMessage {
            val currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            val emoji = getWeatherEmoji(weather)

            return SlackMessage(
                text = "$emoji 날씨 알림",
                username = "날씨 알림봇",
                iconEmoji = ":partly_sunny:",
                attachments = listOf(
                    SlackAttachment(
                        color = getWeatherColor(weather),
                        fields = listOf(
                            SlackField("지역", location, true),
                            SlackField("날씨", "$emoji $weather", true),
                            SlackField("기온", "$temperature°C", true),
                            SlackField("시간", currentTime, true)
                        ),
                        footer = "날씨 알림 서비스",
                        text = "$userName 님, 오늘의 날씨를 확인하세요!"
                    )
                )
            )
        }

        fun error(message: String): SlackMessage {
            return SlackMessage(
                text = "⚠️ 오류 발생",
                username = "날씨 알림봇",
                iconEmoji = ":warning:",
                attachments = listOf(
                    SlackAttachment(
                        color = "danger",
                        text = message,
                        footer = "날씨 알림 서비스"
                    )
                )
            )
        }

        private fun getWeatherEmoji(weather: String): String {
            return when {
                weather.contains("맑음") -> "☀️"
                weather.contains("구름") -> "☁️"
                weather.contains("흐림") -> "☁️"
                weather.contains("비") -> "🌧️"
                weather.contains("눈") -> "❄️"
                weather.contains("소나기") -> "⛈️"
                else -> "🌤️"
            }
        }

        private fun getWeatherColor(weather: String): String {
            return when {
                weather.contains("맑음") -> "good"
                weather.contains("구름") -> "warning"
                weather.contains("흐림") -> "#808080"
                weather.contains("비") -> "#4169E1"
                weather.contains("눈") -> "#E6E6FA"
                weather.contains("소나기") -> "danger"
                else -> "#36a64f"
            }
        }
    }
}
