package com.ota.platform.common.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * 요청/응답 로깅 필터.
 *
 * - 모든 요청에 traceId를 부여하여 MDC에 저장 (로그 추적용)
 * - 요청 시작/종료 시 메서드, URI, 상태 코드, 처리 시간을 로깅
 * - Swagger, actuator 등 정적 경로는 제외
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val TRACE_ID_HEADER = "X-Trace-Id"
        const val MDC_TRACE_ID = "traceId"

        private val EXCLUDE_PATHS = listOf(
            "/swagger-ui", "/api-docs", "/actuator", "/favicon.ico",
        )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val traceId = request.getHeader(TRACE_ID_HEADER)
            ?: UUID.randomUUID().toString().replace("-", "").take(16)

        MDC.put(MDC_TRACE_ID, traceId)
        response.setHeader(TRACE_ID_HEADER, traceId)

        val uri = request.requestURI
        val method = request.method
        val startTime = System.currentTimeMillis()

        try {
            if (!isExcluded(uri)) {
                val queryString = request.queryString?.let { "?$it" } ?: ""
                log.info("→ {} {}{}", method, uri, queryString)
            }

            filterChain.doFilter(request, response)
        } finally {
            if (!isExcluded(uri)) {
                val elapsed = System.currentTimeMillis() - startTime
                val status = response.status
                val level = when {
                    elapsed >= 3000 -> "SLOW"
                    status >= 500 -> "ERROR"
                    status >= 400 -> "WARN"
                    else -> "OK"
                }
                log.info("← {} {} {} {}ms [{}]", method, uri, status, elapsed, level)
            }
            MDC.clear()
        }
    }

    private fun isExcluded(uri: String): Boolean =
        EXCLUDE_PATHS.any { uri.startsWith(it) }
}
