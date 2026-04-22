package com.ota.platform.property.api

import com.ota.platform.property.domain.BedType
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

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
