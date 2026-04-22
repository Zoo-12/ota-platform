package com.ota.platform.property.api

import com.ota.platform.common.response.ApiResponse
import com.ota.platform.common.response.RegisterResponse
import com.ota.platform.property.application.RegisterRoomTypeCommand
import com.ota.platform.property.application.RoomTypeUseCase
import com.ota.platform.property.application.UpdateRoomTypeCommand
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

@Tag(name = "Extranet - 객실 타입")
@RestController
@RequestMapping("/api/extranet/properties/{propertyId}/room-types")
class ExtranetRoomTypeController(
    private val roomTypeUseCase: RoomTypeUseCase,
) {
    @Operation(summary = "객실 타입 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @PathVariable propertyId: Long,
        @Valid @RequestBody request: RegisterRoomTypeRequest,
    ): ApiResponse<RegisterResponse> {
        val id = roomTypeUseCase.register(
            RegisterRoomTypeCommand(
                propertyId = propertyId,
                name = request.name,
                description = request.description,
                maxOccupancy = request.maxOccupancy,
                bedType = request.bedType,
                sizeSqm = request.sizeSqm,
                amenities = request.amenities,
                totalCount = request.totalCount,
                initInventoryFrom = request.initInventoryFrom,
                initInventoryTo = request.initInventoryTo,
            ),
        )
        return ApiResponse.ok(RegisterResponse(id))
    }

    @Operation(summary = "객실 타입 목록 조회")
    @GetMapping
    fun list(@PathVariable propertyId: Long): ApiResponse<List<RoomTypeSummaryResponse>> =
        ApiResponse.ok(roomTypeUseCase.getByProperty(propertyId).map {
            RoomTypeSummaryResponse(it.id, it.name, it.maxOccupancy, it.bedType.name)
        })

    @Operation(summary = "객실 타입 수정")
    @PutMapping("/{roomTypeId}")
    fun update(
        @PathVariable propertyId: Long,
        @PathVariable roomTypeId: Long,
        @Valid @RequestBody request: UpdateRoomTypeRequest,
    ): ApiResponse<Unit> {
        roomTypeUseCase.update(roomTypeId, UpdateRoomTypeCommand(
            name = request.name,
            description = request.description,
            maxOccupancy = request.maxOccupancy,
            bedType = request.bedType,
            sizeSqm = request.sizeSqm,
            amenities = request.amenities,
        ))
        return ApiResponse.ok()
    }
}
