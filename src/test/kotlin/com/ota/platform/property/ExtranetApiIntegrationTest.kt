package com.ota.platform.property

import com.ota.platform.AbstractIntegrationTest
import com.ota.platform.TestFixtures
import com.ota.platform.inventory.infrastructure.RoomInventoryRepository
import com.ota.platform.property.application.*
import com.ota.platform.property.domain.BedType
import com.ota.platform.property.domain.CancelPolicy
import com.ota.platform.property.domain.PropertyStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("Extranet API 통합 테스트")
class ExtranetApiIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var fixtures: TestFixtures
    @Autowired lateinit var roomTypeUseCase: RoomTypeUseCase
    @Autowired lateinit var ratePlanUseCase: RatePlanUseCase
    @Autowired lateinit var inventoryUseCase: InventoryUseCase
    @Autowired lateinit var roomInventoryRepository: RoomInventoryRepository

    @Test
    @Transactional
    @DisplayName("파트너가 숙소를 등록하면 PENDING_APPROVAL 상태로 생성된다")
    fun `숙소 등록 시 PENDING_APPROVAL 상태`() {
        val partner = fixtures.createPartner()
        val property = fixtures.createPendingProperty(partner.id)

        assertThat(property.status).isEqualTo(PropertyStatus.PENDING_APPROVAL)
        assertThat(property.partnerId).isEqualTo(partner.id)
    }

    @Test
    @Transactional
    @DisplayName("Admin이 숙소를 승인하면 ACTIVE 상태가 된다")
    fun `숙소 승인 시 ACTIVE 상태`() {
        val partner = fixtures.createPartner()
        val property = fixtures.createActiveProperty(partner.id)

        assertThat(property.status).isEqualTo(PropertyStatus.ACTIVE)
    }

    @Test
    @Transactional
    @DisplayName("객실 타입 등록 시 재고 자동 초기화된다")
    fun `객실 타입 등록 + 재고 초기화`() {
        val partner = fixtures.createPartner()
        val property = fixtures.createActiveProperty(partner.id)

        val from = LocalDate.now().plusDays(1)
        val to = LocalDate.now().plusDays(7)

        val roomTypeId = roomTypeUseCase.register(
            RegisterRoomTypeCommand(
                propertyId = property.id,
                name = "디럭스 룸",
                description = "넓은 객실",
                maxOccupancy = 2,
                bedType = BedType.KING,
                sizeSqm = 35.0,
                amenities = null,
                totalCount = 5,
                initInventoryFrom = from,
                initInventoryTo = to,
            ),
        )

        val inventories = roomInventoryRepository.findAllByRoomTypeIdAndDateBetween(roomTypeId, from, to)
        assertThat(inventories).hasSize(7)
        assertThat(inventories).allMatch { it.totalCount == 5 && it.availableCount == 5 }
    }

    @Test
    @Transactional
    @DisplayName("요금 플랜 등록 후 날짜별 요금 오버라이드가 적용된다")
    fun `날짜별 요금 오버라이드`() {
        val partner = fixtures.createPartner()
        val property = fixtures.createActiveProperty(partner.id)
        val roomType = fixtures.createRoomType(property.id)

        val ratePlanId = ratePlanUseCase.register(
            RegisterRatePlanCommand(
                roomTypeId = roomType.id,
                name = "성수기 요금",
                cancelPolicy = CancelPolicy.NON_REFUNDABLE,
                breakfastIncluded = true,
                basePrice = BigDecimal("200000"),
            ),
        )

        val targetDate = LocalDate.now().plusDays(30)
        ratePlanUseCase.setDailyRate(ratePlanId, targetDate, BigDecimal("300000"))

        val plans = ratePlanUseCase.getByRoomType(roomType.id)
        assertThat(plans).hasSize(1)
        assertThat(plans[0].basePrice).isEqualByComparingTo("200000")
    }

    @Test
    @Transactional
    @DisplayName("재고 일괄 업데이트 - stopSell 설정")
    fun `재고 stopSell 일괄 설정`() {
        val partner = fixtures.createPartner()
        val property = fixtures.createActiveProperty(partner.id)
        val roomType = fixtures.createRoomType(property.id)

        val from = LocalDate.now().plusDays(1)
        val to = LocalDate.now().plusDays(3)
        fixtures.createInventoryRange(roomType.id, from, to, totalCount = 3)

        inventoryUseCase.bulkUpdate(
            BulkUpdateInventoryCommand(
                roomTypeId = roomType.id,
                from = from,
                to = to,
                totalCount = null,
                stopSell = true,
                minStay = null,
                maxStay = null,
            ),
        )

        val inventories = inventoryUseCase.getInventories(roomType.id, from, to)
        assertThat(inventories).allMatch { it.stopSell }
    }
}
