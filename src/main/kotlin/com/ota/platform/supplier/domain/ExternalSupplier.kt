package com.ota.platform.supplier.domain

import com.ota.platform.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "external_supplier")
class ExternalSupplier(
    name: String,
    adapterType: SupplierAdapterType,
    apiEndpoint: String?,
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
        protected set

    @Column(nullable = false, length = 100)
    var name: String = name
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    var adapterType: SupplierAdapterType = adapterType
        protected set

    @Column(length = 500)
    var apiEndpoint: String? = apiEndpoint
        protected set

    @Column(nullable = false)
    var isActive: Boolean = true
        protected set

    fun deactivate() {
        isActive = false
    }
}

enum class SupplierAdapterType {
    MOCK_SUPPLIER_A,
    MOCK_SUPPLIER_B,
}
