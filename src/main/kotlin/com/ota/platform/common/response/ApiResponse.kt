package com.ota.platform.common.response

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorDetail? = null,
) {
    companion object {
        fun <T> ok(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)

        fun <T> ok(): ApiResponse<T> = ApiResponse(success = true)

        fun <T> fail(code: String, message: String): ApiResponse<T> =
            ApiResponse(success = false, error = ErrorDetail(code, message))
    }
}

data class ErrorDetail(
    val code: String,
    val message: String,
)
