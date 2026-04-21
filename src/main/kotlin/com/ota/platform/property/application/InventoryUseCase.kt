package com.ota.platform.property.application

import com.ota.platform.common.exception.NotFoundException
import com.ota.platform.property.infrastructure.RoomTypeRepository
import com.ota.platform.property.port.InventoryData
import com.ota.platform.property.port.InventoryPort
import com.ota.platform.common.config.CacheNames
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class InventoryUseCase(
    private val inventoryPort: InventoryPort,
    private val roomTypeRepository: RoomTypeRepository,
) {

    @CacheEvict(cacheNames = [CacheNames.ACCOMMODATION_SEARCH], allEntries = true)
    @Transactional
    fun bulkUpdate(command: BulkUpdateInventoryCommand) {
        if (!roomTypeRepository.existsById(command.roomTypeId)) {
            throw NotFoundException("RoomType", command.roomTypeId)
        }
        val dates = command.from.datesUntil(command.to.plusDays(1)).toList()

        dates.forEach { date ->
            val inventory = inventoryPort.findByRoomTypeIdAndDate(command.roomTypeId, date)
            if (inventory != null) {
                command.stopSell?.let { inventoryPort.updateStopSell(inventory.id, it) }
                command.minStay?.let { inventoryPort.updateStayRestriction(inventory.id, it, command.maxStay) }
            } else {
                inventoryPort.create(command.roomTypeId, date, command.totalCount ?: 0)
            }
        }
    }

    @Transactional(readOnly = true)
    fun getInventories(roomTypeId: Long, from: LocalDate, to: LocalDate): List<InventoryData> =
        inventoryPort.findAllByRoomTypeIdAndDateBetween(roomTypeId, from, to)
}

data class BulkUpdateInventoryCommand(
    val roomTypeId: Long,
    val from: LocalDate,
    val to: LocalDate,
    val totalCount: Int?,
    val stopSell: Boolean?,
    val minStay: Int?,
    val maxStay: Int?,
)
