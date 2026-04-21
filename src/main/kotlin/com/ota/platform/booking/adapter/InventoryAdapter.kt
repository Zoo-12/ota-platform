package com.ota.platform.booking.adapter

import com.ota.platform.booking.port.InventoryInfo
import com.ota.platform.booking.port.InventoryPort
import com.ota.platform.inventory.domain.RoomInventoryService
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component("bookingInventoryAdapter")
class InventoryAdapter(
    private val roomInventoryService: RoomInventoryService,
) : InventoryPort {

    override fun decrease(roomTypeId: Long, checkIn: LocalDate, checkOut: LocalDate): List<InventoryInfo> =
        roomInventoryService.decreaseInventories(roomTypeId, checkIn, checkOut)
            .map { InventoryInfo(it.id, it.date) }

    override fun increase(roomInventoryIds: List<Long>) =
        roomInventoryService.increaseInventories(roomInventoryIds)
}
