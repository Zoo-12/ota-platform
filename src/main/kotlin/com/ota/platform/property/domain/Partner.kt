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
@Table(name = "partner")
class Partner(
    name: String,
    email: String,
    phone: String,
    businessNumber: String,
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
        protected set

    @Column(nullable = false, length = 100)
    var name: String = name
        protected set

    @Column(nullable = false, unique = true, length = 255)
    var email: String = email
        protected set

    @Column(nullable = false, length = 20)
    var phone: String = phone
        protected set

    @Column(nullable = false, unique = true, length = 20)
    var businessNumber: String = businessNumber
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: PartnerStatus = PartnerStatus.PENDING
        protected set

    fun isPending(): Boolean = status == PartnerStatus.PENDING

    fun isActive(): Boolean = status == PartnerStatus.ACTIVE

    fun activate() {
        status = PartnerStatus.ACTIVE
    }

    fun suspend() {
        status = PartnerStatus.SUSPENDED
    }
}

enum class PartnerStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
}
