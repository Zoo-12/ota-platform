package com.ota.platform.property.domain

import com.ota.platform.property.infrastructure.DailyRateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 요금 계산 도메인 서비스.
 * DailyRate 우선 조회, 없으면 RatePlan.basePrice 사용.
 */
@Service
class RateCalculationService(
    private val dailyRateRepository: DailyRateRepository,
) {

    /**
     * 특정 날짜의 실효 요금 계산.
     */
    @Transactional(readOnly = true)
    fun getEffectivePrice(ratePlan: RatePlan, date: LocalDate): BigDecimal {
        return dailyRateRepository.findByRatePlanIdAndDate(ratePlan.id, date)?.price
            ?: ratePlan.basePrice
    }

    /**
     * 체크인~체크아웃 기간의 총 요금 계산.
     * 날짜별 가격 스냅샷 리스트도 함께 반환 (BookingRoom 저장용).
     */
    @Transactional(readOnly = true)
    fun calculateTotalPrice(ratePlan: RatePlan, checkIn: LocalDate, checkOut: LocalDate): PriceBreakdown {
        val dates = checkIn.datesUntil(checkOut).toList()
        val dailyRates = dailyRateRepository
            .findAllByRatePlanIdInAndDateBetween(listOf(ratePlan.id), checkIn, checkOut.minusDays(1))
            .associateBy { it.date }

        val priceByDate = dates.associateWith { date ->
            dailyRates[date]?.price ?: ratePlan.basePrice
        }

        return PriceBreakdown(
            priceByDate = priceByDate,
            totalPrice = priceByDate.values.fold(BigDecimal.ZERO, BigDecimal::add),
        )
    }
}

data class PriceBreakdown(
    val priceByDate: Map<LocalDate, BigDecimal>,
    val totalPrice: BigDecimal,
)
