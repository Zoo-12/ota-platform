package com.ota.platform.booking.port

interface RoomTypePort {
    fun getById(id: Long): RoomTypeInfo
}

data class RoomTypeInfo(
    val id: Long,
    val propertyId: Long,
    val name: String,
    val bedType: String,
    val maxOccupancy: Int,
)
