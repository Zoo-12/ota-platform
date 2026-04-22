package com.ota.platform.property.api

import com.ota.platform.common.response.ApiResponse
import com.ota.platform.property.application.GetPropertyDetailUseCase
import com.ota.platform.property.application.PropertyUseCase
import com.ota.platform.property.domain.PropertyStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Caching
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
    private val propertyUseCase: PropertyUseCase,
    private val getPropertyDetailUseCase: GetPropertyDetailUseCase,
) {
    @Operation(summary = "전체 숙소 목록 조회")
    @GetMapping
    fun list(
        @RequestParam(required = false) status: PropertyStatus?,
        @RequestParam(required = false) city: String?,
    ): ApiResponse<List<PropertySummaryResponse>> =
        ApiResponse.ok(propertyUseCase.list(status, city).map {
            PropertySummaryResponse(it.id, it.name, it.category.name, it.status.name, it.addressCity)
        })

    @Operation(summary = "숙소 상세 조회")
    @GetMapping("/{propertyId}")
    fun get(@PathVariable propertyId: Long): ApiResponse<PropertyDetailResponse> =
        ApiResponse.ok(getPropertyDetailUseCase.getById(propertyId).toResponse())

    @Operation(summary = "숙소 승인 (PENDING_APPROVAL → ACTIVE)")
    @Caching(evict = [
        CacheEvict(cacheNames = ["accommodation-search"], allEntries = true),
        CacheEvict(cacheNames = ["accommodation-detail"], allEntries = true),
    ])
    @PatchMapping("/{propertyId}/approve")
    fun approve(@PathVariable propertyId: Long): ApiResponse<Unit> {
        propertyUseCase.approve(propertyId)
        return ApiResponse.ok()
    }

    @Operation(summary = "숙소 비활성화 (ACTIVE → INACTIVE)")
    @Caching(evict = [
        CacheEvict(cacheNames = ["accommodation-search"], allEntries = true),
        CacheEvict(cacheNames = ["accommodation-detail"], allEntries = true),
    ])
    @PatchMapping("/{propertyId}/deactivate")
    fun deactivate(@PathVariable propertyId: Long): ApiResponse<Unit> {
        propertyUseCase.deactivate(propertyId)
        return ApiResponse.ok()
    }

    @Operation(summary = "숙소 재활성화 (INACTIVE → ACTIVE)")
    @Caching(evict = [
        CacheEvict(cacheNames = ["accommodation-search"], allEntries = true),
        CacheEvict(cacheNames = ["accommodation-detail"], allEntries = true),
    ])
    @PatchMapping("/{propertyId}/reactivate")
    fun reactivate(@PathVariable propertyId: Long): ApiResponse<Unit> {
        propertyUseCase.reactivate(propertyId)
        return ApiResponse.ok()
    }
}
