package com.weather.alarm.domain.user.application

import com.weather.alarm.domain.user.entity.User
import com.weather.alarm.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FindUserOrNullBySlackWebhookUrlDomainService(
    private val userRepository: UserRepository
) {

    @Transactional(readOnly = true)
    fun find(authSlackUrl: String): User? {
        return userRepository.findByAuthSlackUrl(authSlackUrl)
    }
}