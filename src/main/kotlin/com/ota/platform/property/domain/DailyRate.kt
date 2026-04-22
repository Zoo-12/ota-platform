package com.ota.platform.property.domain

import com.ota.platform.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 날짜별 요금 오버라이드.
 * 요금 조회 시 DailyRate 우선, 없으면 RatePlan.basePrice 사용.
 */
@Entity
@Table(name = "daily_rate")
class DailyRate(
    ratePlanId: Long,
    date: LocalDate,
    price: BigDecimal,
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
        protected set

    @Column(nullable = false)
    var ratePlanId: Long = ratePlanId
        protected set

    @Column(nullable = false)
    var date: LocalDate = date
        protected set

    @Column(nullable = false, precision = 12, scale = 2)
    var price: BigDecimal = price
        protected set

    fun updatePrice(price: BigDecimal) {
        this.price = price
    }
}
