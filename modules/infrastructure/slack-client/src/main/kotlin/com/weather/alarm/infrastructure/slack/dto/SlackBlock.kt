package com.weather.alarm.infrastructure.slack.dto

data class SlackBlock(
    val type: String,
    val text: SlackText? = null
)
