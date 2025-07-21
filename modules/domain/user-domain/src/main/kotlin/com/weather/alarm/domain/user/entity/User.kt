package com.weather.alarm.domain.user.entity

import com.weather.alarm.domain.notification.entity.NotificationInfo
import com.weather.alarm.domain.user.type.UserStatus
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["auth_code"])
    ]
)
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
        private set

    @Column(nullable = false, length = 100)
    var name: String = _name
        private set

    @Column(name = "auth_slack_url", nullable = false, columnDefinition = "TEXT")
    var authSlackUrl: String = _authSlackUrl
        private set

    @Column(name = "auth_code", nullable = false, length = 10)
    var authCode: String = _authCode
        private set

    @Column(name = "auth_code_expires_at", nullable = false)
    var authCodeExpiresAt: LocalDateTime = _authCodeExpiresAt
        private set

    @Column(nullable = false)
    var verified: Boolean = _verified
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: UserStatus = _status
        private set

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var notificationInfos: MutableList<NotificationInfo> = mutableListOf()
        private set

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

    fun upgradeToReauthCode() {
        this.authCodeExpiresAt = LocalDateTime.now().plusDays(365)
    }
}
