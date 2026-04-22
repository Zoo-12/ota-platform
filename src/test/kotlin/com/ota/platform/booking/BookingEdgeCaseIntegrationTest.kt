package com.ota.platform.booking

import com.ota.platform.AbstractIntegrationTest
import com.ota.platform.TestFixtures
import com.ota.platform.booking.application.CreateBookingCommand
import com.ota.platform.booking.application.CreateBookingUseCase
import com.ota.platform.booking.infrastructure.BookingRepository
import com.ota.platform.common.exception.BadRequestException
import com.ota.platform.common.exception.ConflictException
import com.ota.platform.property.application.BulkUpdateInventoryCommand
import com.ota.platform.property.application.InventoryUseCase
import com.ota.platform.property.application.RatePlanUseCase
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 예약 엣지 케이스 통합 테스트.
 *
 * - stopSell=true 날짜에 예약 시도 → ConflictException
 * - DailyRate 오버라이드 요금이 예약 totalPrice 및 priceSnapshot에 반영되는지
 * - ratePlan이 해당 roomType 소속이 아닐 때 → BadRequestException
 */
@DisplayName("예약 엣지 케이스 통합 테스트")
class BookingEdgeCaseIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var fixtures: TestFixtures
    @Autowired lateinit var createBookingUseCase: CreateBookingUseCase
    @Autowired lateinit var inventoryUseCase: InventoryUseCase
    @Autowired lateinit var ratePlanUseCase: RatePlanUseCase
    @Autowired lateinit var bookingRepository: BookingRepository

    private val checkIn: LocalDate = LocalDate.now().plusDays(50)
    private val checkOut: LocalDate = LocalDate.now().plusDays(52) // 2박

    @Test
    @DisplayName("stopSell=true인 날짜를 포함한 예약 요청은 ConflictException이 발생한다")
    fun `stopSell 날짜 예약 시 ConflictException`() {
        val partner = fixtures.createPartner()
        val property = fixtures.createActiveProperty(partner.id)
        val roomType = fixtures.createRoomType(property.id)
        val ratePlan = fixtures.createRatePlan(roomType.id)
        val customer = fixtures.createCustomer()

        // 재고 생성 후 전체 기간 stopSell 설정
        fixtures.createInventoryRange(roomType.id, checkIn, checkOut.minusDays(1), totalCount = 3)
        inventoryUseCase.bulkUpdate(
            BulkUpdateInventoryCommand(
                roomTypeId = roomType.id,
                from = checkIn,
                to = checkOut.minusDays(1),
                totalCount = null,
                stopSell = true,
                minStay = null,
                maxStay = null,
            ),
        )

        assertThatThrownBy {
            createBookingUseCase.create(
                CreateBookingCommand(
                    customerId = customer.id,
                    roomTypeId = roomType.id,
                    ratePlanId = ratePlan.id,
                    checkIn = checkIn,
                    checkOut = checkOut,
                    guestCount = 1,
                    guestName = "홍길동",
                    guestPhone = null,
                    specialRequest = null,
                ),
            )
        }.isInstanceOf(ConflictException::class.java)
    }

    @Test
    @DisplayName("stopSell=true였다가 false로 해제하면 예약이 정상 생성된다")
    fun `stopSell 해제 후 예약 성공`() {
        val partner = fixtures.createPartner()
        val property = fixtures.createActiveProperty(partner.id)
        val roomType = fixtures.createRoomType(property.id)
        val ratePlan = fixtures.createRatePlan(roomType.id)
        val customer = fixtures.createCustomer()

        fixtures.createInventoryRange(roomType.id, checkIn, checkOut.minusDays(1), totalCount = 2)

        // stopSell 설정
        inventoryUseCase.bulkUpdate(
            BulkUpdateInventoryCommand(
                roomTypeId = roomType.id,
                from = checkIn,
                to = checkOut.minusDays(1),
                totalCount = null,
                stopSell = true,
                minStay = null,
                maxStay = null,
            ),
        )

        // stopSell 해제
        inventoryUseCase.bulkUpdate(
            BulkUpdateInventoryCommand(
                roomTypeId = roomType.id,
                from = checkIn,
                to = checkOut.minusDays(1),
                totalCount = null,
                stopSell = false,
                minStay = null,
                maxStay = null,
            ),
        )

        val bookingId = createBookingUseCase.create(
            CreateBookingCommand(
                customerId = customer.id,
                roomTypeId = roomType.id,
                ratePlanId = ratePlan.id,
                checkIn = checkIn,
                checkOut = checkOut,
                guestCount = 1,
                guestName = "홍길동",
                guestPhone = null,
                specialRequest = null,
            ),
        )

        val booking = bookingRepository.findById(bookingId).get()
        assertThat(booking).isNotNull
    }

    @Test
    @Transactional
    @DisplayName("DailyRate로 오버라이드된 날짜의 요금이 예약 totalPrice와 priceSnapshot에 반영된다")
    fun `DailyRate 오버라이드 요금이 예약에 반영`() {
        val partner = fixtures.createPartner()
        val property = fixtures.createActiveProperty(partner.id)
        val roomType = fixtures.createRoomType(property.id)
        // basePrice = 100,000원
        val ratePlan = fixtures.createRatePlan(roomType.id, BigDecimal("100000"))
        val customer = fixtures.createCustomer()
        fixtures.createInventoryRange(roomType.id, checkIn, checkOut.minusDays(1), totalCount = 2)

        // checkIn 날짜만 150,000원으로 오버라이드
        // checkOut-1 날짜는 basePrice 그대로 100,000원
        ratePlanUseCase.setDailyRate(ratePlan.id, checkIn, BigDecimal("150000"))

        // 예상 총액: 150,000 + 100,000 = 250,000
        val bookingId = createBookingUseCase.create(
            CreateBookingCommand(
                customerId = customer.id,
                roomTypeId = roomType.id,
                ratePlanId = ratePlan.id,
                checkIn = checkIn,
                checkOut = checkOut,
                guestCount = 1,
                guestName = "홍길동",
                guestPhone = null,
                specialRequest = null,
            ),
        )

        val booking = bookingRepository.findById(bookingId).get()
        assertThat(booking.totalPrice).isEqualByComparingTo("250000")

        // 날짜별 priceSnapshot도 각각 반영됐는지 확인
        val snapshots = booking.bookingRooms.sortedBy { it.date }.map { it.priceSnapshot }
        assertThat(snapshots[0]).isEqualByComparingTo("150000") // checkIn: DailyRate
        assertThat(snapshots[1]).isEqualByComparingTo("100000") // checkOut-1: basePrice
    }

    @Test
    @Transactional
    @DisplayName("DailyRate가 없으면 모든 날짜에 basePrice가 그대로 적용된다")
    fun `DailyRate 없으면 basePrice 적용`() {
        val partner = fixtures.createPartner()
        val property = fixtures.createActiveProperty(partner.id)
        val roomType = fixtures.createRoomType(property.id)
        val ratePlan = fixtures.createRatePlan(roomType.id, BigDecimal("80000"))
        val customer = fixtures.createCustomer()
        fixtures.createInventoryRange(roomType.id, checkIn, checkOut.minusDays(1), totalCount = 2)

        // DailyRate 설정 없음
        val bookingId = createBookingUseCase.create(
            CreateBookingCommand(
                customerId = customer.id,
                roomTypeId = roomType.id,
                ratePlanId = ratePlan.id,
                checkIn = checkIn,
                checkOut = checkOut,
                guestCount = 1,
                guestName = "홍길동",
                guestPhone = null,
                specialRequest = null,
            ),
        )

        val booking = bookingRepository.findById(bookingId).get()
        // 80,000 * 2박 = 160,000
        assertThat(booking.totalPrice).isEqualByComparingTo("160000")
        assertThat(booking.bookingRooms).allMatch {
            it.priceSnapshot.compareTo(BigDecimal("80000")) == 0
        }
    }

    @Test
    @DisplayName("ratePlan이 해당 roomType 소속이 아니면 BadRequestException이 발생한다")
    fun `다른 roomType의 ratePlan으로 예약 시 BadRequestException`() {
        val partner = fixtures.createPartner()
        val property = fixtures.createActiveProperty(partner.id)
        val customer = fixtures.createCustomer()

        // 객실 타입 A 와 B 각각 생성
        val roomTypeA = fixtures.createRoomType(property.id)
        val roomTypeB = fixtures.createRoomType(property.id)

        // B의 요금 플랜을 A 객실 예약에 사용
        val ratePlanOfB = fixtures.createRatePlan(roomTypeB.id)
        fixtures.createInventoryRange(roomTypeA.id, checkIn, checkOut.minusDays(1), totalCount = 2)

        assertThatThrownBy {
            createBookingUseCase.create(
                CreateBookingCommand(
                    customerId = customer.id,
                    roomTypeId = roomTypeA.id,
                    ratePlanId = ratePlanOfB.id, // 다른 roomType의 ratePlan
                    checkIn = checkIn,
                    checkOut = checkOut,
                    guestCount = 1,
                    guestName = "홍길동",
                    guestPhone = null,
                    specialRequest = null,
                ),
            )
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("객실 타입")
    }
}
