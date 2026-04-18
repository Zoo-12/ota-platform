package com.ota.platform.property.api

import com.ota.platform.common.exception.NotFoundException
import com.ota.platform.common.response.ApiResponse
import com.ota.platform.property.application.PropertyUseCase
import com.ota.platform.property.domain.PropertyStatus
import com.ota.platform.property.infrastructure.PropertyRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin - 숙소")
@RestController
@RequestMapping("/api/admin/properties")
class AdminPropertyController(
    private val propertyRepository: PropertyRepository,
    private val propertyUseCase: PropertyUseCase,
) {
    @Operation(summary = "전체 숙소 목록 조회")
    @GetMapping
    fun list(
        @RequestParam(required = false) status: PropertyStatus?,
        @RequestParam(required = false) city: String?,
    ): ApiResponse<List<PropertySummaryResponse>> {
        val properties = when {
            status != null && city != null -> propertyRepository.findAllByStatusAndAddressCity(status, city)
            status != null -> propertyRepository.findAllByStatus(status)
            else -> propertyRepository.findAll()
        }
        return ApiResponse.ok(properties.map {
            PropertySummaryResponse(it.id, it.name, it.category.name, it.status.name, it.addressCity)
        })
    }

    @Operation(summary = "숙소 상세 조회")
    @GetMapping("/{propertyId}")
    fun get(@PathVariable propertyId: Long): ApiResponse<PropertySummaryResponse> {
        val property = propertyUseCase.getById(propertyId)
        return ApiResponse.ok(
            PropertySummaryResponse(property.id, property.name, property.category.name, property.status.name, property.addressCity)
        )
    }

    @Operation(summary = "숙소 승인 (PENDING_APPROVAL → ACTIVE)")
    @PatchMapping("/{propertyId}/approve")
    fun approve(@PathVariable propertyId: Long): ApiResponse<Unit> {
        val property = propertyRepository.findById(propertyId)
            .orElseThrow { NotFoundException("Property", propertyId) }
        property.approve()
        propertyRepository.save(property)
        return ApiResponse.ok()
    }

    @Operation(summary = "숙소 비활성화 (ACTIVE → INACTIVE)")
    @PatchMapping("/{propertyId}/deactivate")
    fun deactivate(@PathVariable propertyId: Long): ApiResponse<Unit> {
        val property = propertyRepository.findById(propertyId)
            .orElseThrow { NotFoundException("Property", propertyId) }
        property.deactivate()
        propertyRepository.save(property)
        return ApiResponse.ok()
    }

    @Operation(summary = "숙소 재활성화 (INACTIVE → ACTIVE)")
    @PatchMapping("/{propertyId}/reactivate")
    fun reactivate(@PathVariable propertyId: Long): ApiResponse<Unit> {
        val property = propertyRepository.findById(propertyId)
            .orElseThrow { NotFoundException("Property", propertyId) }
        property.reactivate()
        propertyRepository.save(property)
        return ApiResponse.ok()
    }
}
