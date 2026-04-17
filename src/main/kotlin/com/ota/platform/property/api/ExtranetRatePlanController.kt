package com.ota.platform.property.api

import com.ota.platform.common.response.ApiResponse
import com.ota.platform.common.response.RegisterResponse
import com.ota.platform.property.application.RatePlanUseCase
import com.ota.platform.property.application.RegisterRatePlanCommand
import com.ota.platform.property.application.UpdateRatePlanCommand
import com.ota.platform.property.domain.CancelPolicy
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

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
