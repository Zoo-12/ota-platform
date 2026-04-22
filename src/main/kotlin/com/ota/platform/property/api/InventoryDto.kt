package com.ota.platform.property.api

import jakarta.validation.constraints.NotNull
import java.time.LocalDate

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
