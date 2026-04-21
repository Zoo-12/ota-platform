package com.ota.platform.property.port

import java.time.LocalDate

interface InventoryPort {
    fun findByRoomTypeIdAndDate(roomTypeId: Long, date: LocalDate): InventoryData?
    fun updateStopSell(id: Long, stopSell: Boolean)
    fun updateStayRestriction(id: Long, minStay: Int, maxStay: Int?)
    fun create(roomTypeId: Long, date: LocalDate, totalCount: Int)
    fun initInventories(roomTypeId: Long, totalCount: Int, from: LocalDate, to: LocalDate)
    fun findAllByRoomTypeIdAndDateBetween(roomTypeId: Long, from: LocalDate, to: LocalDate): List<InventoryData>
}

data class InventoryData(
    val id: Long,
    val date: LocalDate,
    val totalCount: Int,
    val availableCount: Int,
    val stopSell: Boolean,
)
