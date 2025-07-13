package com.weather.alarm.domain.user.application

import com.weather.alarm.domain.user.entity.User
import com.weather.alarm.domain.user.repository.UserRepository
import com.weather.alarm.domain.user.type.LoginValidationResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LoginWithCodeDomainService(
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun findUserByNameAndAuthCode(name: String, authCode: String): User? {
        logger.debug("이름과 인증코드로 사용자 조회 - 이름: $name, 코드: ${authCode.take(3)}***")

        val user = userRepository.findByNameAndAuthCode(name, authCode)

        if (user != null) {
            logger.info("로그인 성공 - 사용자: ${user.name}, ID: ${user.id}, 인증 상태: ${user.verified}")
        } else {
            logger.warn("로그인 실패 - 이름 또는 인증코드가 일치하지 않음: $name")
        }

        return user
    }

    /**
     * 사용자 인증코드 유효성 확인
     */
    fun validateUserLogin(user: User): LoginValidationResult {
        return when {
            user.isAuthCodeExpired() -> {
                logger.warn("로그인 실패 - 인증코드 만료: ${user.name}")
                LoginValidationResult.EXPIRED
            }

            else -> {
                logger.info("로그인 유효성 검증 성공: ${user.name}")
                LoginValidationResult.VALID
            }
        }
    }
}
