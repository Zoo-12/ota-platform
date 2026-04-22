package com.ota.platform.booking.application

import com.ota.platform.booking.domain.BookingKeyType
import com.ota.platform.booking.domain.ExternalBooking
import com.ota.platform.booking.infrastructure.ExternalBookingRepository
import com.ota.platform.supplier.adapter.SupplierPrefixes
import com.ota.platform.supplier.port.AccommodationSource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * 외부 공급사 예약 생성 UseCase.
 * accommodationId의 prefix를 기반으로 공급사를 판별하고,
 * ExternalBooking 레코드(외부 예약 번호 포함)를 저장한다.
 */
@Service
class CreateExternalBookingUseCase(
    private val externalBookingRepository: ExternalBookingRepository,
) {
    @Transactional
    fun create(command: CreateExternalBookingCommand): String {
        val source = resolveSource(command.accommodationId)
        val bookingNoPrefix = resolveBookingNoPrefix(source)
        val externalBookingNo = "$bookingNoPrefix-${UUID.randomUUID().toString().take(8).uppercase()}"

        val saved = externalBookingRepository.save(
            ExternalBooking(
                customerId = command.customerId,
                accommodationId = command.accommodationId,
                externalBookingNo = externalBookingNo,
                source = source,
                checkIn = command.checkIn,
                checkOut = command.checkOut,
                guestCount = command.guestCount,
                totalPrice = command.totalPrice,
                guestName = command.guestName,
                guestPhone = command.guestPhone,
            ),
        )
        return BookingKeyType.EXTERNAL.key(saved.id)
    }

    private fun resolveSource(accommodationId: String): String = when {
        accommodationId.startsWith(SupplierPrefixes.SUPPLIER_A) -> AccommodationSource.SUPPLIER_A.name
        else -> "SUPPLIER_UNKNOWN"
    }

    private fun resolveBookingNoPrefix(source: String): String = when (source) {
        AccommodationSource.SUPPLIER_A.name -> "SA"
        else -> "EXT"
    }
}

data class CreateExternalBookingCommand(
    val customerId: Long,
    val accommodationId: String,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val guestCount: Int,
    val totalPrice: BigDecimal?,
    val guestName: String,
    val guestPhone: String?,
)
