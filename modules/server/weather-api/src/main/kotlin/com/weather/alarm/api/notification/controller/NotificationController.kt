package com.weather.alarm.api.notification.controller

import com.weather.alarm.api.common.dto.response.ApiResponse
import com.weather.alarm.api.notification.application.CreateNotificationInfoService
import com.weather.alarm.api.notification.application.DeleteNotificationInfoByIdService
import com.weather.alarm.api.notification.application.FindAllNotificationInfosByUserIdService
import com.weather.alarm.api.notification.application.ToggleNotificationEnabledByIdService
import com.weather.alarm.api.notification.application.UpdateNotificationInfoService
import com.weather.alarm.api.notification.dto.request.CreateNotificationInfoRequest
import com.weather.alarm.api.notification.dto.request.UpdateNotificationInfoRequest
import com.weather.alarm.api.notification.dto.response.CreateNotificationInfoResponse
import com.weather.alarm.api.notification.dto.response.NotificationInfoResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val createNotificationInfoService: CreateNotificationInfoService,
    private val findAllNotificationInfosByUserIdService: FindAllNotificationInfosByUserIdService,
    private val updateNotificationInfoService: UpdateNotificationInfoService,
    private val deleteNotificationInfoByIdService: DeleteNotificationInfoByIdService,
    private val toggleNotificationEnabledByIdService: ToggleNotificationEnabledByIdService
) {

    @PostMapping
    fun createNotificationInfo(
        @Valid @RequestBody request: CreateNotificationInfoRequest
    ): ResponseEntity<ApiResponse<CreateNotificationInfoResponse>> {
        val response = createNotificationInfoService.create(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/user/{userId}")
    fun getNotificationInfosByUserId(
        @PathVariable userId: Long
    ): ResponseEntity<ApiResponse<List<NotificationInfoResponse>>> {
        val response = findAllNotificationInfosByUserIdService.find(userId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PutMapping("/{notificationId}")
    fun updateNotificationInfo(
        @PathVariable notificationId: Long,
        @Valid @RequestBody request: UpdateNotificationInfoRequest
    ): ResponseEntity<ApiResponse<NotificationInfoResponse>> {
        val response = updateNotificationInfoService.update(notificationId, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/{notificationId}")
    fun deleteNotificationInfo(
        @PathVariable notificationId: Long
    ): ResponseEntity<ApiResponse<String>> {
        deleteNotificationInfoByIdService.delete(notificationId)
        return ResponseEntity.ok(ApiResponse.success("알림 설정이 삭제되었습니다."))
    }

    @PatchMapping("/{notificationId}/toggle")
    fun toggleNotificationEnabled(
        @PathVariable notificationId: Long
    ): ResponseEntity<ApiResponse<NotificationInfoResponse>> {
        val response = toggleNotificationEnabledByIdService.toggle(notificationId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
