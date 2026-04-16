package com.ota.platform.supplier.port

import java.math.BigDecimal
import java.time.LocalDate

interface AccommodationPort {
    fun search(query: AccommodationSearchQuery): List<AccommodationSearchResult>
    fun getRates(accommodationId: String, checkIn: LocalDate, checkOut: LocalDate): List<AccommodationRateResult>
}

data class AccommodationSearchQuery(
    val city: String,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val guestCount: Int,
)

data class AccommodationSearchResult(
    val accommodationId: String,   // 공급사별 고유 ID (내부: "INTERNAL:{id}", 외부: "SUPPLIER_A:{id}")
    val name: String,
    val category: String,
    val addressCity: String,
    val minPrice: BigDecimal,      // 해당 기간 최저가
    val source: AccommodationSource,
)

data class AccommodationRateResult(
    val roomTypeId: String,
    val roomTypeName: String,
    val ratePlanId: String,
    val ratePlanName: String,
    val cancelPolicy: String,
    val breakfastIncluded: Boolean,
    val pricePerNight: BigDecimal,
    val totalPrice: BigDecimal,
    val availableCount: Int,
)

enum class AccommodationSource {
    INTERNAL,
    SUPPLIER_A,
    SUPPLIER_B,
}
