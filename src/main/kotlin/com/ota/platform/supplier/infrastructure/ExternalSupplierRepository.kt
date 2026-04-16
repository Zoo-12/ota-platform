package com.ota.platform.supplier.infrastructure

import com.ota.platform.supplier.domain.ExternalSupplier
import org.springframework.data.jpa.repository.JpaRepository

interface ExternalSupplierRepository : JpaRepository<ExternalSupplier, Long> {
    fun findAllByIsActiveTrue(): List<ExternalSupplier>
}
