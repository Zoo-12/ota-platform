package com.ota.platform.property.infrastructure

import com.ota.platform.property.domain.Partner
import com.ota.platform.property.domain.PartnerStatus
import org.springframework.data.jpa.repository.JpaRepository

interface PartnerRepository : JpaRepository<Partner, Long> {
    fun existsByEmail(email: String): Boolean
    fun existsByBusinessNumber(businessNumber: String): Boolean
    fun findAllByStatus(status: PartnerStatus): List<Partner>
}
