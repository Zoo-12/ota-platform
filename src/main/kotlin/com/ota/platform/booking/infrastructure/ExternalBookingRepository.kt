package com.ota.platform.booking.infrastructure

import com.ota.platform.booking.domain.ExternalBooking
import org.springframework.data.jpa.repository.JpaRepository

interface ExternalBookingRepository : JpaRepository<ExternalBooking, Long> {
    fun findAllByCustomerId(customerId: Long): List<ExternalBooking>
}
