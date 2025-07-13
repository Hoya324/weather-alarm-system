package com.weather.alarm.domain.user.entity

import com.weather.alarm.domain.notification.entity.NotificationInfo
import com.weather.alarm.domain.user.type.UserStatus
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    _id: Long = 0,
    _createdAt: LocalDateTime = LocalDateTime.now(),
    _updatedAt: LocalDateTime = LocalDateTime.now(),
    _name: String,
    _authSlackUrl: String,
    _authCode: String,
    _authCodeExpiresAt: LocalDateTime = LocalDateTime.now().plusHours(1),
    _verified: Boolean = false,
    _status: UserStatus = UserStatus.ACTIVE
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

    @Column(nullable = false, length = 100)
    var name: String = _name

    @Column(name = "auth_slack_url", nullable = false, unique = true, columnDefinition = "text")
    var authSlackUrl: String = _authSlackUrl

    @Column(name = "auth_code", nullable = false, length = 10, unique = true)
    var authCode: String = _authCode

    @Column(name = "auth_code_expires_at", nullable = false)
    var authCodeExpiresAt: LocalDateTime = _authCodeExpiresAt

    @Column(nullable = false)
    var verified: Boolean = _verified

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: UserStatus = _status

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var notificationInfos: MutableList<NotificationInfo> = mutableListOf()

    fun verify() {
        this.verified = true
    }

    fun refreshAuthCode(newAuthCode: String) {
        this.authCode = newAuthCode
        this.authCodeExpiresAt = LocalDateTime.now().plusHours(1)
    }

    fun isAuthCodeExpired(): Boolean {
        return LocalDateTime.now().isAfter(authCodeExpiresAt)
    }

    fun isAuthCodeValid(code: String): Boolean {
        return !isAuthCodeExpired() && this.authCode == code
    }

    fun addNotificationInfo(notificationInfo: NotificationInfo) {
        notificationInfos.add(notificationInfo)
        notificationInfo.user = this
    }

    fun removeNotificationInfo(notificationInfo: NotificationInfo) {
        notificationInfos.remove(notificationInfo)
    }
}
