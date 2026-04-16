package com.ota.platform.property.domain

import com.ota.platform.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "rate_plans")
class RatePlan(
    roomTypeId: Long,
    name: String,
    cancelPolicy: CancelPolicy,
    breakfastIncluded: Boolean,
    basePrice: BigDecimal,
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
        protected set

    @Column(nullable = false)
    var roomTypeId: Long = roomTypeId
        protected set

    @Column(nullable = false, length = 100)
    var name: String = name
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var cancelPolicy: CancelPolicy = cancelPolicy
        protected set

    @Column(nullable = false)
    var breakfastIncluded: Boolean = breakfastIncluded
        protected set

    @Column(nullable = false, precision = 12, scale = 2)
    var basePrice: BigDecimal = basePrice
        protected set

    @Column(nullable = false)
    var isActive: Boolean = true
        protected set

    fun deactivate() {
        isActive = false
    }

    fun update(
        name: String,
        cancelPolicy: CancelPolicy,
        breakfastIncluded: Boolean,
        basePrice: BigDecimal,
    ) {
        this.name = name
        this.cancelPolicy = cancelPolicy
        this.breakfastIncluded = breakfastIncluded
        this.basePrice = basePrice
    }
}

enum class CancelPolicy {
    FREE_CANCEL,
    NON_REFUNDABLE,
    PARTIAL_REFUND,
}
