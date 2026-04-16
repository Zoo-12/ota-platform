package com.ota.platform.property.application

import com.ota.platform.common.exception.NotFoundException
import com.ota.platform.inventory.domain.RoomInventory
import com.ota.platform.inventory.infrastructure.RoomInventoryRepository
import com.ota.platform.property.infrastructure.RoomTypeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class InventoryUseCase(
    private val roomInventoryRepository: RoomInventoryRepository,
    private val roomTypeRepository: RoomTypeRepository,
) {

    @Transactional
    fun bulkUpdate(command: BulkUpdateInventoryCommand) {
        if (!roomTypeRepository.existsById(command.roomTypeId)) {
            throw NotFoundException("RoomType", command.roomTypeId)
        }
        val dates = command.from.datesUntil(command.to.plusDays(1)).toList()

        dates.forEach { date ->
            val inventory = roomInventoryRepository.findByRoomTypeIdAndDate(command.roomTypeId, date)
            if (inventory != null) {
                // 기존 재고 행 업데이트
                command.stopSell?.let { inventory.updateStopSell(it) }
                command.minStay?.let { inventory.updateStayRestriction(it, command.maxStay) }
            } else {
                // 재고 행 신규 생성
                roomInventoryRepository.save(
                    RoomInventory(
                        roomTypeId = command.roomTypeId,
                        date = date,
                        totalCount = command.totalCount ?: 0,
                        availableCount = command.totalCount ?: 0,
                    ),
                )
            }
        }
    }

    @Transactional(readOnly = true)
    fun getInventories(roomTypeId: Long, from: LocalDate, to: LocalDate): List<RoomInventory> =
        roomInventoryRepository.findAllByRoomTypeIdAndDateBetween(roomTypeId, from, to)
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
