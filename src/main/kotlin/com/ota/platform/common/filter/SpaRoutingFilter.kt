package com.ota.platform.common.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

// Next.js 정적 빌드를 Spring Boot가 서빙할 때 SPA 라우팅 처리.
// 브라우저 새로고침 시 /admin/bookings 같은 프론트 경로로 직접 요청이 오면
// Spring MVC 핸들러 매핑에 도달하기 전에 해당 index.html로 포워딩한다.
//
// 포워딩 제외 조건 (filterChain 그대로 통과):
//   - 이미 forward된 요청 (jakarta.servlet.forward.request_uri 속성 존재)
//   - /api/ 로 시작하는 API 요청
//   - /api-docs, /swagger-ui, /actuator 등 서버 경로
//   - 점(.)이 포함된 파일 요청 (*.js, *.css, *.html 등)
@Component
@Order(1)
class SpaRoutingFilter : OncePerRequestFilter() {

    private val serverPaths = listOf("/api/", "/api-docs", "/swagger-ui", "/actuator")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val path = request.requestURI
        val isForward = request.getAttribute("jakarta.servlet.forward.request_uri") != null
        val isServerPath = serverPaths.any { path.startsWith(it) }
        val hasFileExtension = path.substringAfterLast('/').contains('.')

        if (!isForward && !isServerPath && !hasFileExtension) {
            val targetPage = when {
                path.startsWith("/admin") -> "/admin/index.html"
                path.startsWith("/extranet") -> "/extranet/index.html"
                else -> "/index.html"
            }
            request.getRequestDispatcher(targetPage).forward(request, response)
            return
        }

        filterChain.doFilter(request, response)
    }
}
