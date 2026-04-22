package com.ota.platform.property.api

import com.ota.platform.common.response.ApiResponse
import com.ota.platform.common.response.RegisterResponse
import com.ota.platform.property.application.PropertyUseCase
import com.ota.platform.property.application.RegisterPropertyCommand
import com.ota.platform.property.application.UpdatePropertyCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Extranet - 숙소")
@RestController
@RequestMapping("/api/extranet/partners/{partnerId}/properties")
class ExtranetPropertyController(
    private val propertyUseCase: PropertyUseCase,
) {
    @Operation(summary = "숙소 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @PathVariable partnerId: Long,
        @Valid @RequestBody request: RegisterPropertyRequest,
    ): ApiResponse<RegisterResponse> {
        val id = propertyUseCase.register(
            RegisterPropertyCommand(
                partnerId = partnerId,
                name = request.name,
                description = request.description,
                category = request.category,
                addressCity = request.addressCity,
                addressDistrict = request.addressDistrict,
                addressDetail = request.addressDetail,
                latitude = request.latitude,
                longitude = request.longitude,
                checkInTime = request.checkInTime,
                checkOutTime = request.checkOutTime,
            ),
        )
        return ApiResponse.ok(RegisterResponse(id))
    }

    @Operation(summary = "내 숙소 목록 조회")
    @GetMapping
    fun list(@PathVariable partnerId: Long): ApiResponse<List<PropertySummaryResponse>> =
        ApiResponse.ok(propertyUseCase.getByPartner(partnerId).map {
            PropertySummaryResponse(it.id, it.name, it.category.name, it.status.name, it.addressCity)
        })

    @Operation(summary = "숙소 수정")
    @PutMapping("/{propertyId}")
    fun update(
        @PathVariable partnerId: Long,
        @PathVariable propertyId: Long,
        @Valid @RequestBody request: UpdatePropertyRequest,
    ): ApiResponse<Unit> {
        propertyUseCase.update(
            UpdatePropertyCommand(
                partnerId = partnerId,
                propertyId = propertyId,
                name = request.name,
                description = request.description,
                addressCity = request.addressCity,
                addressDistrict = request.addressDistrict,
                addressDetail = request.addressDetail,
                checkInTime = request.checkInTime,
                checkOutTime = request.checkOutTime,
            ),
        )
        return ApiResponse.ok()
    }
}
