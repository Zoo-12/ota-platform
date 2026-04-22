package com.ota.platform.property.application

import com.ota.platform.common.config.CacheNames
import com.ota.platform.common.exception.NotFoundException
import com.ota.platform.property.domain.BedType
import com.ota.platform.property.domain.RoomType
import com.ota.platform.property.infrastructure.PropertyRepository
import com.ota.platform.property.infrastructure.RoomTypeRepository
import com.ota.platform.property.port.InventoryPort
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class RoomTypeUseCase(
    private val roomTypeRepository: RoomTypeRepository,
    private val propertyRepository: PropertyRepository,
    private val inventoryPort: InventoryPort,
) {

    @Caching(evict = [
        CacheEvict(cacheNames = [CacheNames.ACCOMMODATION_DETAIL], allEntries = true),
        CacheEvict(cacheNames = [CacheNames.ACCOMMODATION_SEARCH], allEntries = true),
    ])
    @Transactional
    fun register(command: RegisterRoomTypeCommand): Long {
        if (!propertyRepository.existsById(command.propertyId)) {
            throw NotFoundException("Property", command.propertyId)
        }
        val roomType = RoomType(
            propertyId = command.propertyId,
            name = command.name,
            description = command.description,
            maxOccupancy = command.maxOccupancy,
            bedType = command.bedType,
            sizeSqm = command.sizeSqm,
            amenities = command.amenities,
        )
        val saved = roomTypeRepository.save(roomType)

        // 등록 시 기본 재고(0개) 초기화 — 파트너가 이후 재고 설정
        if (command.initInventoryFrom != null && command.initInventoryTo != null) {
            inventoryPort.initInventories(
                roomTypeId = saved.id,
                totalCount = command.totalCount ?: 0,
                from = command.initInventoryFrom,
                to = command.initInventoryTo,
            )
        }
        return saved.id
    }

    @Caching(evict = [
        CacheEvict(cacheNames = [CacheNames.ACCOMMODATION_DETAIL], allEntries = true),
    ])
    @Transactional
    fun update(roomTypeId: Long, command: UpdateRoomTypeCommand) {
        val roomType = findById(roomTypeId)
        roomType.update(
            name = command.name,
            description = command.description,
            maxOccupancy = command.maxOccupancy,
            bedType = command.bedType,
            sizeSqm = command.sizeSqm,
            amenities = command.amenities,
        )
    }

    @Transactional(readOnly = true)
    fun getByProperty(propertyId: Long): List<RoomType> =
        roomTypeRepository.findAllByPropertyId(propertyId)

    private fun findById(roomTypeId: Long): RoomType =
        roomTypeRepository.findById(roomTypeId)
            .orElseThrow { NotFoundException("RoomType", roomTypeId) }
}

data class RegisterRoomTypeCommand(
    val propertyId: Long,
    val name: String,
    val description: String?,
    val maxOccupancy: Int,
    val bedType: BedType,
    val sizeSqm: Double?,
    val amenities: String?,
    val totalCount: Int?,
    val initInventoryFrom: LocalDate?,
    val initInventoryTo: LocalDate?,
)

data class UpdateRoomTypeCommand(
    val name: String,
    val description: String?,
    val maxOccupancy: Int,
    val bedType: BedType,
    val sizeSqm: Double?,
    val amenities: String?,
)
