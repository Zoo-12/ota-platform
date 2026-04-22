package com.ota.platform.booking.domain

import com.ota.platform.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "external_booking")
class ExternalBooking(
    customerId: Long,
    accommodationId: String,
    externalBookingNo: String,
    source: String,
    checkIn: LocalDate,
    checkOut: LocalDate,
    guestCount: Int,
    totalPrice: BigDecimal?,
    guestName: String,
    guestPhone: String?,
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
        protected set

    @Column(nullable = false)
    var customerId: Long = customerId
        protected set

    @Column(nullable = false, length = 255)
    var accommodationId: String = accommodationId
        protected set

    @Column(nullable = false, length = 255)
    var externalBookingNo: String = externalBookingNo
        protected set

    @Column(nullable = false, length = 50)
    var source: String = source
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

    @Column(precision = 12, scale = 2)
    var totalPrice: BigDecimal? = totalPrice
        protected set

    @Column(nullable = false, length = 255)
    var guestName: String = guestName
        protected set

    @Column(length = 20)
    var guestPhone: String? = guestPhone
        protected set

    @Column(nullable = false, length = 20)
    var status: String = "CONFIRMED"
        protected set
}
