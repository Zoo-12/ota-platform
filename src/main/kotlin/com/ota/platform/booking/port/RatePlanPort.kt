package com.ota.platform.booking.port

import java.math.BigDecimal
import java.time.LocalDate

interface RatePlanPort {
    fun getById(id: Long): RatePlanInfo
    fun calculateTotalPrice(ratePlanId: Long, checkIn: LocalDate, checkOut: LocalDate): PriceBreakdown
}

data class RatePlanInfo(
    val id: Long,
    val roomTypeId: Long,
    val name: String,
    val cancelPolicy: String,
    val breakfastIncluded: Boolean,
    val basePrice: BigDecimal,
)

data class PriceBreakdown(
    val totalPrice: BigDecimal,
    val priceByDate: Map<LocalDate, BigDecimal>,
)
