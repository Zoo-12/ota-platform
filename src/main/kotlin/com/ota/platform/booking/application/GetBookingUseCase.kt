package com.ota.platform.booking.application

import com.ota.platform.booking.domain.Booking
import com.ota.platform.booking.infrastructure.BookingRepository
import com.ota.platform.common.exception.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetBookingUseCase(
    private val bookingRepository: BookingRepository,
) {

    @Transactional(readOnly = true)
    fun getById(bookingId: Long): Booking =
        bookingRepository.findById(bookingId)
            .orElseThrow { NotFoundException("Booking", bookingId) }

    @Transactional(readOnly = true)
    fun getByCustomer(customerId: Long): List<Booking> =
        bookingRepository.findAllByCustomerId(customerId)

    @Transactional(readOnly = true)
    fun getAll(): List<Booking> =
        bookingRepository.findAll()

    @Transactional(readOnly = true)
    fun getByPropertyId(propertyId: Long): List<Booking> =
        bookingRepository.findAllByPropertyId(propertyId)
}
