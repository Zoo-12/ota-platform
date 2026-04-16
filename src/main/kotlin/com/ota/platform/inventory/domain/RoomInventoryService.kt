package com.ota.platform.inventory.domain

import com.ota.platform.common.exception.ConflictException
import com.ota.platform.inventory.infrastructure.RoomInventoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class RoomInventoryService(
    private val roomInventoryRepository: RoomInventoryRepository,
) {

    /**
     * 체크인~체크아웃 기간의 각 날짜 재고를 비관적 락으로 조회 후 차감.
     * 하나라도 재고 없으면 전체 실패.
     */
    @Transactional
    fun decreaseInventories(roomTypeId: Long, checkIn: LocalDate, checkOut: LocalDate): List<RoomInventory> {
        val dates = checkIn.datesUntil(checkOut).toList()
        val inventories = roomInventoryRepository.findAllWithLock(roomTypeId, dates)

        if (inventories.size != dates.size) {
            throw ConflictException("일부 날짜의 재고 정보가 없습니다.")
        }

        inventories.forEach { it.decrease() }
        return inventories
    }

    /**
     * 예약 취소 시 재고 복원.
     */
    @Transactional
    fun increaseInventories(roomInventoryIds: List<Long>) {
        val inventories = roomInventoryRepository.findAllById(roomInventoryIds)
        inventories.forEach { it.increase() }
    }

    /**
     * 특정 기간 가용 여부 확인 (락 없음 — 조회 전용).
     */
    @Transactional(readOnly = true)
    fun checkAvailability(roomTypeId: Long, checkIn: LocalDate, checkOut: LocalDate): Boolean {
        val dates = checkIn.datesUntil(checkOut).toList()
        val inventories = roomInventoryRepository.findAllByRoomTypeIdAndDateBetween(
            roomTypeId, checkIn, checkOut.minusDays(1),
        )
        return inventories.size == dates.size && inventories.all { it.isAvailable() }
    }
}
