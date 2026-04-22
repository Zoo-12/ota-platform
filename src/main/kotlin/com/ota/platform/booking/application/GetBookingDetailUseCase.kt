package com.ota.platform.booking.application

import com.ota.platform.booking.domain.Booking
import com.ota.platform.booking.domain.BookingKeyType
import com.ota.platform.booking.domain.ExternalBooking
import com.ota.platform.booking.infrastructure.ExternalBookingRepository
import com.ota.platform.booking.port.RatePlanPort
import com.ota.platform.booking.port.RoomTypePort
import com.ota.platform.common.exception.NotFoundException
import com.ota.platform.property.application.PropertyUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 예약 도메인 + 숙소/객실/요금제 이름 정보를 조합한 상세 결과를 반환.
 * 내부 예약(booking)과 외부 예약(external_booking)을 함께 조회한다.
 */
@Service
class GetBookingDetailUseCase(
    private val getBookingUseCase: GetBookingUseCase,
    private val roomTypePort: RoomTypePort,
    private val ratePlanPort: RatePlanPort,
    private val propertyUseCase: PropertyUseCase,
    private val externalBookingRepository: ExternalBookingRepository,
) {
    @Transactional(readOnly = true)
    fun getById(bookingKey: String): BookingDetailResult {
        val (type, id) = BookingKeyType.parse(bookingKey)
        return when (type) {
            BookingKeyType.EXTERNAL -> {
                val extBooking = externalBookingRepository.findById(id)
                    .orElseThrow { NotFoundException("ExternalBooking", id) }
                enrichExternal(extBooking)
            }
            BookingKeyType.INTERNAL -> enrich(getBookingUseCase.getById(id))
        }
    }

    @Transactional(readOnly = true)
    fun getByCustomer(customerId: Long): List<BookingDetailResult> {
        val internal = getBookingUseCase.getByCustomer(customerId).map { enrich(it) }
        val external = externalBookingRepository.findAllByCustomerId(customerId).map { enrichExternal(it) }
        return (internal + external).sortedByDescending { it.createdAt }
    }

    @Transactional(readOnly = true)
    fun getAll(): List<BookingDetailResult> {
        val internal = getBookingUseCase.getAll().map { enrich(it) }
        val external = externalBookingRepository.findAll().map { enrichExternal(it) }
        return (internal + external).sortedByDescending { it.createdAt }
    }

    @Transactional(readOnly = true)
    fun getByPropertyId(propertyId: Long): List<BookingDetailResult> =
        getBookingUseCase.getByPropertyId(propertyId).map { enrich(it) }

    private fun enrich(booking: Booking): BookingDetailResult {
        val roomType = roomTypePort.getById(booking.roomTypeId)
        val ratePlan = ratePlanPort.getById(booking.ratePlanId)
        val property = propertyUseCase.getById(roomType.propertyId)
        return BookingDetailResult(
            bookingKey = BookingKeyType.INTERNAL.key(booking.id),
            source = "INTERNAL",
            propertyId = booking.propertyId.toString(),
            propertyName = property.name,
            roomTypeId = booking.roomTypeId.toString(),
            roomTypeName = roomType.name,
            ratePlanId = booking.ratePlanId.toString(),
            ratePlanName = ratePlan.name,
            externalBookingNo = null,
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
            createdAt = booking.createdAt,
        )
    }

    private fun enrichExternal(ext: ExternalBooking): BookingDetailResult =
        BookingDetailResult(
            bookingKey = BookingKeyType.EXTERNAL.key(ext.id),
            source = ext.source,
            propertyId = null,
            propertyName = null,
            roomTypeId = null,
            roomTypeName = null,
            ratePlanId = null,
            ratePlanName = null,
            externalBookingNo = ext.externalBookingNo,
            checkIn = ext.checkIn,
            checkOut = ext.checkOut,
            guestCount = ext.guestCount,
            totalPrice = ext.totalPrice,
            guestName = ext.guestName,
            guestPhone = ext.guestPhone,
            specialRequest = null,
            status = ext.status.name,
            cancelledAt = null,
            cancelReason = null,
            createdAt = ext.createdAt,
        )
}

data class BookingDetailResult(
    val bookingKey: String,
    val source: String,
    val propertyId: String?,
    val propertyName: String?,
    val roomTypeId: String?,
    val roomTypeName: String?,
    val ratePlanId: String?,
    val ratePlanName: String?,
    val externalBookingNo: String?,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val guestCount: Int,
    val totalPrice: BigDecimal?,
    val guestName: String,
    val guestPhone: String?,
    val specialRequest: String?,
    val status: String,
    val cancelledAt: LocalDateTime?,
    val cancelReason: String?,
    val createdAt: LocalDateTime,
)
