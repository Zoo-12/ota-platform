package com.ota.platform.inventory.infrastructure

import com.ota.platform.inventory.domain.RoomInventory
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface RoomInventoryRepository : JpaRepository<RoomInventory, Long> {

    fun findByRoomTypeIdAndDate(roomTypeId: Long, date: LocalDate): RoomInventory?

    fun findAllByRoomTypeIdAndDateBetween(
        roomTypeId: Long,
        from: LocalDate,
        to: LocalDate,
    ): List<RoomInventory>

    fun findAllByRoomTypeIdInAndDateBetween(
        roomTypeIds: List<Long>,
        from: LocalDate,
        to: LocalDate,
    ): List<RoomInventory>

    /**
     * 예약 시 비관적 락 적용.
     * 동일 날짜 동시 예약 요청이 들어오면 첫 번째 트랜잭션이 커밋될 때까지 대기.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT ri FROM RoomInventory ri
        WHERE ri.roomTypeId = :roomTypeId
          AND ri.date IN :dates
        ORDER BY ri.date ASC
    """)
    fun findAllWithLock(
        @Param("roomTypeId") roomTypeId: Long,
        @Param("dates") dates: List<LocalDate>,
    ): List<RoomInventory>
}
