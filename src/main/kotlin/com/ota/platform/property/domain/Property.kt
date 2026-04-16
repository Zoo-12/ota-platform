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
import java.time.LocalTime

@Entity
@Table(name = "properties")
class Property(
    partnerId: Long,
    name: String,
    description: String?,
    category: PropertyCategory,
    addressCity: String,
    addressDistrict: String?,
    addressDetail: String?,
    latitude: Double?,
    longitude: Double?,
    checkInTime: LocalTime?,
    checkOutTime: LocalTime?,
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
        protected set

    @Column(nullable = false)
    var partnerId: Long = partnerId
        protected set

    @Column(nullable = false, length = 200)
    var name: String = name
        protected set

    @Column(columnDefinition = "TEXT")
    var description: String? = description
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var category: PropertyCategory = category
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: PropertyStatus = PropertyStatus.PENDING_APPROVAL
        protected set

    @Column(nullable = false, length = 100)
    var addressCity: String = addressCity
        protected set

    @Column(length = 100)
    var addressDistrict: String? = addressDistrict
        protected set

    @Column(length = 255)
    var addressDetail: String? = addressDetail
        protected set

    @Column
    var latitude: Double? = latitude
        protected set

    @Column
    var longitude: Double? = longitude
        protected set

    var checkInTime: LocalTime? = checkInTime
        protected set

    var checkOutTime: LocalTime? = checkOutTime
        protected set

    fun approve() {
        status = PropertyStatus.ACTIVE
    }

    fun deactivate() {
        status = PropertyStatus.INACTIVE
    }

    fun update(
        name: String,
        description: String?,
        addressCity: String,
        addressDistrict: String?,
        addressDetail: String?,
        checkInTime: LocalTime?,
        checkOutTime: LocalTime?,
    ) {
        this.name = name
        this.description = description
        this.addressCity = addressCity
        this.addressDistrict = addressDistrict
        this.addressDetail = addressDetail
        this.checkInTime = checkInTime
        this.checkOutTime = checkOutTime
    }
}

enum class PropertyCategory {
    HOTEL,
    PENSION,
    GUESTHOUSE,
    RESORT,
    MOTEL,
}

enum class PropertyStatus {
    PENDING_APPROVAL,
    ACTIVE,
    INACTIVE,
}
