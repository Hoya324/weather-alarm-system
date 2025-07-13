package com.weather.alarm.api.common.exception

import com.weather.alarm.api.common.dto.response.ApiResponse
import com.weather.alarm.domain.exception.BusinessLogicException
import com.weather.alarm.domain.exception.DataNotFoundException
import com.weather.alarm.domain.exception.DuplicateDataException
import com.weather.alarm.domain.exception.InvalidRequestException
import com.weather.alarm.domain.user.exception.UserDomainException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(UserDomainException::class)
    fun handleUserDomainException(e: UserDomainException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("UserDomainException occurred: {}", e.message)
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(e.message ?: "사용자 도메인 오류가 발생했습니다.", "USER_DOMAIN_ERROR"))
    }

    @ExceptionHandler(DataNotFoundException::class)
    fun handleDataNotFoundException(e: DataNotFoundException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("DataNotFoundException occurred: {}", e.message)
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(e.message ?: "데이터를 찾을 수 없습니다.", "DATA_NOT_FOUND"))
    }

    @ExceptionHandler(DuplicateDataException::class)
    fun handleDuplicateDataException(e: DuplicateDataException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("DuplicateDataException occurred: {}", e.message)
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(e.message ?: "중복된 데이터입니다.", "DUPLICATE_DATA"))
    }

    @ExceptionHandler(InvalidRequestException::class)
    fun handleInvalidRequestException(e: InvalidRequestException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("InvalidRequestException occurred: {}", e.message)
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(e.message ?: "잘못된 요청입니다.", "INVALID_REQUEST"))
    }

    @ExceptionHandler(BusinessLogicException::class)
    fun handleBusinessLogicException(e: BusinessLogicException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("BusinessLogicException occurred: {}", e.message)
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(e.message ?: "비즈니스 로직 오류가 발생했습니다.", "BUSINESS_LOGIC_ERROR"))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val errors = e.bindingResult.fieldErrors
            .map { "${it.field}: ${it.defaultMessage}" }
            .joinToString(", ")

        logger.warn("Validation error occurred: {}", errors)
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error("입력값 검증 오류: $errors", "VALIDATION_ERROR"))
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<ApiResponse<Nothing>> {
        val message = "잘못된 형식의 매개변수입니다: ${e.name}"
        logger.warn("Type mismatch error occurred: {}", message)
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(message, "TYPE_MISMATCH_ERROR"))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("IllegalArgumentException occurred: {}", e.message)
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(e.message ?: "잘못된 요청입니다.", "ILLEGAL_ARGUMENT_ERROR"))
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(e: RuntimeException): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("RuntimeException occurred", e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("서버 내부 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Unexpected exception occurred", e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("예상치 못한 오류가 발생했습니다.", "UNEXPECTED_ERROR"))
    }
}
