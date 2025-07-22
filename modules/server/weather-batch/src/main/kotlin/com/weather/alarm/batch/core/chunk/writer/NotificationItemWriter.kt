package com.weather.alarm.batch.core.chunk.writer

import com.weather.alarm.batch.domain.dto.ProcessedNotificationItem
import com.weather.alarm.domain.port.out.NotificationPort
import com.weather.alarm.domain.port.out.dto.NotificationRequest
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

@Component
class NotificationItemWriter(
    private val notificationPort: NotificationPort
) : ItemWriter<ProcessedNotificationItem> {

    private val logger = LoggerFactory.getLogger(NotificationItemWriter::class.java)

    override fun write(chunk: Chunk<out ProcessedNotificationItem>) {
        logger.info("=== 알림 발송 시작: ${chunk.size()}개 ===")

        var successCount = 0
        var failureCount = 0

        chunk.items.forEach { item ->
            try {
                val notificationRequest = NotificationRequest(
                    target = item.webhookUrl,
                    message = item.message
                )

                notificationPort.send(notificationRequest)
                successCount++

                logger.debug("알림 발송 성공: userId=${item.userId}, notificationId=${item.notificationId}")
            } catch (e: Exception) {
                failureCount++
                logger.error("알림 발송 실패: userId=${item.userId}, notificationId=${item.notificationId}", e)
            }
        }

        logger.info("=== 알림 발송 완료: 성공 ${successCount}개, 실패 ${failureCount}개 ===")
    }
}
