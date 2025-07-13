package com.weather.alarm.domain.user.repository

import com.weather.alarm.domain.notification.entity.NotificationInfo
import com.weather.alarm.domain.user.entity.User
import com.weather.alarm.domain.user.type.UserStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.LocalTime

@Repository
interface UserRepository : JpaRepository<User, Long> {

    @Query(
        """
        select distinct u 
        from User u 
        join u.notificationInfos n 
        where n.slackWebHookUrl = :slackWebhookUrl
        and u.status = :status
    """
    )
    fun findBySlackWebhookUrl(
        @Param("slackWebhookUrl") slackWebhookUrl: String,
        @Param("status") status: UserStatus = UserStatus.ACTIVE
    ): User?

    /**
     * 인증 슬랙 URL로 사용자 조회
     */
    fun findByAuthSlackUrl(authSlackUrl: String): User?

    /**
     * 인증 코드로 사용자 조회
     */
    fun findByAuthCode(authCode: String): User?

    /**
     * 이름으로 사용자 조회
     */
    fun findByName(name: String): User?

    /**
     * 이름과 인증코드로 사용자 조회
     */
    fun findByNameAndAuthCode(name: String, authCode: String): User?

    /**
     * 만료된 인증 코드를 가진 사용자들 조회
     */
    @Query("SELECT u FROM User u WHERE u.authCodeExpiresAt < :now AND u.verified = false")
    fun findUsersWithExpiredAuthCode(@Param("now") now: LocalDateTime): List<User>

    /**
     * 활성 알림 사용자들의 NotificationInfo 조회
     */
    @Query(
        """
        SELECT n 
        FROM NotificationInfo n 
        JOIN n.user u 
        WHERE u.status = :status 
        AND u.verified = true 
        AND n.notificationEnabled = true
    """
    )
    fun findActiveNotificationUsers(
        @Param("status") status: UserStatus = UserStatus.ACTIVE
    ): List<NotificationInfo>

    /**
     * 특정 시간에 알림 받을 사용자들 조회
     */
    @Query(
        """
        SELECT n 
        FROM NotificationInfo n 
        JOIN n.user u 
        WHERE u.status = :status 
        AND u.verified = true 
        AND n.notificationEnabled = true 
        AND n.notificationTime = :time
    """
    )
    fun findNotificationUsersByTime(
        @Param("time") time: LocalTime,
        @Param("status") status: UserStatus = UserStatus.ACTIVE
    ): List<NotificationInfo>

    /**
     * 특정 지역 범위 내 사용자들 조회
     */
    @Query(
        """
        SELECT n 
        FROM NotificationInfo n 
        JOIN n.user u 
        WHERE u.status = :status 
        AND u.verified = true 
        AND n.notificationEnabled = true 
        AND n.coordinate.latitude BETWEEN :minLat AND :maxLat 
        AND n.coordinate.longitude BETWEEN :minLon AND :maxLon
    """
    )
    fun findNotificationUsersByLocationRange(
        @Param("minLat") minLatitude: Double,
        @Param("maxLat") maxLatitude: Double,
        @Param("minLon") minLongitude: Double,
        @Param("maxLon") maxLongitude: Double,
        @Param("status") status: UserStatus = UserStatus.ACTIVE
    ): List<NotificationInfo>
}
