package com.ota.platform.booking.domain

import com.ota.platform.common.domain.BaseEntity
import com.ota.platform.common.exception.BadRequestException
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "bookings")
class Booking(
    customerId: Long,
    propertyId: Long,
    roomTypeId: Long,
    ratePlanId: Long,
    checkIn: LocalDate,
    checkOut: LocalDate,
    guestCount: Int,
    totalPrice: BigDecimal,
    guestName: String,
    guestPhone: String?,
    specialRequest: String?,
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
        protected set

    @Column(nullable = false)
    var customerId: Long = customerId
        protected set

    // 예약 시점 비정규화 — 목록 조회 시 JOIN 없이 사용
    @Column(nullable = false)
    var propertyId: Long = propertyId
        protected set

    @Column(nullable = false)
    var roomTypeId: Long = roomTypeId
        protected set

    @Column(nullable = false)
    var ratePlanId: Long = ratePlanId
        protected set

    @Column(nullable = false)
    var checkIn: LocalDate = checkIn
        protected set

    @Column(nullable = false)
    var checkOut: LocalDate = checkOut
        protected set

    @Column(nullable = false)
    var guestCount: Int = guestCount
        protected set

    @Column(nullable = false, precision = 12, scale = 2)
    var totalPrice: BigDecimal = totalPrice
        protected set

    @Column(nullable = false, length = 100)
    var guestName: String = guestName
        protected set

    @Column(length = 20)
    var guestPhone: String? = guestPhone
        protected set

    @Column(columnDefinition = "TEXT")
    var specialRequest: String? = specialRequest
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: BookingStatus = BookingStatus.CONFIRMED
        protected set

    var cancelledAt: LocalDateTime? = null
        protected set

    @Column(columnDefinition = "TEXT")
    var cancelReason: String? = null
        protected set

    @OneToMany(mappedBy = "booking", cascade = [CascadeType.ALL], orphanRemoval = true)
    val bookingRooms: MutableList<BookingRoom> = mutableListOf()

    fun cancel(reason: String?) {
        if (status == BookingStatus.CANCELLED) throw BadRequestException("이미 취소된 예약입니다.")
        status = BookingStatus.CANCELLED
        cancelledAt = LocalDateTime.now()
        cancelReason = reason
    }

    fun isCancellable(): Boolean = status == BookingStatus.CONFIRMED
}

enum class BookingStatus {
    CONFIRMED,
    CANCELLED,
}
