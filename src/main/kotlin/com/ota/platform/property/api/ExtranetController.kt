package com.ota.platform.property.api

import com.ota.platform.common.response.ApiResponse
import com.ota.platform.property.application.BulkUpdateInventoryCommand
import com.ota.platform.property.application.InventoryUseCase
import com.ota.platform.property.application.PartnerUseCase
import com.ota.platform.property.application.PropertyUseCase
import com.ota.platform.property.application.RatePlanUseCase
import com.ota.platform.property.application.RegisterPartnerCommand
import com.ota.platform.property.application.RegisterPropertyCommand
import com.ota.platform.property.application.RegisterRatePlanCommand
import com.ota.platform.property.application.RegisterRoomTypeCommand
import com.ota.platform.property.application.RoomTypeUseCase
import com.ota.platform.property.application.UpdatePropertyCommand
import com.ota.platform.property.application.UpdateRatePlanCommand
import com.ota.platform.property.application.UpdateRoomTypeCommand
import com.ota.platform.property.domain.BedType
import com.ota.platform.property.domain.CancelPolicy
import com.ota.platform.property.domain.PropertyCategory
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

// ────────────────────────────────────────────────────────────────
// 파트너
// ────────────────────────────────────────────────────────────────

@Tag(name = "Extranet - 파트너")
@RestController
@RequestMapping("/api/extranet/partners")
class ExtranetPartnerController(
    private val partnerUseCase: PartnerUseCase,
) {
    @Operation(summary = "파트너 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterPartnerRequest): ApiResponse<RegisterResponse> {
        val id = partnerUseCase.register(
            RegisterPartnerCommand(
                name = request.name,
                email = request.email,
                phone = request.phone,
                businessNumber = request.businessNumber,
            ),
        )
        return ApiResponse.ok(RegisterResponse(id))
    }

    @Operation(summary = "파트너 조회")
    @GetMapping("/{partnerId}")
    fun get(@PathVariable partnerId: Long): ApiResponse<PartnerResponse> {
        val partner = partnerUseCase.getById(partnerId)
        return ApiResponse.ok(
            PartnerResponse(
                id = partner.id,
                name = partner.name,
                email = partner.email,
                phone = partner.phone,
                businessNumber = partner.businessNumber,
                status = partner.status.name,
            ),
        )
    }
}

data class RegisterPartnerRequest(
    @field:NotBlank val name: String,
    @field:NotBlank val email: String,
    @field:NotBlank val phone: String,
    @field:NotBlank val businessNumber: String,
)

data class PartnerResponse(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String,
    val businessNumber: String,
    val status: String,
)

// ────────────────────────────────────────────────────────────────
// 숙소
// ────────────────────────────────────────────────────────────────

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
    fun list(@PathVariable partnerId: Long): ApiResponse<List<PropertySummaryResponse>> {
        val properties = propertyUseCase.getByPartner(partnerId)
        return ApiResponse.ok(properties.map {
            PropertySummaryResponse(it.id, it.name, it.category.name, it.status.name, it.addressCity)
        })
    }

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

data class RegisterPropertyRequest(
    @field:NotBlank val name: String,
    val description: String?,
    @field:NotNull val category: PropertyCategory,
    @field:NotBlank val addressCity: String,
    val addressDistrict: String?,
    val addressDetail: String?,
    val latitude: Double?,
    val longitude: Double?,
    val checkInTime: LocalTime?,
    val checkOutTime: LocalTime?,
)

data class UpdatePropertyRequest(
    @field:NotBlank val name: String,
    val description: String?,
    @field:NotBlank val addressCity: String,
    val addressDistrict: String?,
    val addressDetail: String?,
    val checkInTime: LocalTime?,
    val checkOutTime: LocalTime?,
)

data class PropertySummaryResponse(
    val id: Long,
    val name: String,
    val category: String,
    val status: String,
    val addressCity: String,
)

// ────────────────────────────────────────────────────────────────
// 객실 타입
// ────────────────────────────────────────────────────────────────

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
    fun list(@PathVariable propertyId: Long): ApiResponse<List<RoomTypeSummaryResponse>> {
        val roomTypes = roomTypeUseCase.getByProperty(propertyId)
        return ApiResponse.ok(roomTypes.map {
            RoomTypeSummaryResponse(it.id, it.name, it.maxOccupancy, it.bedType.name)
        })
    }

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

data class RegisterRoomTypeRequest(
    @field:NotBlank val name: String,
    val description: String?,
    @field:Min(1) val maxOccupancy: Int,
    @field:NotNull val bedType: BedType,
    val sizeSqm: Double?,
    val amenities: String?,
    val totalCount: Int?,
    val initInventoryFrom: LocalDate?,
    val initInventoryTo: LocalDate?,
)

data class UpdateRoomTypeRequest(
    @field:NotBlank val name: String,
    val description: String?,
    @field:Min(1) val maxOccupancy: Int,
    @field:NotNull val bedType: BedType,
    val sizeSqm: Double?,
    val amenities: String?,
)

data class RoomTypeSummaryResponse(
    val id: Long,
    val name: String,
    val maxOccupancy: Int,
    val bedType: String,
)

// ────────────────────────────────────────────────────────────────
// 요금 플랜
// ────────────────────────────────────────────────────────────────

