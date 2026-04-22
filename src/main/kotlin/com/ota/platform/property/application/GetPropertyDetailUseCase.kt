package com.ota.platform.property.application

import com.ota.platform.property.infrastructure.RatePlanRepository
import com.ota.platform.property.infrastructure.RoomTypeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * 숙소 상세 정보 (객실 타입 + 요금제 포함) 조합 조회.
 */
@Service
class GetPropertyDetailUseCase(
    private val propertyUseCase: PropertyUseCase,
    private val roomTypeRepository: RoomTypeRepository,
    private val ratePlanRepository: RatePlanRepository,
) {
    @Transactional(readOnly = true)
    fun getById(propertyId: Long): PropertyDetailResult {
        val property = propertyUseCase.getById(propertyId)
        val roomTypes = roomTypeRepository.findAllByPropertyId(property.id)
        val ratePlansByRoomType = ratePlanRepository
            .findAllByRoomTypeIdIn(roomTypes.map { it.id })
            .groupBy { it.roomTypeId }

        return PropertyDetailResult(
            id = property.id,
            partnerId = property.partnerId,
            name = property.name,
            description = property.description,
            category = property.category.name,
            status = property.status.name,
            addressCity = property.addressCity,
            addressDistrict = property.addressDistrict,
            addressDetail = property.addressDetail,
            checkInTime = property.checkInTime?.toString(),
            checkOutTime = property.checkOutTime?.toString(),
            roomTypes = roomTypes.map { rt ->
                RoomTypeResult(
                    id = rt.id,
                    name = rt.name,
                    maxOccupancy = rt.maxOccupancy,
                    bedType = rt.bedType.name,
                    sizeSqm = rt.sizeSqm,
                    ratePlans = ratePlansByRoomType[rt.id].orEmpty().map { rp ->
                        RatePlanResult(
                            id = rp.id,
                            name = rp.name,
                            cancelPolicy = rp.cancelPolicy.name,
                            breakfastIncluded = rp.breakfastIncluded,
                            basePrice = rp.basePrice,
                        )
                    },
                )
            },
        )
    }
}

data class PropertyDetailResult(
    val id: Long,
    val partnerId: Long,
    val name: String,
    val description: String?,
    val category: String,
    val status: String,
    val addressCity: String,
    val addressDistrict: String?,
    val addressDetail: String?,
    val checkInTime: String?,
    val checkOutTime: String?,
    val roomTypes: List<RoomTypeResult>,
)

data class RoomTypeResult(
    val id: Long,
    val name: String,
    val maxOccupancy: Int,
    val bedType: String,
    val sizeSqm: Double?,
    val ratePlans: List<RatePlanResult>,
)

data class RatePlanResult(
    val id: Long,
    val name: String,
    val cancelPolicy: String,
    val breakfastIncluded: Boolean,
    val basePrice: BigDecimal,
)
