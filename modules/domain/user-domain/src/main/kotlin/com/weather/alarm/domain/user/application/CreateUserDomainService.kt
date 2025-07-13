package com.weather.alarm.domain.user.application

import com.weather.alarm.domain.exception.DuplicateDataException
import com.weather.alarm.domain.exception.InvalidRequestException
import com.weather.alarm.domain.notification.application.SendMessageNotificationDomainService
import com.weather.alarm.domain.user.dto.request.CreateUserDomainRequest
import com.weather.alarm.domain.user.dto.response.CreateUserDomainResult
import com.weather.alarm.domain.user.entity.User
import com.weather.alarm.domain.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateUserDomainService(
    private val userRepository: UserRepository,
    private val authCodeDomainService: AuthCodeDomainService,
    private val sendMessageNotificationDomainService: SendMessageNotificationDomainService
) {
    private val logger = LoggerFactory.getLogger(CreateUserDomainService::class.java)

    @Transactional
    fun createUser(request: CreateUserDomainRequest): CreateUserDomainResult {
        logger.info("사용자 생성 시작 - 이름: ${request.name}")

        // 유효성 검증
        validateRequest(request)

        // 중복 검증
        validateDuplicates(request)

        // 웹훅 URL 유효성 검증
        validateWebhookUrl(request.authSlackUrl)

        // 인증 코드 생성
        val authCode = authCodeDomainService.generateAuthCode()

        // 사용자 생성
        val user = User(
            _name = request.name,
            _authSlackUrl = request.authSlackUrl,
            _authCode = authCode
        )

        val savedUser = userRepository.save(user)
        logger.info("사용자 생성 완료 - ID: ${savedUser.id}, 이름: ${savedUser.name}")

        sendNotifications(savedUser)

        return CreateUserDomainResult(savedUser, authCode)
    }

    private fun validateRequest(request: CreateUserDomainRequest) {
        val errors = request.validate()
        if (errors.isNotEmpty()) {
            throw InvalidRequestException("입력 데이터가 유효하지 않습니다: ${errors.joinToString(", ")}")
        }
    }

    private fun validateDuplicates(request: CreateUserDomainRequest) {
        userRepository.findByAuthSlackUrl(request.authSlackUrl)?.let {
            throw DuplicateDataException("해당 Slack 웹훅 URL은 이미 등록되어 있습니다.")
        }

        userRepository.findByName(request.name)?.let {
            throw DuplicateDataException("해당 이름은 이미 사용 중입니다.")
        }
    }

    private fun validateWebhookUrl(webhookUrl: String) {
        if (!sendMessageNotificationDomainService.validateWebhookUrl(webhookUrl)) {
            throw InvalidRequestException("유효하지 않은 Slack 웹훅 URL입니다.")
        }
    }

    private fun sendNotifications(user: User) {
        try {
            sendMessageNotificationDomainService.sendAuthCodeAsync(user)

            logger.info("알림 전송 요청 완료 - 사용자: ${user.name}")
        } catch (e: Exception) {
            logger.error("알림 전송 중 오류 발생 - 사용자: ${user.name}", e)
        }
    }
}
