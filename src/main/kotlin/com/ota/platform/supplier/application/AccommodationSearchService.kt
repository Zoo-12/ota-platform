package com.ota.platform.supplier.application

import com.ota.platform.supplier.adapter.InternalAccommodationAdapter
import com.ota.platform.supplier.adapter.MockSupplierAAdapter
import com.ota.platform.supplier.port.AccommodationPort
import com.ota.platform.supplier.port.AccommodationRateResult
import com.ota.platform.supplier.port.AccommodationSearchQuery
import com.ota.platform.supplier.port.AccommodationSearchResult
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * 내부 숙소 + 외부 Supplier 통합 검색 서비스.
 * 각 어댑터의 결과를 병합하여 최저가 기준 정렬 후 반환.
 */
@Service
class AccommodationSearchService(
    private val internalAdapter: InternalAccommodationAdapter,
    private val mockSupplierAAdapter: MockSupplierAAdapter,
) {
    private val adapters: List<AccommodationPort>
        get() = listOf(internalAdapter, mockSupplierAAdapter)

    fun search(query: AccommodationSearchQuery): List<AccommodationSearchResult> {
        return adapters
            .flatMap { adapter ->
                runCatching { adapter.search(query) }.getOrElse { emptyList() }
            }
            .sortedBy { it.minPrice }
    }

    fun getRates(accommodationId: String, checkIn: LocalDate, checkOut: LocalDate): List<AccommodationRateResult> {
        val adapter = resolveAdapter(accommodationId)
        return adapter.getRates(accommodationId, checkIn, checkOut)
            .sortedBy { it.totalPrice }
    }

    private fun resolveAdapter(accommodationId: String): AccommodationPort = when {
        accommodationId.startsWith("INTERNAL:") -> internalAdapter
        accommodationId.startsWith("SUPPLIER_A:") -> mockSupplierAAdapter
        else -> throw IllegalArgumentException("알 수 없는 숙소 ID 형식: $accommodationId")
    }
}
