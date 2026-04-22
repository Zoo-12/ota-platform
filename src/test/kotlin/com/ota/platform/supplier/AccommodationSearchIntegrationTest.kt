package com.ota.platform.supplier

import com.ota.platform.AbstractIntegrationTest
import com.ota.platform.TestFixtures
import com.ota.platform.supplier.application.AccommodationSearchService
import com.ota.platform.supplier.port.AccommodationSearchQuery
import com.ota.platform.supplier.port.AccommodationSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@DisplayName("통합 검색 통합 테스트")
class AccommodationSearchIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var fixtures: TestFixtures
    @Autowired lateinit var accommodationSearchService: AccommodationSearchService

    private val checkIn = LocalDate.now().plusDays(5)
    private val checkOut = LocalDate.now().plusDays(7)

    @Test
    @Transactional
    @DisplayName("내부 숙소와 외부 Supplier 결과가 통합되어 반환된다")
    fun `통합 검색 - 내부 + Supplier 결과 병합`() {
        // given: 서울에 자사 숙소 1개 등록
        val partner = fixtures.createPartner()
        val property = fixtures.createActiveProperty(partner.id, city = "서울")
        val roomType = fixtures.createRoomType(property.id)
        fixtures.createRatePlan(roomType.id)
        fixtures.createInventoryRange(roomType.id, checkIn, checkOut.minusDays(1), 2)

        // when
        val results = accommodationSearchService.search(
            AccommodationSearchQuery(
                city = "서울",
                checkIn = checkIn,
                checkOut = checkOut,
                guestCount = 1,
            ),
        )

        // then: 자사 숙소 1개 + MockSupplierA 2개 = 3개 이상
        assertThat(results).isNotEmpty
        val sources = results.map { it.source }
        assertThat(sources).contains(AccommodationSource.INTERNAL)
        assertThat(sources).contains(AccommodationSource.SUPPLIER_A)
    }

    @Test
    @Transactional
    @DisplayName("재고가 없는 숙소는 검색 결과에서 제외된다")
    fun `재고 없는 숙소 검색 제외`() {
        val partner = fixtures.createPartner()
        val property = fixtures.createActiveProperty(partner.id, city = "서울")
        val roomType = fixtures.createRoomType(property.id)
        fixtures.createRatePlan(roomType.id)
        fixtures.createInventoryRange(roomType.id, checkIn, checkOut.minusDays(1), totalCount = 0)

        val results = accommodationSearchService.search(
            AccommodationSearchQuery(city = "서울", checkIn = checkIn, checkOut = checkOut, guestCount = 1),
        )

        val internalResults = results.filter { it.source == AccommodationSource.INTERNAL }
        assertThat(internalResults).noneMatch { it.accommodationId == "INTERNAL:${property.id}" }
    }

    @Test
    @Transactional
    @DisplayName("검색 결과는 최저가 기준으로 오름차순 정렬된다")
    fun `검색 결과 최저가 정렬`() {
        val partner = fixtures.createPartner()

        // 비싼 숙소 먼저 등록
        val property1 = fixtures.createActiveProperty(partner.id, city = "서울")
        val roomType1 = fixtures.createRoomType(property1.id)
        fixtures.createRatePlan(roomType1.id, java.math.BigDecimal("300000"))
        fixtures.createInventoryRange(roomType1.id, checkIn, checkOut.minusDays(1), 2)

        // 저렴한 숙소 나중에 등록
        val property2 = fixtures.createActiveProperty(partner.id, city = "서울")
        val roomType2 = fixtures.createRoomType(property2.id)
        fixtures.createRatePlan(roomType2.id, java.math.BigDecimal("80000"))
        fixtures.createInventoryRange(roomType2.id, checkIn, checkOut.minusDays(1), 2)

        val results = accommodationSearchService.search(
            AccommodationSearchQuery(city = "서울", checkIn = checkIn, checkOut = checkOut, guestCount = 1),
        )

        // 최저가 기준 오름차순 정렬 검증
        val prices = results.map { it.minPrice }
        assertThat(prices).isSorted
    }

    @Test
    @Transactional
    @DisplayName("서울 이외 도시는 MockSupplierA 결과가 없다")
    fun `도시 필터 - 부산 검색 시 Supplier 결과 없음`() {
        val results = accommodationSearchService.search(
            AccommodationSearchQuery(city = "부산", checkIn = checkIn, checkOut = checkOut, guestCount = 1),
        )

        val supplierResults = results.filter { it.source == AccommodationSource.SUPPLIER_A }
        assertThat(supplierResults).isEmpty()
    }
}
