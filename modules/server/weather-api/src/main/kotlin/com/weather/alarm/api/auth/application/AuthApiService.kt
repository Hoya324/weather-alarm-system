package com.weather.alarm.api.auth.application

import com.weather.alarm.api.auth.dto.request.CreateAuthCodeRequest
import com.weather.alarm.api.auth.dto.request.LoginWithCodeRequest
import com.weather.alarm.api.auth.dto.request.VerifyAuthCodeRequest
import com.weather.alarm.api.auth.dto.response.CreateAuthCodeResponse
import com.weather.alarm.api.auth.dto.response.LoginWithCodeResponse
import com.weather.alarm.api.auth.dto.response.VerifyAuthCodeResponse
import com.weather.alarm.domain.notification.application.SendAuthCodeDomainService
import com.weather.alarm.domain.notification.application.SendMessageNotificationDomainService
import com.weather.alarm.domain.user.application.AuthCodeDomainService
import com.weather.alarm.domain.user.application.CreateUserDomainService
import com.weather.alarm.domain.user.application.FindUserOrNullBySlackWebhookUrlDomainService
import com.weather.alarm.domain.user.application.LoginWithCodeDomainService
import com.weather.alarm.domain.user.dto.request.CreateUserDomainRequest
import com.weather.alarm.domain.user.entity.User
import com.weather.alarm.domain.user.type.LoginValidationResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

@Service
class AuthApiService(
    private val findUserOrNullBySlackWebhookUrlDomainService: FindUserOrNullBySlackWebhookUrlDomainService,
    private val createUserDomainService: CreateUserDomainService,
    private val authCodeDomainService: AuthCodeDomainService,
    private val sendAuthCodeDomainService: SendAuthCodeDomainService,
    private val sendMessageNotificationDomainService: SendMessageNotificationDomainService,
    private val loginWithCodeDomainService: LoginWithCodeDomainService
) {

    @Transactional(readOnly = true)
    fun loginWithCode(request: LoginWithCodeRequest): LoginWithCodeResponse {
        val user = loginWithCodeDomainService.findUserByNameAndAuthCode(request.name, request.authCode)
            ?: return LoginWithCodeResponse.failure("이름 또는 인증코드가 올바르지 않습니다.")

        // 인증코드 유효성 검증
        when (loginWithCodeDomainService.validateUserLogin(user)) {
            LoginValidationResult.EXPIRED -> {
                return LoginWithCodeResponse.failure("인증코드가 만료되었습니다. 새로운 인증코드를 요청해주세요.")
            }

            LoginValidationResult.VALID -> {
                return LoginWithCodeResponse.success(
                    userId = user.id,
                    userName = user.name,
                    authSlackUrl = user.authSlackUrl,
                    isVerified = user.verified
                )
            }
        }
    }

    @Transactional
    fun createAuthCode(request: CreateAuthCodeRequest): CreateAuthCodeResponse {
        val existingUser = findUserOrNullBySlackWebhookUrlDomainService.find(request.authSlackUrl)

        val user = createOrRefreshAuthCode(existingUser, request)

        // 기존 사용자의 경우에만 인증 코드 전송 (신규 사용자는 createUser에서 이미 전송됨)
        if (existingUser != null) {
            sendAuthCodeDomainService.sendAuthCode(user.authSlackUrl, user.authCode)
        }

        return CreateAuthCodeResponse(
            message = "인증 코드가 Slack으로 전송되었습니다.",
            authSlackUrl = user.authSlackUrl,
            authCodeExpiresAt = user.authCodeExpiresAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        )
    }

    @Transactional
    fun verifyAuthCode(request: VerifyAuthCodeRequest): VerifyAuthCodeResponse {
        val user = findUserOrNullBySlackWebhookUrlDomainService.find(request.authSlackUrl)
            ?: return VerifyAuthCodeResponse(
                success = false,
                message = "등록되지 않은 Slack URL입니다."
            )

        if (user.isAuthCodeExpired()) {
            return VerifyAuthCodeResponse(
                success = false,
                message = "인증 코드가 만료되었습니다. 새로운 인증 코드를 요청해주세요."
            )
        }

        if (!user.isAuthCodeValid(request.authCode)) {
            return VerifyAuthCodeResponse(
                success = false,
                message = "인증 코드가 올바르지 않습니다."
            )
        }

        user.verify()

        try {
            sendMessageNotificationDomainService.sendWelcomeMessageAsync(user)
        } catch (e: Exception) {
        }

        return VerifyAuthCodeResponse(
            success = true,
            message = "인증이 완료되었습니다.",
            userId = user.id
        )
    }

    private fun createOrRefreshAuthCode(
        existingUser: User?,
        request: CreateAuthCodeRequest
    ) = if (existingUser != null) {
        val newAuthCode = authCodeDomainService.generateAuthCode()
        existingUser.refreshAuthCode(newAuthCode)
        existingUser
    } else {
        val createUserDomainRequest = CreateUserDomainRequest(
            name = request.name,
            authSlackUrl = request.authSlackUrl
        )
        val userResult = createUserDomainService.createUser(createUserDomainRequest)
        userResult.user
    }
}
