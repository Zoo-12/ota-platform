package com.ota.platform.common.response

import org.slf4j.MDC

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorDetail? = null,
) {
    companion object {
        fun <T> ok(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)

        fun <T> ok(): ApiResponse<T> = ApiResponse(success = true)

        fun <T> fail(code: String, message: String): ApiResponse<T> =
            ApiResponse(success = false, error = ErrorDetail(code, message, MDC.get("traceId")))
    }
}

data class ErrorDetail(
    val code: String,
    val message: String,
    val traceId: String? = null,
)
