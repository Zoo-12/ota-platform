package com.ota.platform.property.adapter

import com.ota.platform.inventory.domain.RoomInventory
import com.ota.platform.inventory.infrastructure.RoomInventoryRepository
import com.ota.platform.property.port.InventoryData
import com.ota.platform.property.port.InventoryPort
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component("propertyInventoryAdapter")
class InventoryAdapter(
    private val roomInventoryRepository: RoomInventoryRepository,
) : InventoryPort {

    override fun findByRoomTypeIdAndDate(roomTypeId: Long, date: LocalDate): InventoryData? =
        roomInventoryRepository.findByRoomTypeIdAndDate(roomTypeId, date)?.toData()

    override fun updateStopSell(id: Long, stopSell: Boolean) {
        val inventory = roomInventoryRepository.findById(id).orElseThrow()
        inventory.updateStopSell(stopSell)
    }

    override fun updateStayRestriction(id: Long, minStay: Int, maxStay: Int?) {
        val inventory = roomInventoryRepository.findById(id).orElseThrow()
        inventory.updateStayRestriction(minStay, maxStay)
    }

    override fun create(roomTypeId: Long, date: LocalDate, totalCount: Int) {
        roomInventoryRepository.save(RoomInventory(roomTypeId, date, totalCount, totalCount))
    }

    override fun initInventories(roomTypeId: Long, totalCount: Int, from: LocalDate, to: LocalDate) {
        val inventories = from.datesUntil(to.plusDays(1)).map { date ->
            RoomInventory(roomTypeId, date, totalCount, totalCount)
        }.toList()
        roomInventoryRepository.saveAll(inventories)
    }

    override fun findAllByRoomTypeIdAndDateBetween(
        roomTypeId: Long,
        from: LocalDate,
        to: LocalDate,
    ): List<InventoryData> =
        roomInventoryRepository.findAllByRoomTypeIdAndDateBetween(roomTypeId, from, to).map { it.toData() }

    private fun RoomInventory.toData() = InventoryData(id, date, totalCount, availableCount, stopSell)
}
