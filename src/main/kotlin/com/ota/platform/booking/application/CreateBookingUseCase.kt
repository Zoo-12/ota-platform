package com.ota.platform.booking.application

import com.ota.platform.booking.domain.Booking
import com.ota.platform.booking.domain.BookingRoom
import com.ota.platform.booking.infrastructure.BookingRepository
import com.ota.platform.booking.infrastructure.CustomerRepository
import com.ota.platform.common.exception.BadRequestException
import com.ota.platform.common.exception.NotFoundException
import com.ota.platform.inventory.domain.RoomInventoryService
import com.ota.platform.property.domain.RateCalculationService
import com.ota.platform.property.infrastructure.RatePlanRepository
import com.ota.platform.property.infrastructure.RoomTypeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class CreateBookingUseCase(
    private val bookingRepository: BookingRepository,
    private val customerRepository: CustomerRepository,
    private val roomTypeRepository: RoomTypeRepository,
    private val ratePlanRepository: RatePlanRepository,
    private val roomInventoryService: RoomInventoryService,
    private val rateCalculationService: RateCalculationService,
) {

    /**
     * 예약 생성 핵심 흐름:
     * 1. 입력값 유효성 검증
     * 2. 재고 비관적 락 획득 + 차감 (RoomInventoryService)
     * 3. 요금 계산 및 스냅샷 저장
     * 4. Booking + BookingRoom 저장
     */
    @Transactional
    fun create(command: CreateBookingCommand): Long {
        validateDates(command.checkIn, command.checkOut)

        val customer = customerRepository.findById(command.customerId)
            .orElseThrow { NotFoundException("Customer", command.customerId) }

        val roomType = roomTypeRepository.findById(command.roomTypeId)
            .orElseThrow { NotFoundException("RoomType", command.roomTypeId) }

        val ratePlan = ratePlanRepository.findById(command.ratePlanId)
            .orElseThrow { NotFoundException("RatePlan", command.ratePlanId) }

        if (ratePlan.roomTypeId != roomType.id) {
            throw BadRequestException("요금 플랜이 해당 객실 타입에 속하지 않습니다.")
        }

        // 요금 계산 (날짜별 스냅샷 포함)
        val priceBreakdown = rateCalculationService.calculateTotalPrice(ratePlan, command.checkIn, command.checkOut)

        // 재고 비관적 락 획득 + 차감 — 동시 예약 시 두 번째 요청은 여기서 대기
        val inventories = roomInventoryService.decreaseInventories(roomType.id, command.checkIn, command.checkOut)

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
                    priceSnapshot = priceBreakdown.priceByDate[date]
                        ?: ratePlan.basePrice,
                ),
            )
        }

        return bookingRepository.save(booking).id
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
