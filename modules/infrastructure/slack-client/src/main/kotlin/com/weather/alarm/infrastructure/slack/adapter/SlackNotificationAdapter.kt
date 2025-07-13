package com.weather.alarm.infrastructure.slack.adapter

import com.weather.alarm.domain.port.out.NotificationPort
import com.weather.alarm.domain.port.out.dto.NotificationRequest
import com.weather.alarm.infrastructure.slack.client.SlackWebhookClient
import com.weather.alarm.infrastructure.slack.dto.SlackMessage
import org.springframework.stereotype.Component

@Component
class SlackNotificationAdapter(
    private val slackClient: SlackWebhookClient
) : NotificationPort {

    override fun send(notificationRequest: NotificationRequest): Boolean {
        if (!slackClient.validateWebhookUrl(notificationRequest.target)) {
            return false
        }

        val message = SlackMessage.simple(notificationRequest.message)
        return slackClient.sendMessage(notificationRequest.target, message)
            .block() ?: false
    }
}