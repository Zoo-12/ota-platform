package com.ota.platform.property.api

import com.ota.platform.common.response.ApiResponse
import com.ota.platform.property.application.PartnerUseCase
import com.ota.platform.property.domain.PartnerStatus
import com.ota.platform.property.infrastructure.PartnerRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin - 파트너")
@RestController
@RequestMapping("/api/admin/partners")
class AdminPartnerController(
    private val partnerRepository: PartnerRepository,
    private val partnerUseCase: PartnerUseCase,
) {

    @Operation(summary = "파트너 목록 조회 (상태 필터)")
    @GetMapping
    fun list(
        @RequestParam(required = false) status: PartnerStatus?,
    ): ApiResponse<List<AdminPartnerResponse>> {
        val partners = if (status != null) {
            partnerRepository.findAllByStatus(status)
        } else {
            partnerRepository.findAll()
        }
        return ApiResponse.ok(partners.map {
            AdminPartnerResponse(it.id, it.name, it.email, it.phone, it.businessNumber, it.status.name)
        })
    }

    @Operation(summary = "파트너 승인 (PENDING → ACTIVE)")
    @PatchMapping("/{partnerId}/approve")
    fun approve(@PathVariable partnerId: Long): ApiResponse<Unit> {
        partnerUseCase.approve(partnerId)
        return ApiResponse.ok()
    }
}

data class AdminPartnerResponse(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String,
    val businessNumber: String,
    val status: String,
)
