package com.weather.alarm.domain.exception

abstract class DomainException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class DuplicateDataException(
    message: String,
    cause: Throwable? = null
) : DomainException(message, cause)

class InvalidRequestException(
    message: String,
    cause: Throwable? = null
) : DomainException(message, cause)

class DataNotFoundException(
    message: String,
    cause: Throwable? = null
) : DomainException(message, cause)

class BusinessLogicException(
    message: String,
    cause: Throwable? = null
) : DomainException(message, cause)
