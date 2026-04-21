package com.ota.platform.booking.application

import com.ota.platform.booking.domain.Booking
import com.ota.platform.booking.domain.BookingRoom
import com.ota.platform.booking.event.BookingCreatedEvent
import com.ota.platform.booking.infrastructure.BookingRepository
import com.ota.platform.booking.infrastructure.CustomerRepository
import com.ota.platform.booking.port.InventoryPort
import com.ota.platform.booking.port.RatePlanPort
import com.ota.platform.booking.port.RoomTypePort
import com.ota.platform.common.exception.BadRequestException
import com.ota.platform.common.exception.NotFoundException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class CreateBookingUseCase(
    private val bookingRepository: BookingRepository,
    private val customerRepository: CustomerRepository,
    private val roomTypePort: RoomTypePort,
    private val ratePlanPort: RatePlanPort,
    private val inventoryPort: InventoryPort,
    private val eventPublisher: ApplicationEventPublisher,
) {

    /**
     * 예약 생성 핵심 흐름:
     * 1. 입력값 유효성 검증
     * 2. 재고 비관적 락 획득 + 차감 (InventoryPort)
     * 3. 요금 계산 및 스냅샷 저장
     * 4. Booking + BookingRoom 저장
     */
    @Transactional
    fun create(command: CreateBookingCommand): Long {
        validateDates(command.checkIn, command.checkOut)

        val customer = customerRepository.findById(command.customerId)
            .orElseThrow { NotFoundException("Customer", command.customerId) }

        val roomType = roomTypePort.getById(command.roomTypeId)
        val ratePlan = ratePlanPort.getById(command.ratePlanId)

        if (ratePlan.roomTypeId != roomType.id) {
            throw BadRequestException("요금 플랜이 해당 객실 타입에 속하지 않습니다.")
        }

        // 요금 계산 (날짜별 스냅샷 포함)
        val priceBreakdown = ratePlanPort.calculateTotalPrice(ratePlan.id, command.checkIn, command.checkOut)

        // 재고 비관적 락 획득 + 차감 — 동시 예약 시 두 번째 요청은 여기서 대기
        val inventories = inventoryPort.decrease(roomType.id, command.checkIn, command.checkOut)

        val booking = Booking(
            customerId = customer.id,
            propertyId = roomType.propertyId,
            roomTypeId = roomType.id,
            ratePlanId = ratePlan.id,
            checkIn = command.checkIn,
            checkOut = command.checkOut,
            guestCount = command.guestCount,
            totalPrice = priceBreakdown.totalPrice,
            guestName = command.guestName,
            guestPhone = command.guestPhone,
            specialRequest = command.specialRequest,
        )

        // 날짜별 BookingRoom 생성 (재고 ID + 가격 스냅샷)
        val inventoryById = inventories.associateBy { it.date }
        command.checkIn.datesUntil(command.checkOut).forEach { date ->
            val inventory = inventoryById[date] ?: throw BadRequestException("재고 정보가 없습니다: $date")
            booking.bookingRooms.add(
                BookingRoom(
                    booking = booking,
                    roomInventoryId = inventory.id,
                    date = date,
                    priceSnapshot = priceBreakdown.priceByDate[date] ?: ratePlan.basePrice,
                ),
            )
        }

        val savedBooking = bookingRepository.save(booking)

        // 트랜잭션 커밋 후 이벤트 발행 (@TransactionalEventListener AFTER_COMMIT)
        eventPublisher.publishEvent(
            BookingCreatedEvent(
                bookingId = savedBooking.id,
                customerId = savedBooking.customerId,
                propertyId = savedBooking.propertyId,
                roomTypeId = savedBooking.roomTypeId,
                guestName = savedBooking.guestName,
                checkIn = savedBooking.checkIn,
                checkOut = savedBooking.checkOut,
                totalPrice = savedBooking.totalPrice,
            ),
        )

        return savedBooking.id
    }

    private fun validateDates(checkIn: LocalDate, checkOut: LocalDate) {
        if (!checkOut.isAfter(checkIn)) throw BadRequestException("체크아웃은 체크인 이후여야 합니다.")
        if (checkIn.isBefore(LocalDate.now())) throw BadRequestException("체크인 날짜는 오늘 이후여야 합니다.")
    }
}

data class CreateBookingCommand(
    val customerId: Long,
    val roomTypeId: Long,
    val ratePlanId: Long,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val guestCount: Int,
    val guestName: String,
    val guestPhone: String?,
    val specialRequest: String?,
)
