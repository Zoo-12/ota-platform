package com.ota.platform.booking.event

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 예약 생성 이벤트.
 * 트랜잭션 커밋 후 발행되어 알림 발송, 통계 수집 등 후처리를 담당한다.
 */
data class BookingCreatedEvent(
    val bookingId: Long,
    val customerId: Long,
    val propertyId: Long,
    val roomTypeId: Long,
    val guestName: String,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val totalPrice: BigDecimal,
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)

/**
 * 예약 취소 이벤트.
 * 취소 확정 후 알림 발송, 쿠폰 복원 등 후처리를 담당한다.
 */
data class BookingCancelledEvent(
    val bookingId: Long,
    val customerId: Long,
    val propertyId: Long,
    val reason: String?,
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)
