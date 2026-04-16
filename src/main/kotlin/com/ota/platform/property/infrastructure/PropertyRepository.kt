package com.ota.platform.property.infrastructure

import com.ota.platform.property.domain.Property
import com.ota.platform.property.domain.PropertyStatus
import org.springframework.data.jpa.repository.JpaRepository

interface PropertyRepository : JpaRepository<Property, Long> {
    fun findAllByPartnerId(partnerId: Long): List<Property>
    fun findAllByStatusAndAddressCity(status: PropertyStatus, addressCity: String): List<Property>
    fun findAllByStatus(status: PropertyStatus): List<Property>
}