@Tag(name = "Extranet - 요금 플랜")
@RestController
@RequestMapping("/api/extranet/room-types/{roomTypeId}/rate-plans")
class ExtranetRatePlanController(
    private val ratePlanUseCase: RatePlanUseCase,
) {
    @Operation(summary = "요금 플랜 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @PathVariable roomTypeId: Long,
        @Valid @RequestBody request: RegisterRatePlanRequest,
    ): ApiResponse<RegisterResponse> {
        val id = ratePlanUseCase.register(
            RegisterRatePlanCommand(
                roomTypeId = roomTypeId,
                name = request.name,
                cancelPolicy = request.cancelPolicy,
                breakfastIncluded = request.breakfastIncluded,
                basePrice = request.basePrice,
            ),
        )
        return ApiResponse.ok(RegisterResponse(id))
    }

    @Operation(summary = "요금 플랜 목록 조회")
    @GetMapping
    fun list(@PathVariable roomTypeId: Long): ApiResponse<List<RatePlanResponse>> {
        val plans = ratePlanUseCase.getByRoomType(roomTypeId)
        return ApiResponse.ok(plans.map {
            RatePlanResponse(it.id, it.name, it.cancelPolicy.name, it.breakfastIncluded, it.basePrice)
        })
    }

    @Operation(summary = "요금 플랜 수정")
    @PutMapping("/{ratePlanId}")
    fun update(
        @PathVariable roomTypeId: Long,
        @PathVariable ratePlanId: Long,
        @Valid @RequestBody request: UpdateRatePlanRequest,
    ): ApiResponse<Unit> {
        ratePlanUseCase.update(ratePlanId, UpdateRatePlanCommand(
            name = request.name,
            cancelPolicy = request.cancelPolicy,
            breakfastIncluded = request.breakfastIncluded,
            basePrice = request.basePrice,
        ))
        return ApiResponse.ok()
    }

    @Operation(summary = "날짜별 요금 설정")
    @PatchMapping("/{ratePlanId}/daily-rates")
    fun setDailyRate(
        @PathVariable ratePlanId: Long,
        @Valid @RequestBody request: SetDailyRateRequest,
    ): ApiResponse<Unit> {
        ratePlanUseCase.setDailyRate(ratePlanId, request.date, request.price)
        return ApiResponse.ok()
    }
}

data class RegisterRatePlanRequest(
    @field:NotBlank val name: String,
    @field:NotNull val cancelPolicy: CancelPolicy,
    val breakfastIncluded: Boolean = false,
    @field:NotNull val basePrice: BigDecimal,
)

data class UpdateRatePlanRequest(
    @field:NotBlank val name: String,
    @field:NotNull val cancelPolicy: CancelPolicy,
    val breakfastIncluded: Boolean = false,
    @field:NotNull val basePrice: BigDecimal,
)

data class SetDailyRateRequest(
    @field:NotNull val date: LocalDate,
    @field:NotNull val price: BigDecimal,
)

data class RatePlanResponse(
    val id: Long,
    val name: String,
    val cancelPolicy: String,
    val breakfastIncluded: Boolean,
    val basePrice: BigDecimal,
)

// ────────────────────────────────────────────────────────────────
// 재고
// ────────────────────────────────────────────────────────────────

@Tag(name = "Extranet - 재고")
@RestController
@RequestMapping("/api/extranet/room-types/{roomTypeId}/inventories")
class ExtranetInventoryController(
    private val inventoryUseCase: InventoryUseCase,
) {
    @Operation(summary = "기간별 재고 일괄 설정")
    @PutMapping
    fun bulkUpdate(
        @PathVariable roomTypeId: Long,
        @Valid @RequestBody request: BulkUpdateInventoryRequest,
    ): ApiResponse<Unit> {
        inventoryUseCase.bulkUpdate(
            BulkUpdateInventoryCommand(
                roomTypeId = roomTypeId,
                from = request.from,
                to = request.to,
                totalCount = request.totalCount,
                stopSell = request.stopSell,
                minStay = request.minStay,
                maxStay = request.maxStay,
            ),
        )
        return ApiResponse.ok()
    }

    @Operation(summary = "기간별 재고 조회")
    @GetMapping
    fun list(
        @PathVariable roomTypeId: Long,
        @RequestParam from: LocalDate,
        @RequestParam to: LocalDate,
    ): ApiResponse<List<InventoryResponse>> {
        val inventories = inventoryUseCase.getInventories(roomTypeId, from, to)
        return ApiResponse.ok(inventories.map {
            InventoryResponse(it.date.toString(), it.totalCount, it.availableCount, it.stopSell)
        })
    }
}

data class BulkUpdateInventoryRequest(
    @field:NotNull val from: LocalDate,
    @field:NotNull val to: LocalDate,
    val totalCount: Int?,
    val stopSell: Boolean?,
    val minStay: Int?,
    val maxStay: Int?,
)

data class InventoryResponse(
    val date: String,
    val totalCount: Int,
    val availableCount: Int,
    val stopSell: Boolean,
)

// ────────────────────────────────────────────────────────────────
// 공통
// ────────────────────────────────────────────────────────────────

data class RegisterResponse(val id: Long)
