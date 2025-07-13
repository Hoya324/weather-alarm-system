package com.weather.alarm.domain.port.out.dto

data class NotificationRequest(
    val target: String,            // e.g., Slack Webhook URL, Email 주소, 전화번호 등
    val title: String? = null,     // 제목 (optional)
    val message: String,           // 메시지 본문
    val meta: Map<String, Any>? = null // 확장 메타데이터 (optional)
)