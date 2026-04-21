package com.ota.platform.booking.adapter

import com.ota.platform.booking.port.PriceBreakdown
import com.ota.platform.booking.port.RatePlanInfo
import com.ota.platform.booking.port.RatePlanPort
import com.ota.platform.common.exception.NotFoundException
import com.ota.platform.property.domain.RateCalculationService
import com.ota.platform.property.infrastructure.RatePlanRepository
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RatePlanAdapter(
    private val ratePlanRepository: RatePlanRepository,
    private val rateCalculationService: RateCalculationService,
) : RatePlanPort {

    override fun getById(id: Long): RatePlanInfo =
        ratePlanRepository.findById(id)
            .map { RatePlanInfo(it.id, it.roomTypeId, it.basePrice) }
            .orElseThrow { NotFoundException("RatePlan", id) }

    override fun calculateTotalPrice(ratePlanId: Long, checkIn: LocalDate, checkOut: LocalDate): PriceBreakdown {
        val ratePlan = ratePlanRepository.findById(ratePlanId)
            .orElseThrow { NotFoundException("RatePlan", ratePlanId) }
        val breakdown = rateCalculationService.calculateTotalPrice(ratePlan, checkIn, checkOut)
        return PriceBreakdown(breakdown.totalPrice, breakdown.priceByDate)
    }
}
