package com.ota.platform.supplier.port

import java.math.BigDecimal
import java.time.LocalDate

interface AccommodationPort {
    fun canHandle(accommodationId: String): Boolean
    fun search(query: AccommodationSearchQuery): List<AccommodationSearchResult>
    fun getRates(accommodationId: String, checkIn: LocalDate, checkOut: LocalDate): List<AccommodationRateResult>
    fun getDetail(accommodationId: String): AccommodationDetailResult
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

data class AccommodationDetailResult(
    val accommodationId: String,
    val name: String,
    val description: String?,
    val category: String,
    val addressCity: String,
    val addressDistrict: String?,
    val addressDetail: String?,
    val checkInTime: String?,
    val checkOutTime: String?,
    val source: AccommodationSource,
    val roomTypes: List<RoomTypeDetail>,
)

data class RoomTypeDetail(
    val roomTypeId: String,
    val name: String,
    val maxOccupancy: Int,
    val bedType: String,
    val sizeSqm: Double?,
    val ratePlans: List<RatePlanDetail>,
)

data class RatePlanDetail(
    val ratePlanId: String,
    val name: String,
    val cancelPolicy: String,
    val breakfastIncluded: Boolean,
    val basePrice: java.math.BigDecimal,
)

enum class AccommodationSource {
    INTERNAL,
    SUPPLIER_A,
    SUPPLIER_B,
}
