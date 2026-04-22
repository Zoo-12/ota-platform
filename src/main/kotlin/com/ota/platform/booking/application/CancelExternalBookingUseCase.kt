package com.ota.platform.booking.application

import com.ota.platform.booking.infrastructure.ExternalBookingRepository
import com.ota.platform.common.exception.BadRequestException
import com.ota.platform.common.exception.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CancelExternalBookingUseCase(
    private val externalBookingRepository: ExternalBookingRepository,
) {
    @Transactional
    fun cancel(command: CancelExternalBookingCommand) {
        val booking = externalBookingRepository.findById(command.bookingId)
            .orElseThrow { NotFoundException("ExternalBooking", command.bookingId) }

        if (booking.customerId != command.customerId) {
            throw BadRequestException("해당 예약에 대한 권한이 없습니다.")
        }

        booking.cancel()
        externalBookingRepository.save(booking)
    }
}

data class CancelExternalBookingCommand(
    val customerId: Long,
    val bookingId: Long,
)
