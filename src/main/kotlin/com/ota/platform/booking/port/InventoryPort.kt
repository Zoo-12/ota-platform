package com.ota.platform.booking.port

import java.time.LocalDate

interface InventoryPort {
    fun decrease(roomTypeId: Long, checkIn: LocalDate, checkOut: LocalDate): List<InventoryInfo>
    fun increase(roomInventoryIds: List<Long>)
}

data class InventoryInfo(
    val id: Long,
    val date: LocalDate,
)
