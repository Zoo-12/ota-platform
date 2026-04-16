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

@Entity
@Table(name = "room_types")
class RoomType(
    propertyId: Long,
    name: String,
    description: String?,
    maxOccupancy: Int,
    bedType: BedType,
    sizeSqm: Double?,
    amenities: String?,
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
        protected set

    @Column(nullable = false)
    var propertyId: Long = propertyId
        protected set

    @Column(nullable = false, length = 100)
    var name: String = name
        protected set

    @Column(columnDefinition = "TEXT")
    var description: String? = description
        protected set

    @Column(nullable = false)
    var maxOccupancy: Int = maxOccupancy
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var bedType: BedType = bedType
        protected set

    @Column
    var sizeSqm: Double? = sizeSqm
        protected set

    // JSON 컬럼: ["wifi", "parking", "breakfast"] 형태로 저장
    @Column(columnDefinition = "JSON")
    var amenities: String? = amenities
        protected set

    fun update(
        name: String,
        description: String?,
        maxOccupancy: Int,
        bedType: BedType,
        sizeSqm: Double?,
        amenities: String?,
    ) {
        this.name = name
        this.description = description
        this.maxOccupancy = maxOccupancy
        this.bedType = bedType
        this.sizeSqm = sizeSqm
        this.amenities = amenities
    }
}

enum class BedType {
    SINGLE,
    DOUBLE,
    TWIN,
    KING,
    QUEEN,
}
