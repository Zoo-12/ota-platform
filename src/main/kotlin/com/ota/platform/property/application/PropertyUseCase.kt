package com.ota.platform.property.application

import com.ota.platform.common.exception.BadRequestException
import com.ota.platform.common.exception.NotFoundException
import com.ota.platform.property.domain.Property
import com.ota.platform.property.domain.PropertyCategory
import com.ota.platform.property.infrastructure.PartnerRepository
import com.ota.platform.property.infrastructure.PropertyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime

@Service
class PropertyUseCase(
    private val propertyRepository: PropertyRepository,
    private val partnerRepository: PartnerRepository,
) {

    @Transactional
    fun register(command: RegisterPropertyCommand): Long {
        if (!partnerRepository.existsById(command.partnerId)) {
            throw NotFoundException("Partner", command.partnerId)
        }
        val property = Property(
            partnerId = command.partnerId,
            name = command.name,
            description = command.description,
            category = command.category,
            addressCity = command.addressCity,
            addressDistrict = command.addressDistrict,
            addressDetail = command.addressDetail,
            latitude = command.latitude,
            longitude = command.longitude,
            checkInTime = command.checkInTime,
            checkOutTime = command.checkOutTime,
        )
        return propertyRepository.save(property).id
    }

    @Transactional
    fun update(command: UpdatePropertyCommand) {
        val property = findById(command.propertyId)
        validatePartnerOwnership(property, command.partnerId)
        property.update(
            name = command.name,
            description = command.description,
            addressCity = command.addressCity,
            addressDistrict = command.addressDistrict,
            addressDetail = command.addressDetail,
            checkInTime = command.checkInTime,
            checkOutTime = command.checkOutTime,
        )
    }

    @Transactional(readOnly = true)
    fun getByPartner(partnerId: Long): List<Property> =
        propertyRepository.findAllByPartnerId(partnerId)

    @Transactional(readOnly = true)
    fun getById(propertyId: Long): Property = findById(propertyId)

    private fun findById(propertyId: Long): Property =
        propertyRepository.findById(propertyId)
            .orElseThrow { NotFoundException("Property", propertyId) }

    private fun validatePartnerOwnership(property: Property, partnerId: Long) {
        if (property.partnerId != partnerId) {
            throw BadRequestException("해당 숙소에 대한 권한이 없습니다.")
        }
    }
}

data class RegisterPropertyCommand(
    val partnerId: Long,
    val name: String,
    val description: String?,
    val category: PropertyCategory,
    val addressCity: String,
    val addressDistrict: String?,
    val addressDetail: String?,
    val latitude: Double?,
    val longitude: Double?,
    val checkInTime: LocalTime?,
    val checkOutTime: LocalTime?,
)

data class UpdatePropertyCommand(
    val partnerId: Long,
    val propertyId: Long,
    val name: String,
    val description: String?,
    val addressCity: String,
    val addressDistrict: String?,
    val addressDetail: String?,
    val checkInTime: LocalTime?,
    val checkOutTime: LocalTime?,
)
