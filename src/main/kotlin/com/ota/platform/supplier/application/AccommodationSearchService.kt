package com.ota.platform.supplier.application

import com.ota.platform.common.config.CacheNames
import com.ota.platform.supplier.port.AccommodationDetailResult
import com.ota.platform.supplier.port.AccommodationPort
import com.ota.platform.supplier.port.AccommodationRateResult
import com.ota.platform.supplier.port.AccommodationSearchQuery
import com.ota.platform.supplier.port.AccommodationSearchResult
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * 내부 숙소 + 외부 Supplier 통합 검색 서비스.
 * 각 어댑터의 결과를 병합하여 최저가 기준 정렬 후 반환.
 */
@Service
class AccommodationSearchService(
    private val adapters: List<AccommodationPort>,
) {
    @Cacheable(
        cacheNames = [CacheNames.ACCOMMODATION_SEARCH],
        key = "#query.city + ':' + #query.checkIn + ':' + #query.checkOut + ':' + #query.guestCount",
    )
    fun search(query: AccommodationSearchQuery): List<AccommodationSearchResult> {
        return adapters
            .flatMap { adapter ->
                runCatching { adapter.search(query) }.getOrElse { emptyList() }
            }
            .sortedBy { it.minPrice }
    }

    @Cacheable(
        cacheNames = [CacheNames.ACCOMMODATION_RATES],
        key = "#accommodationId + ':' + #checkIn + ':' + #checkOut",
    )
    fun getRates(accommodationId: String, checkIn: LocalDate, checkOut: LocalDate): List<AccommodationRateResult> {
        return resolveAdapter(accommodationId)
            .getRates(accommodationId, checkIn, checkOut)
            .sortedBy { it.totalPrice }
    }

    @Cacheable(
        cacheNames = [CacheNames.ACCOMMODATION_DETAIL],
        key = "#accommodationId",
    )
    fun getDetail(accommodationId: String): AccommodationDetailResult {
        return resolveAdapter(accommodationId).getDetail(accommodationId)
    }

    private fun resolveAdapter(accommodationId: String): AccommodationPort =
        adapters.firstOrNull { it.canHandle(accommodationId) }
            ?: throw IllegalArgumentException("알 수 없는 숙소 ID 형식: $accommodationId")
}
