package com.ota.platform.booking.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 예약의 날짜별 상세.
 * 체크인~체크아웃 기간의 각 날짜마다 1행 생성.
 * priceSnapshot: 예약 시점 요금 불변 저장 — 이후 요금 변경과 무관.
 * 불변 레코드이므로 BaseEntity(updated_at) 미사용.
 */
@Entity
@Table(name = "booking_rooms")
class BookingRoom(
    booking: Booking,
    roomInventoryId: Long,
    date: LocalDate,
    priceSnapshot: BigDecimal,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    var booking: Booking = booking
        protected set

    @Column(nullable = false)
    var roomInventoryId: Long = roomInventoryId
        protected set

    @Column(nullable = false)
    var date: LocalDate = date
        protected set

    @Column(nullable = false, precision = 12, scale = 2)
    var priceSnapshot: BigDecimal = priceSnapshot
        protected set

    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set
}
