package com.ota.platform.booking.application

import com.ota.platform.booking.infrastructure.BookingRepository
import com.ota.platform.common.exception.BadRequestException
import com.ota.platform.common.exception.NotFoundException
import com.ota.platform.inventory.domain.RoomInventoryService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CancelBookingUseCase(
    private val bookingRepository: BookingRepository,
    private val roomInventoryService: RoomInventoryService,
) {

    @Transactional
    fun cancel(command: CancelBookingCommand) {
        val booking = bookingRepository.findById(command.bookingId)
            .orElseThrow { NotFoundException("Booking", command.bookingId) }

        if (booking.customerId != command.customerId) {
            throw BadRequestException("해당 예약에 대한 권한이 없습니다.")
        }

        booking.cancel(command.reason)

        // 재고 복원
        val inventoryIds = booking.bookingRooms.map { it.roomInventoryId }
        roomInventoryService.increaseInventories(inventoryIds)
    }
}

data class CancelBookingCommand(
    val customerId: Long,
    val bookingId: Long,
    val reason: String?,
)
