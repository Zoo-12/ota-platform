package com.ota.platform.inventory.domain

import com.ota.platform.common.domain.BaseEntity
import com.ota.platform.common.exception.ConflictException
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

/**
 * 날짜별 객실 재고.
 * (room_type_id, date) UNIQUE — 날짜별로 단 하나의 행만 존재.
 * 예약 시 SELECT FOR UPDATE로 잠금 후 available_count 차감.
 */
@Entity
@Table(name = "room_inventory")
class RoomInventory(
    roomTypeId: Long,
    date: LocalDate,
    totalCount: Int,
    availableCount: Int,
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
        protected set

    @Column(nullable = false)
    var roomTypeId: Long = roomTypeId
        protected set

    @Column(nullable = false)
    var date: LocalDate = date
        protected set

    @Column(nullable = false)
    var totalCount: Int = totalCount
        protected set

    @Column(nullable = false)
    var availableCount: Int = availableCount
        protected set

    @Column(nullable = false)
    var stopSell: Boolean = false
        protected set

    @Column(nullable = false)
    var minStay: Int = 1
        protected set

    var maxStay: Int? = null
        protected set

    fun isAvailable(): Boolean = !stopSell && availableCount > 0

    fun decrease() {
        if (!isAvailable()) throw ConflictException("재고가 없습니다. roomTypeId=$roomTypeId, date=$date")
        availableCount -= 1
    }

    fun increase() {
        check(availableCount < totalCount) { "재고가 총 수량을 초과합니다." }
        availableCount += 1
    }

    fun updateStopSell(stopSell: Boolean) {
        this.stopSell = stopSell
    }

    fun updateStayRestriction(minStay: Int, maxStay: Int?) {
        this.minStay = minStay
        this.maxStay = maxStay
    }
}
