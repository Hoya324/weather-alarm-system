package com.weather.alarm.domain.user.application

import com.weather.alarm.domain.exception.DataNotFoundException
import com.weather.alarm.domain.user.entity.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FindVerifiedUserByIdDomainService(
    private val findUserByIdDomainService: FindUserByIdDomainService
) {

    @Transactional(readOnly = true)
    fun find(id: Long): User {
        val user = findUserByIdDomainService.find(id)

        if (!user.verified) {
            throw DataNotFoundException("인증되지 않은 사용자입니다. 먼저 인증을 완료해주세요.")
        }

        return user
    }
}