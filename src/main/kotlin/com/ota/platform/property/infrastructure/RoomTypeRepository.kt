package com.ota.platform.property.infrastructure

import com.ota.platform.property.domain.RoomType
import org.springframework.data.jpa.repository.JpaRepository

interface RoomTypeRepository : JpaRepository<RoomType, Long> {
    fun findAllByPropertyId(propertyId: Long): List<RoomType>
}
