package com.ota.platform.property.infrastructure

import com.ota.platform.property.domain.DailyRate
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface DailyRateRepository : JpaRepository<DailyRate, Long> {
    fun findByRatePlanIdAndDate(ratePlanId: Long, date: LocalDate): DailyRate?
    fun findAllByRatePlanIdInAndDateBetween(
        ratePlanIds: List<Long>,
        from: LocalDate,
        to: LocalDate,
    ): List<DailyRate>
}
