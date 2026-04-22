package com.ota.platform.booking.application

import com.ota.platform.booking.domain.Booking
import com.ota.platform.booking.port.RatePlanPort
import com.ota.platform.booking.port.RoomTypePort
import com.ota.platform.property.application.PropertyUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 예약 도메인 + 숙소/객실/요금제 이름 정보를 조합한 상세 결과를 반환.
 */
@Service
class GetBookingDetailUseCase(
    private val getBookingUseCase: GetBookingUseCase,
    private val roomTypePort: RoomTypePort,
    private val ratePlanPort: RatePlanPort,
    private val propertyUseCase: PropertyUseCase,
) {
    @Transactional(readOnly = true)
    fun getById(bookingId: Long): BookingDetailResult =
        enrich(getBookingUseCase.getById(bookingId))

    @Transactional(readOnly = true)
    fun getByCustomer(customerId: Long): List<BookingDetailResult> =
        getBookingUseCase.getByCustomer(customerId).map { enrich(it) }

    @Transactional(readOnly = true)
    fun getAll(): List<BookingDetailResult> =
        getBookingUseCase.getAll().map { enrich(it) }

    @Transactional(readOnly = true)
    fun getByPropertyId(propertyId: Long): List<BookingDetailResult> =
        getBookingUseCase.getByPropertyId(propertyId).map { enrich(it) }

    private fun enrich(booking: Booking): BookingDetailResult {
        val roomType = roomTypePort.getById(booking.roomTypeId)
        val ratePlan = ratePlanPort.getById(booking.ratePlanId)
        val property = propertyUseCase.getById(roomType.propertyId)
        return BookingDetailResult(
            id = booking.id,
            propertyId = booking.propertyId,
            propertyName = property.name,
            roomTypeId = booking.roomTypeId,
            roomTypeName = roomType.name,
            ratePlanId = booking.ratePlanId,
            ratePlanName = ratePlan.name,
            checkIn = booking.checkIn,
            checkOut = booking.checkOut,
            guestCount = booking.guestCount,
            totalPrice = booking.totalPrice,
            guestName = booking.guestName,
            guestPhone = booking.guestPhone,
            specialRequest = booking.specialRequest,
            status = booking.status.name,
            cancelledAt = booking.cancelledAt,
            cancelReason = booking.cancelReason,
        )
    }
}

data class BookingDetailResult(
    val id: Long,
    val propertyId: Long,
    val propertyName: String,
    val roomTypeId: Long,
    val roomTypeName: String,
    val ratePlanId: Long,
    val ratePlanName: String,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val guestCount: Int,
    val totalPrice: BigDecimal,
    val guestName: String,
    val guestPhone: String?,
    val specialRequest: String?,
    val status: String,
    val cancelledAt: LocalDateTime?,
    val cancelReason: String?,
)
