package com.weather.alarm.api.auth.controller

import com.weather.alarm.api.auth.application.AuthApiService
import com.weather.alarm.api.auth.dto.request.CreateAuthCodeRequest
import com.weather.alarm.api.auth.dto.request.LoginWithCodeRequest
import com.weather.alarm.api.auth.dto.request.VerifyAuthCodeRequest
import com.weather.alarm.api.auth.dto.response.CreateAuthCodeResponse
import com.weather.alarm.api.auth.dto.response.LoginWithCodeResponse
import com.weather.alarm.api.auth.dto.response.VerifyAuthCodeResponse
import com.weather.alarm.api.common.dto.response.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authApiService: AuthApiService
) {

    @PostMapping("/login")
    fun loginWithCode(
        @Valid @RequestBody request: LoginWithCodeRequest
    ): ResponseEntity<ApiResponse<LoginWithCodeResponse>> {
        val response = authApiService.loginWithCode(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/code")
    fun createAuthCode(
        @Valid @RequestBody request: CreateAuthCodeRequest
    ): ResponseEntity<ApiResponse<CreateAuthCodeResponse>> {
        val response = authApiService.createAuthCode(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/verify")
    fun verifyAuthCode(
        @Valid @RequestBody request: VerifyAuthCodeRequest
    ): ResponseEntity<ApiResponse<VerifyAuthCodeResponse>> {
        val response = authApiService.verifyAuthCode(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
