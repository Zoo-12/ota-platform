package com.ota.platform.booking.domain

import com.ota.platform.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "customers")
class Customer(
    email: String,
    name: String,
    phone: String?,
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
        protected set

    @Column(nullable = false, unique = true, length = 255)
    var email: String = email
        protected set

    @Column(nullable = false, length = 100)
    var name: String = name
        protected set

    @Column(length = 20)
    var phone: String? = phone
        protected set
}
