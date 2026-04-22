package com.ota.platform.booking

import com.ota.platform.AbstractIntegrationTest
import com.ota.platform.TestFixtures
import com.ota.platform.booking.application.CancelBookingCommand
import com.ota.platform.booking.application.CancelBookingUseCase
import com.ota.platform.booking.application.CreateBookingCommand
import com.ota.platform.booking.application.CreateBookingUseCase
import com.ota.platform.booking.domain.BookingStatus
import com.ota.platform.booking.infrastructure.BookingRepository
import com.ota.platform.common.exception.BadRequestException
import com.ota.platform.common.exception.ConflictException
import com.ota.platform.inventory.infrastructure.RoomInventoryRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@DisplayName("예약 통합 테스트")
class BookingIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var fixtures: TestFixtures
    @Autowired lateinit var createBookingUseCase: CreateBookingUseCase
    @Autowired lateinit var cancelBookingUseCase: CancelBookingUseCase
    @Autowired lateinit var bookingRepository: BookingRepository
    @Autowired lateinit var roomInventoryRepository: RoomInventoryRepository

    private val checkIn = LocalDate.now().plusDays(10)
    private val checkOut = LocalDate.now().plusDays(12) // 2박

    @Test
    @Transactional
    @DisplayName("예약 생성 성공 - 재고가 차감되고 BookingRoom이 날짜별로 생성된다")
    fun `예약 생성 성공`() {
        val (customerId, roomTypeId, ratePlanId) = setupScenario()

        val bookingId = createBookingUseCase.create(
            CreateBookingCommand(
                customerId = customerId,
                roomTypeId = roomTypeId,
                ratePlanId = ratePlanId,
                checkIn = checkIn,
                checkOut = checkOut,
                guestCount = 2,
                guestName = "홍길동",
                guestPhone = "010-0000-0000",
                specialRequest = null,
            ),
        )

        val booking = bookingRepository.findById(bookingId).get()
        assertThat(booking.status).isEqualTo(BookingStatus.CONFIRMED)
        assertThat(booking.bookingRooms).hasSize(2) // 2박 → 2개 날짜
        assertThat(booking.totalPrice.toInt()).isEqualTo(200000) // 100000 * 2박

        // 재고가 차감됐는지 확인
        val inventories = roomInventoryRepository.findAllByRoomTypeIdAndDateBetween(
            roomTypeId, checkIn, checkOut.minusDays(1),
        )
        assertThat(inventories).allMatch { it.availableCount == 0 }
    }

    @Test
    @Transactional
    @DisplayName("예약 취소 성공 - 상태가 CANCELLED로 변경되고 재고가 복원된다")
    fun `예약 취소 성공`() {
        val (customerId, roomTypeId, ratePlanId) = setupScenario()

        val bookingId = createBookingUseCase.create(
            CreateBookingCommand(
                customerId = customerId,
                roomTypeId = roomTypeId,
                ratePlanId = ratePlanId,
                checkIn = checkIn,
                checkOut = checkOut,
                guestCount = 1,
                guestName = "홍길동",
                guestPhone = null,
                specialRequest = null,
            ),
        )

        cancelBookingUseCase.cancel(
            CancelBookingCommand(
                customerId = customerId,
                bookingId = bookingId,
                reason = "개인 사정",
            ),
        )

        val booking = bookingRepository.findById(bookingId).get()
        assertThat(booking.status).isEqualTo(BookingStatus.CANCELLED)
        assertThat(booking.cancelReason).isEqualTo("개인 사정")
        assertThat(booking.cancelledAt).isNotNull()

        // 재고가 복원됐는지 확인
        val inventories = roomInventoryRepository.findAllByRoomTypeIdAndDateBetween(
            roomTypeId, checkIn, checkOut.minusDays(1),
        )
        assertThat(inventories).allMatch { it.availableCount == 1 }
    }

    @Test
    @DisplayName("이미 취소된 예약을 다시 취소하면 예외가 발생한다")
    fun `중복 취소 시 예외`() {
        val (customerId, roomTypeId, ratePlanId) = setupScenario()

        val bookingId = createBookingUseCase.create(
            CreateBookingCommand(
                customerId = customerId,
                roomTypeId = roomTypeId,
                ratePlanId = ratePlanId,
                checkIn = checkIn,
                checkOut = checkOut,
                guestCount = 1,
                guestName = "홍길동",
                guestPhone = null,
                specialRequest = null,
            ),
        )

        val command = CancelBookingCommand(customerId = customerId, bookingId = bookingId, reason = null)
        cancelBookingUseCase.cancel(command)

        assertThatThrownBy { cancelBookingUseCase.cancel(command) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("이미 취소된 예약")
    }

    @Test
    @DisplayName("재고가 없을 때 예약하면 ConflictException이 발생한다")
    fun `재고 없을 때 예약 실패`() {
        val (customerId, roomTypeId, ratePlanId) = setupScenario(totalCount = 0)

        assertThatThrownBy {
            createBookingUseCase.create(
                CreateBookingCommand(
                    customerId = customerId,
                    roomTypeId = roomTypeId,
                    ratePlanId = ratePlanId,
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
    @DisplayName("체크아웃이 체크인보다 이전이면 BadRequestException이 발생한다")
    fun `잘못된 날짜 예약 실패`() {
        val (customerId, roomTypeId, ratePlanId) = setupScenario()

        assertThatThrownBy {
            createBookingUseCase.create(
                CreateBookingCommand(
                    customerId = customerId,
                    roomTypeId = roomTypeId,
                    ratePlanId = ratePlanId,
                    checkIn = checkIn,
                    checkOut = checkIn, // 같은 날짜
                    guestCount = 1,
                    guestName = "홍길동",
                    guestPhone = null,
                    specialRequest = null,
                ),
            )
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("체크아웃")
    }

    private data class TestScenario(val customerId: Long, val roomTypeId: Long, val ratePlanId: Long)

    private fun setupScenario(totalCount: Int = 1): TestScenario {
        val partner = fixtures.createPartner()
        val property = fixtures.createActiveProperty(partner.id)
        val roomType = fixtures.createRoomType(property.id)
        val ratePlan = fixtures.createRatePlan(roomType.id)
        fixtures.createInventoryRange(roomType.id, checkIn, checkOut.minusDays(1), totalCount)
        val customer = fixtures.createCustomer()
        return TestScenario(customer.id, roomType.id, ratePlan.id)
    }
}
