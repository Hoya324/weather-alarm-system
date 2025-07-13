package com.weather.alarm.domain.user.application

import com.weather.alarm.domain.exception.DataNotFoundException
import com.weather.alarm.domain.user.entity.User
import com.weather.alarm.domain.user.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FindUserByIdDomainService(
    private val userRepository: UserRepository
) {

    @Transactional(readOnly = true)
    fun find(id: Long): User {
        return userRepository.findByIdOrNull(id)
            ?: throw DataNotFoundException("사용자를 찾을 수 없습니다. ID: $id")
    }
}