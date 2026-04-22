package com.ota.platform.booking

import com.ota.platform.AbstractIntegrationTest
import com.ota.platform.TestFixtures
import com.ota.platform.booking.application.CreateBookingCommand
import com.ota.platform.booking.application.CreateBookingUseCase
import com.ota.platform.booking.domain.BookingStatus
import com.ota.platform.booking.infrastructure.BookingRepository
import com.ota.platform.inventory.infrastructure.RoomInventoryRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * 동시 예약 통합 테스트.
 *
 * 비관적 락(SELECT FOR UPDATE)이 초과 예약을 방지하는지 검증.
 * @Transactional 없이 실행 — 각 스레드가 독립 트랜잭션을 가져야 함.
 */
@DisplayName("동시 예약 통합 테스트 (비관적 락)")
class ConcurrentBookingIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var fixtures: TestFixtures
    @Autowired lateinit var createBookingUseCase: CreateBookingUseCase
    @Autowired lateinit var bookingRepository: BookingRepository
    @Autowired lateinit var roomInventoryRepository: RoomInventoryRepository

    @Test
    @DisplayName("재고 1개에 10명이 동시에 예약하면 정확히 1건만 성공한다")
    fun `동시 예약 - 재고 1개에 10명 요청 시 1건만 성공`() {
        // given
        val checkIn = LocalDate.now().plusDays(20)
        val checkOut = LocalDate.now().plusDays(21)
        val totalInventory = 1
        val concurrentRequests = 10

        val partner = fixtures.createPartner()
        val property = fixtures.createActiveProperty(partner.id)
        val roomType = fixtures.createRoomType(property.id)
        val ratePlan = fixtures.createRatePlan(roomType.id)
        fixtures.createInventoryRange(roomType.id, checkIn, checkOut.minusDays(1), totalInventory)

        val customers = (1..concurrentRequests).map { fixtures.createCustomer() }

        // when — 10개 스레드 동시 예약 시작
        val executor = Executors.newFixedThreadPool(concurrentRequests)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(concurrentRequests)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        customers.forEach { customer ->
            executor.submit {
                try {
                    startLatch.await()
                    createBookingUseCase.create(
                        CreateBookingCommand(
                            customerId = customer.id,
                            roomTypeId = roomType.id,
                            ratePlanId = ratePlan.id,
                            checkIn = checkIn,
                            checkOut = checkOut,
                            guestCount = 1,
                            guestName = customer.name,
                            guestPhone = null,
                            specialRequest = null,
                        ),
                    )
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        doneLatch.await()
        executor.shutdown()

        // then
        val confirmedBookings = bookingRepository.findAll()
            .filter { it.roomTypeId == roomType.id && it.status == BookingStatus.CONFIRMED }

        assertThat(successCount.get())
            .withFailMessage("재고 1개에 성공한 예약이 1건이어야 합니다. 실제: ${successCount.get()}")
            .isEqualTo(totalInventory)

        assertThat(failCount.get())
            .withFailMessage("나머지 ${concurrentRequests - totalInventory}건은 실패해야 합니다.")
            .isEqualTo(concurrentRequests - totalInventory)

        assertThat(confirmedBookings).hasSize(totalInventory)

        // 재고 available_count 가 정확히 0인지 확인
        val inventories = roomInventoryRepository.findAllByRoomTypeIdAndDateBetween(
            roomType.id, checkIn, checkOut.minusDays(1),
        )
        assertThat(inventories).allMatch { it.availableCount == 0 }
    }

    @Test
    @DisplayName("재고 3개에 10명이 동시에 예약하면 정확히 3건만 성공한다")
    fun `동시 예약 - 재고 3개에 10명 요청 시 3건만 성공`() {
        // given
        val checkIn = LocalDate.now().plusDays(30)
        val checkOut = LocalDate.now().plusDays(32)
        val totalInventory = 3
        val concurrentRequests = 10

        val partner = fixtures.createPartner()
        val property = fixtures.createActiveProperty(partner.id)
        val roomType = fixtures.createRoomType(property.id)
        val ratePlan = fixtures.createRatePlan(roomType.id)
        fixtures.createInventoryRange(roomType.id, checkIn, checkOut.minusDays(1), totalInventory)

        val customers = (1..concurrentRequests).map { fixtures.createCustomer() }

        // when
        val executor = Executors.newFixedThreadPool(concurrentRequests)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(concurrentRequests)
        val successCount = AtomicInteger(0)

        customers.forEach { customer ->
            executor.submit {
                try {
                    startLatch.await()
                    createBookingUseCase.create(
                        CreateBookingCommand(
                            customerId = customer.id,
                            roomTypeId = roomType.id,
                            ratePlanId = ratePlan.id,
                            checkIn = checkIn,
                            checkOut = checkOut,
                            guestCount = 1,
                            guestName = customer.name,
                            guestPhone = null,
                            specialRequest = null,
                        ),
                    )
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    // 재고 부족으로 인한 실패 — 정상
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        doneLatch.await()
        executor.shutdown()

        // then
        assertThat(successCount.get())
            .withFailMessage("재고 3개에 성공한 예약이 3건이어야 합니다. 실제: ${successCount.get()}")
            .isEqualTo(totalInventory)

        val inventories = roomInventoryRepository.findAllByRoomTypeIdAndDateBetween(
            roomType.id, checkIn, checkOut.minusDays(1),
        )
        assertThat(inventories).allMatch { it.availableCount == 0 }
    }
}
