package com.ota.platform.common.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.tags.Tag
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("OTA 숙박 플랫폼 API")
                .description(
                    """
                    가상의 OTA(Online Travel Agency) 숙박 플랫폼 백엔드 API입니다.

                    **이해관계자별 API 그룹**
                    - **Extranet**: 숙소 파트너가 숙소, 객실, 요금제, 재고를 등록 및 관리
                    - **Customer**: 고객이 숙소를 검색하고 예약, 취소
                    - **Admin**: 내부 운영자가 숙소 승인, 예약 모니터링
                    """.trimIndent(),
                )
                .version("1.0.0"),
        )
        .tags(
            listOf(
                Tag().name("Extranet - 파트너").description("파트너 등록 및 조회"),
                Tag().name("Extranet - 숙소").description("숙소 등록 및 관리"),
                Tag().name("Extranet - 객실 타입").description("객실 타입 등록 및 관리"),
                Tag().name("Extranet - 요금제").description("요금 플랜 등록 및 일별 요금 설정"),
                Tag().name("Extranet - 재고").description("날짜별 재고 조회 및 일괄 업데이트"),
                Tag().name("Customer - 숙소").description("숙소 검색, 상세 조회, 요금 조회"),
                Tag().name("Customer - 예약").description("예약 생성, 조회, 취소"),
                Tag().name("Admin - 숙소").description("숙소 목록 조회, 승인, 비활성화"),
                Tag().name("Admin - 예약").description("전체 예약 모니터링"),
            ),
        )
}
