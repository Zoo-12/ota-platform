package com.ota.platform.supplier.adapter

import com.ota.platform.supplier.domain.SupplierAdapterType
import com.ota.platform.supplier.infrastructure.ExternalSupplierRepository
import com.ota.platform.supplier.port.AccommodationDetailResult
import com.ota.platform.supplier.port.AccommodationPort
import com.ota.platform.supplier.port.AccommodationRateResult
import com.ota.platform.supplier.port.AccommodationSearchQuery
import com.ota.platform.supplier.port.AccommodationSearchResult
import com.ota.platform.supplier.port.AccommodationSource
import com.ota.platform.supplier.port.RatePlanDetail
import com.ota.platform.supplier.port.RoomTypeDetail
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 외부 Supplier A Mock 어댑터.
 * DB에서 활성화된 공급사 목록을 조회한 뒤, Supplier A가 켜져있다면 연동을 수행합니다.
 * 실제 연동 시: FeignClient(url = supplierInfo.apiEndpoint) 등의 라이브러리를 통해 외부 서버와 통신합니다.
 * API 요청/응답에 대한 로깅은 이곳에 직접 작성하지 않고, Feign Configuration의
 * RequestInterceptor 또는 Logger 설정을 통해 AOP 단에서 전역적으로 처리합니다.
 */
@Component
class MockSupplierAAdapter(
    private val supplierRepository: ExternalSupplierRepository
) : AccommodationPort {

    override fun canHandle(accommodationId: String) = accommodationId.startsWith(SupplierPrefixes.SUPPLIER_A)

    private fun getActiveSupplierInfo() =
        supplierRepository.findAllByIsActiveTrue()
            .find { it.adapterType == SupplierAdapterType.MOCK_SUPPLIER_A }

    override fun search(query: AccommodationSearchQuery): List<AccommodationSearchResult> {
        val supplierInfo = getActiveSupplierInfo()

        if (supplierInfo == null) {
            return emptyList()
        }

        if (!query.city.contains("서울")) return emptyList()

        return listOf(
            AccommodationSearchResult(
                accommodationId = "${SupplierPrefixes.SUPPLIER_A}MOCK-001",
                name = "[Supplier A] 서울 중심부 호텔",
                category = "HOTEL",
                addressCity = query.city,
                minPrice = BigDecimal("150000"),
                source = AccommodationSource.SUPPLIER_A,
            ),
            AccommodationSearchResult(
                accommodationId = "${SupplierPrefixes.SUPPLIER_A}MOCK-002",
                name = "[Supplier A] 강남 비즈니스 호텔",
                category = "HOTEL",
                addressCity = query.city,
                minPrice = BigDecimal("200000"),
                source = AccommodationSource.SUPPLIER_A,
            ),
        )
    }

    override fun getDetail(accommodationId: String): AccommodationDetailResult {
        val supplierInfo = getActiveSupplierInfo()

        if (supplierInfo == null) {
            throw IllegalStateException("현재 Supplier A의 상세 조회를 이용할 수 없습니다.")
        }

        val mockName = if (accommodationId.endsWith("001")) "[Supplier A] 서울 중심부 호텔" else "[Supplier A] 강남 비즈니스 호텔"
        return AccommodationDetailResult(
            accommodationId = accommodationId,
            name = mockName,
            description = "Supplier A에서 제공하는 숙소입니다.",
            category = "HOTEL",
            addressCity = "서울",
            addressDistrict = null,
            addressDetail = null,
            checkInTime = "15:00",
            checkOutTime = "11:00",
            source = AccommodationSource.SUPPLIER_A,
            roomTypes = listOf(
                RoomTypeDetail(
                    roomTypeId = "${SupplierPrefixes.SUPPLIER_A}ROOM-001",
                    name = "Standard Room",
                    maxOccupancy = 2,
                    bedType = "DOUBLE",
                    sizeSqm = 28.0,
                    ratePlans = listOf(
                        RatePlanDetail("${SupplierPrefixes.SUPPLIER_A}RATE-001", "기본 요금 (무료 취소)", "FREE_CANCEL", false, BigDecimal("150000")),
                        RatePlanDetail("${SupplierPrefixes.SUPPLIER_A}RATE-002", "조식 포함", "NON_REFUNDABLE", true, BigDecimal("170000")),
                    ),
                ),
            ),
        )
    }

    override fun getRates(accommodationId: String, checkIn: LocalDate, checkOut: LocalDate): List<AccommodationRateResult> {
        val supplierInfo = getActiveSupplierInfo()

        if (supplierInfo == null) {
            return emptyList()
        }

        val nights = checkIn.datesUntil(checkOut).count()
        val pricePerNight = BigDecimal("150000")

        return listOf(
            AccommodationRateResult(
                roomTypeId = "${SupplierPrefixes.SUPPLIER_A}ROOM-001",
                roomTypeName = "Standard Room",
                ratePlanId = "${SupplierPrefixes.SUPPLIER_A}RATE-001",
                ratePlanName = "기본 요금 (무료 취소)",
                cancelPolicy = "FREE_CANCEL",
                breakfastIncluded = false,
                pricePerNight = pricePerNight,
                totalPrice = pricePerNight.multiply(BigDecimal(nights)),
                availableCount = 3,
            ),
            AccommodationRateResult(
                roomTypeId = "${SupplierPrefixes.SUPPLIER_A}ROOM-001",
                roomTypeName = "Standard Room",
                ratePlanId = "${SupplierPrefixes.SUPPLIER_A}RATE-002",
                ratePlanName = "조식 포함",
                cancelPolicy = "NON_REFUNDABLE",
                breakfastIncluded = true,
                pricePerNight = pricePerNight.add(BigDecimal("20000")),
                totalPrice = pricePerNight.add(BigDecimal("20000")).multiply(BigDecimal(nights)),
                availableCount = 2,
            ),
        )
    }
}
