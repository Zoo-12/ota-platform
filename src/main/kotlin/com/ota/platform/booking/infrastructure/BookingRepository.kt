package com.ota.platform.booking.infrastructure

import com.ota.platform.booking.domain.Booking
import com.ota.platform.booking.domain.BookingStatus
import org.springframework.data.jpa.repository.JpaRepository

interface BookingRepository : JpaRepository<Booking, Long> {
    fun findAllByCustomerIdAndStatus(customerId: Long, status: BookingStatus): List<Booking>
    fun findAllByCustomerId(customerId: Long): List<Booking>
    fun findAllByPropertyId(propertyId: Long): List<Booking>
}
