package com.weather.alarm.infrastructure.slack.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class SlackWebhookRequest(
    val text: String,
    val username: String? = null,
    @JsonProperty("icon_emoji")
    val iconEmoji: String? = null,
    val attachments: List<SlackAttachment>? = null,
    val blocks: List<SlackBlock>? = null
) {
    companion object {
        fun from(message: SlackMessage): SlackWebhookRequest {
            return SlackWebhookRequest(
                text = message.text,
                username = message.username,
                iconEmoji = message.iconEmoji,
                attachments = message.attachments,
                blocks = message.blocks
            )
        }
    }
}

