package com.weather.alarm.domain.user.application

import com.weather.alarm.domain.user.entity.User
import com.weather.alarm.domain.user.exception.UserDomainException
import com.weather.alarm.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.random.Random

@Service
class AuthCodeDomainService(
    private val userRepository: UserRepository
) {

    /**
     * 6자리 인증 코드 생성
     */
    fun generateAuthCode(): String {
        var authCode: String
        do {
            authCode = Random.nextInt(100000, 999999).toString()
        } while (userRepository.findByAuthCode(authCode) != null)
        return authCode
    }

    /**
     * 인증 코드 검증 및 사용자 인증 처리
     */
    @Transactional
    fun verifyAuthCode(inputCode: String): User {
        val user = userRepository.findByAuthCode(inputCode)
            ?: throw UserDomainException("유효하지 않은 인증 코드입니다.")

        if (user.isAuthCodeExpired()) {
            throw UserDomainException("인증 코드가 만료되었습니다.")
        }

        if (!user.isAuthCodeValid(inputCode)) {
            throw UserDomainException("인증 코드가 일치하지 않습니다.")
        }

        user.verify()
        return userRepository.save(user)
    }
}
