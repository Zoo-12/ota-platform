package com.ota.platform.supplier.adapter

import com.ota.platform.supplier.port.AccommodationPort
import com.ota.platform.supplier.port.AccommodationRateResult
import com.ota.platform.supplier.port.AccommodationSearchQuery
import com.ota.platform.supplier.port.AccommodationSearchResult
import com.ota.platform.supplier.port.AccommodationSource
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 외부 Supplier A Mock 어댑터.
 * 실제 외부 API 연동 대신 고정 데이터를 반환하여 통합 검색 흐름을 검증한다.
 * 실제 환경에서는 Feign Client 등으로 외부 API 호출로 대체.
 */
@Component
class MockSupplierAAdapter : AccommodationPort {

    override fun search(query: AccommodationSearchQuery): List<AccommodationSearchResult> {
        // Mock: 서울 검색 시에만 결과 반환
        if (!query.city.contains("서울")) return emptyList()

        return listOf(
            AccommodationSearchResult(
                accommodationId = "SUPPLIER_A:MOCK-001",
                name = "[Supplier A] 서울 중심부 호텔",
                category = "HOTEL",
                addressCity = query.city,
                minPrice = BigDecimal("150000"),
                source = AccommodationSource.SUPPLIER_A,
            ),
            AccommodationSearchResult(
                accommodationId = "SUPPLIER_A:MOCK-002",
                name = "[Supplier A] 강남 비즈니스 호텔",
                category = "HOTEL",
                addressCity = query.city,
                minPrice = BigDecimal("200000"),
                source = AccommodationSource.SUPPLIER_A,
            ),
        )
    }

    override fun getRates(accommodationId: String, checkIn: LocalDate, checkOut: LocalDate): List<AccommodationRateResult> {
        val nights = checkIn.datesUntil(checkOut).count()
        val pricePerNight = BigDecimal("150000")

        return listOf(
            AccommodationRateResult(
                roomTypeId = "SUPPLIER_A:ROOM-001",
                roomTypeName = "Standard Room",
                ratePlanId = "SUPPLIER_A:RATE-001",
                ratePlanName = "기본 요금 (무료 취소)",
                cancelPolicy = "FREE_CANCEL",
                breakfastIncluded = false,
                pricePerNight = pricePerNight,
                totalPrice = pricePerNight.multiply(BigDecimal(nights)),
                availableCount = 3,
            ),
            AccommodationRateResult(
                roomTypeId = "SUPPLIER_A:ROOM-001",
                roomTypeName = "Standard Room",
                ratePlanId = "SUPPLIER_A:RATE-002",
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
