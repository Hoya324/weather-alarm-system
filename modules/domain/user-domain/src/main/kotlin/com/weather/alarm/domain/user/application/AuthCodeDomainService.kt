package com.weather.alarm.domain.user.application

import com.weather.alarm.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
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
}
