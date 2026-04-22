package com.ota.platform.property.api

import com.ota.platform.property.domain.PropertyCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalTime

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
