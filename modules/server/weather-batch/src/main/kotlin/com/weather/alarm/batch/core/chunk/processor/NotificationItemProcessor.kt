package com.weather.alarm.batch.core.chunk.processor

import com.weather.alarm.batch.domain.dto.NotificationItem
import com.weather.alarm.batch.domain.dto.ProcessedNotificationItem
import com.weather.alarm.infrastructure.slack.utils.SlackMessageBuilder
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class NotificationItemProcessor(
    private val slackMessageBuilder: SlackMessageBuilder
) : ItemProcessor<NotificationItem, ProcessedNotificationItem> {

    private val logger = LoggerFactory.getLogger(NotificationItemProcessor::class.java)

    override fun process(item: NotificationItem): ProcessedNotificationItem? {
        return try {
            if (!item.shouldSend) {
                logger.debug("알림 조건 불만족으로 건너뜀: userId=${item.notificationInfo.user.id}")
                return null
            }

            val message = slackMessageBuilder.buildWeatherMessage(
                notificationInfo = item.notificationInfo,
                weatherInfo = item.weatherInfo
            )

            val processedItem = ProcessedNotificationItem(
                webhookUrl = item.notificationInfo.slackWebHookUrl,
                message = message,
                userId = item.notificationInfo.user.id,
                notificationId = item.notificationInfo.id
            )

            logger.debug("알림 메시지 생성 완료: userId=${item.notificationInfo.user.id}")
            processedItem

        } catch (e: Exception) {
            logger.error("알림 처리 중 오류: userId=${item.notificationInfo.user.id}", e)
            null
        }
    }
}