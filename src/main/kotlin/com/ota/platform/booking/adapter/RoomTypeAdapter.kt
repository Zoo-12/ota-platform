package com.ota.platform.booking.adapter

import com.ota.platform.booking.port.RoomTypeInfo
import com.ota.platform.booking.port.RoomTypePort
import com.ota.platform.common.exception.NotFoundException
import com.ota.platform.property.infrastructure.RoomTypeRepository
import org.springframework.stereotype.Component

@Component
class RoomTypeAdapter(
    private val roomTypeRepository: RoomTypeRepository,
) : RoomTypePort {
    override fun getById(id: Long): RoomTypeInfo =
        roomTypeRepository.findById(id)
            .map { RoomTypeInfo(it.id, it.propertyId, it.name, it.bedType.name, it.maxOccupancy) }
            .orElseThrow { NotFoundException("RoomType", id) }
}
