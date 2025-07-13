package com.weather.alarm.infrastructure.slack.dto

data class SlackField(
    val title: String,
    val value: String,
    val short: Boolean = false
)
