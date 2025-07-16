package com.weather.alarm.domain.notification.entity

import com.weather.alarm.domain.exception.DataNotFoundException
import com.weather.alarm.domain.notification.type.NotificationType
import com.weather.alarm.domain.notification.vo.Coordinate
import com.weather.alarm.domain.user.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "notification_info")
@SQLDelete(sql = "update notification_info set deleted_at = now() where id = ?")
@SQLRestriction("deleted_at IS NULL")
class NotificationInfo(
    _id: Long = 0,
    _createdAt: LocalDateTime = LocalDateTime.now(),
    _updatedAt: LocalDateTime = LocalDateTime.now(),
    _deletedAt: LocalDateTime? = null,
    _slackWebHookUrl: String,
    _address: String,
    _coordinate: Coordinate? = null,
    _notificationEnabled: Boolean = true,
    _notificationTime: LocalTime = LocalTime.of(7, 0),
    _notificationType: NotificationType = NotificationType.DAILY,
    _weatherTypes: String? = null,
    _temperatureThreshold: Int? = null,
    _user: User
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = _id

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = _createdAt

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = _updatedAt

    @Column(name = "deleted_at", nullable = true)
    var deletedAt: LocalDateTime? = _deletedAt

    @Column(name = "slack_webhook_url", nullable = false, columnDefinition = "text")
    var slackWebHookUrl: String = _slackWebHookUrl

    @Column(nullable = false, length = 500)
    var address: String = _address

    @Embedded
    var coordinate: Coordinate? = _coordinate

    @Column(name = "notification_enabled", nullable = false)
    var notificationEnabled: Boolean = _notificationEnabled

    @Column(name = "notification_time", nullable = false)
    var notificationTime: LocalTime = _notificationTime

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 20)
    var notificationType: NotificationType = _notificationType

    @Column(name = "weather_types", length = 500)
    var weatherTypes: String? = _weatherTypes

    @Column(name = "temperature_threshold")
    var temperatureThreshold: Int? = _temperatureThreshold

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = _user

    fun getWeatherTypesList(): List<String> {
        return weatherTypes?.split(",")?.map { it.trim() } ?: emptyList()
    }

    fun getLatitude(): Double? {
        return coordinate?.latitude
    }

    fun getLongitude(): Double? {
        return coordinate?.longitude
    }

    fun update(
        slackWebHookUrl: String? = null,
        address: String? = null,
        notificationEnabled: Boolean? = null,
        notificationTime: LocalTime? = null,
        notificationType: NotificationType? = null,
        weatherTypes: String? = null,
        temperatureThreshold: Int? = null
    ) {
        slackWebHookUrl?.let { this.slackWebHookUrl = slackWebHookUrl }
        address?.let { this.address = address }
        notificationEnabled?.let { this.notificationEnabled = notificationEnabled }
        notificationTime?.let { this.notificationTime = notificationTime }
        notificationType?.let { this.notificationType = notificationType }
        weatherTypes?.let { this.weatherTypes = weatherTypes }
        temperatureThreshold?.let { this.temperatureThreshold = temperatureThreshold }
    }

    fun updateCoordinate(coordinate: Coordinate) {
        this.coordinate = coordinate
    }

    fun delete() {
        if (deletedAt != null) {
            throw DataNotFoundException("이미 삭제된 notificationInfo 입니다.")
        }
        this.user.removeNotificationInfo(this)
        this.deletedAt = LocalDateTime.now()
    }

    fun toggle() {
        this.notificationEnabled = !this.notificationEnabled
    }
}
