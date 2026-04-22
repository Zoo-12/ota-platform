package com.ota.platform.booking

import com.ota.platform.AbstractIntegrationTest
import com.ota.platform.TestFixtures
import com.ota.platform.booking.application.CreateExternalBookingCommand
import com.ota.platform.booking.application.CreateExternalBookingUseCase
import com.ota.platform.booking.application.CreateBookingCommand
import com.ota.platform.booking.application.CreateBookingUseCase
import com.ota.platform.booking.application.GetBookingDetailUseCase
import com.ota.platform.booking.domain.BookingKeyType
import com.ota.platform.booking.infrastructure.ExternalBookingRepository
import com.ota.platform.supplier.port.AccommodationSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("외부 공급사 예약 통합 테스트")
class ExternalBookingIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var fixtures: TestFixtures
    @Autowired lateinit var createExternalBookingUseCase: CreateExternalBookingUseCase
    @Autowired lateinit var createBookingUseCase: CreateBookingUseCase
    @Autowired lateinit var getBookingDetailUseCase: GetBookingDetailUseCase
    @Autowired lateinit var externalBookingRepository: ExternalBookingRepository

    private val checkIn = LocalDate.now().plusDays(5)
    private val checkOut = LocalDate.now().plusDays(7)

    @Test
    @DisplayName("외부 공급사 예약 생성 성공 - 'EXT-{id}' 형식 bookingKey 반환")
    fun `외부 예약 생성 성공`() {
        val customer = fixtures.createCustomer()

        val bookingKey = createExternalBookingUseCase.create(
            CreateExternalBookingCommand(
                customerId = customer.id,
                accommodationId = "SUPPLIER_A:ACC-001",
                checkIn = checkIn,
                checkOut = checkOut,
                guestCount = 2,
                totalPrice = BigDecimal("150000"),
                guestName = "홍길동",
                guestPhone = "010-1234-5678",
            ),
        )

        assertThat(bookingKey).startsWith(BookingKeyType.EXTERNAL.prefix)

        val (type, id) = BookingKeyType.parse(bookingKey)
        assertThat(type).isEqualTo(BookingKeyType.EXTERNAL)

        val saved = externalBookingRepository.findById(id).orElseThrow()
        assertThat(saved.customerId).isEqualTo(customer.id)
        assertThat(saved.source).isEqualTo(AccommodationSource.SUPPLIER_A.name)
        assertThat(saved.externalBookingNo).startsWith("SA-")
        assertThat(saved.status).isEqualTo("CONFIRMED")
    }

    @Test
    @DisplayName("SUPPLIER_A 숙소 예약 시 source = 'SUPPLIER_A', externalBookingNo prefix = 'SA-'")
    fun `SUPPLIER_A 예약 source 및 bookingNo prefix 검증`() {
        val customer = fixtures.createCustomer()

        createExternalBookingUseCase.create(
            CreateExternalBookingCommand(
                customerId = customer.id,
                accommodationId = "SUPPLIER_A:ACC-999",
                checkIn = checkIn,
                checkOut = checkOut,
                guestCount = 1,
                totalPrice = null,
                guestName = "김철수",
                guestPhone = null,
            ),
        )

        val saved = externalBookingRepository.findAllByCustomerId(customer.id).first()
        assertThat(saved.source).isEqualTo(AccommodationSource.SUPPLIER_A.name)
        assertThat(saved.externalBookingNo).matches("SA-[A-Z0-9]{8}")
    }

    @Test
    @DisplayName("getById - EXT- 키로 외부 예약 상세 조회 성공")
    fun `외부 예약 getById 조회`() {
        val customer = fixtures.createCustomer()

        val bookingKey = createExternalBookingUseCase.create(
            CreateExternalBookingCommand(
                customerId = customer.id,
                accommodationId = "SUPPLIER_A:ACC-001",
                checkIn = checkIn,
                checkOut = checkOut,
                guestCount = 2,
                totalPrice = BigDecimal("200000"),
                guestName = "이영희",
                guestPhone = "010-9999-0000",
            ),
        )

        val detail = getBookingDetailUseCase.getById(bookingKey)

        assertThat(detail.bookingKey).isEqualTo(bookingKey)
        assertThat(detail.source).isEqualTo(AccommodationSource.SUPPLIER_A.name)
        assertThat(detail.externalBookingNo).startsWith("SA-")
        assertThat(detail.guestName).isEqualTo("이영희")
        assertThat(detail.propertyName).isNull()
        assertThat(detail.roomTypeName).isNull()
    }

    @Test
    @DisplayName("getByCustomer - 내부 예약과 외부 예약이 함께 조회되고 createdAt 내림차순 정렬")
    fun `고객 예약 목록 - 내부 외부 통합 조회`() {
        val customer = fixtures.createCustomer()

        // 내부 예약 생성
        val partner = fixtures.createPartner()
        val property = fixtures.createActiveProperty(partner.id)
        val roomType = fixtures.createRoomType(property.id)
        val ratePlan = fixtures.createRatePlan(roomType.id)
        fixtures.createInventoryRange(roomType.id, checkIn, checkOut.minusDays(1))

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

        // 외부 예약 생성
        createExternalBookingUseCase.create(
            CreateExternalBookingCommand(
                customerId = customer.id,
                accommodationId = "SUPPLIER_A:ACC-001",
                checkIn = checkIn,
                checkOut = checkOut,
                guestCount = 1,
                totalPrice = BigDecimal("100000"),
                guestName = "홍길동",
                guestPhone = null,
            ),
        )

        val results = getBookingDetailUseCase.getByCustomer(customer.id)

        assertThat(results).hasSize(2)
        assertThat(results.map { it.source }).containsExactlyInAnyOrder(AccommodationSource.INTERNAL.name, AccommodationSource.SUPPLIER_A.name)

        // createdAt 내림차순 정렬 검증
        val createdAts = results.map { it.createdAt }
        assertThat(createdAts).isSortedAccordingTo(Comparator.reverseOrder())
    }
}
