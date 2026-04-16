package com.ota.platform.common.exception

open class OtaException(
    val code: String,
    override val message: String,
) : RuntimeException(message)

class NotFoundException(resource: String, id: Any) :
    OtaException("NOT_FOUND", "$resource not found: $id")

class ConflictException(message: String) :
    OtaException("CONFLICT", message)

class BadRequestException(message: String) :
    OtaException("BAD_REQUEST", message)
