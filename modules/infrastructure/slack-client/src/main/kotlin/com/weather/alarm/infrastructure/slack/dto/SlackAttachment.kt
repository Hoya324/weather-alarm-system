package com.weather.alarm.infrastructure.slack.dto

data class SlackAttachment(
    val color: String? = null,
    val text: String? = null,
    val fields: List<SlackField>? = null,
    val footer: String? = null,
    val ts: Long? = null
)

