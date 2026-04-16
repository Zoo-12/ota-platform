package com.ota.platform.property.infrastructure

import com.ota.platform.property.domain.RatePlan
import org.springframework.data.jpa.repository.JpaRepository

interface RatePlanRepository : JpaRepository<RatePlan, Long> {
    fun findAllByRoomTypeIdAndIsActiveTrue(roomTypeId: Long): List<RatePlan>
    fun findAllByRoomTypeIdIn(roomTypeIds: List<Long>): List<RatePlan>
}
