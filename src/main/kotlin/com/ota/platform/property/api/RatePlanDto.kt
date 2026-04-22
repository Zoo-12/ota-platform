package com.ota.platform.property.api

import com.ota.platform.property.domain.CancelPolicy
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate

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
