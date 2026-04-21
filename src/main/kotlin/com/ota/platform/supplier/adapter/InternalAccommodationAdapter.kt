package com.ota.platform.supplier.adapter

import com.ota.platform.inventory.infrastructure.RoomInventoryRepository
import com.ota.platform.property.domain.RateCalculationService
import com.ota.platform.property.domain.PropertyStatus
import com.ota.platform.property.infrastructure.PropertyRepository
import com.ota.platform.property.infrastructure.RatePlanRepository
import com.ota.platform.property.infrastructure.RoomTypeRepository
import com.ota.platform.supplier.port.AccommodationDetailResult
import com.ota.platform.supplier.port.AccommodationPort
import com.ota.platform.supplier.port.AccommodationRateResult
import com.ota.platform.supplier.port.AccommodationSearchQuery
import com.ota.platform.supplier.port.AccommodationSearchResult
import com.ota.platform.supplier.port.AccommodationSource
import com.ota.platform.supplier.port.RatePlanDetail
import com.ota.platform.supplier.port.RoomTypeDetail
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Component
class InternalAccommodationAdapter(
    private val propertyRepository: PropertyRepository,
    private val roomTypeRepository: RoomTypeRepository,
    private val ratePlanRepository: RatePlanRepository,
    private val roomInventoryRepository: RoomInventoryRepository,
    private val rateCalculationService: RateCalculationService,
) : AccommodationPort {

    override fun canHandle(accommodationId: String) = accommodationId.startsWith(SupplierPrefixes.INTERNAL)

    @Transactional(readOnly = true)
    override fun search(query: AccommodationSearchQuery): List<AccommodationSearchResult> {
        val properties = propertyRepository.findAllByStatusAndAddressCity(
            PropertyStatus.ACTIVE, query.city,
        )

        return properties.mapNotNull { property ->
            val roomTypes = roomTypeRepository.findAllByPropertyId(property.id)
            val roomTypeIds = roomTypes.map { it.id }

            // 기간 내 가용 재고가 있는 요금 플랜만 조회
            val availableInventories = roomInventoryRepository.findAllByRoomTypeIdInAndDateBetween(
                roomTypeIds, query.checkIn, query.checkOut.minusDays(1),
            )
            val nights = query.checkIn.datesUntil(query.checkOut).count()
            val availableRoomTypeIds = availableInventories
                .filter { it.isAvailable() }
                .groupBy { it.roomTypeId }
                .filter { (_, inventories) -> inventories.size.toLong() == nights }
                .keys

            if (availableRoomTypeIds.isEmpty()) return@mapNotNull null

            val ratePlans = ratePlanRepository.findAllByRoomTypeIdIn(availableRoomTypeIds.toList())
                .filter { it.isActive }

            val minPrice = ratePlans.minOfOrNull { ratePlan ->
                rateCalculationService.calculateTotalPrice(ratePlan, query.checkIn, query.checkOut).totalPrice
            } ?: return@mapNotNull null

            AccommodationSearchResult(
                accommodationId = "${SupplierPrefixes.INTERNAL}${property.id}",
                name = property.name,
                category = property.category.name,
                addressCity = property.addressCity,
                minPrice = minPrice,
                source = AccommodationSource.INTERNAL,
            )
        }
    }

    @Transactional(readOnly = true)
    override fun getDetail(accommodationId: String): AccommodationDetailResult {
        val propertyId = accommodationId.removePrefix(SupplierPrefixes.INTERNAL).toLong()
        val property = propertyRepository.findById(propertyId).orElseThrow { NoSuchElementException("숙소를 찾을 수 없습니다: $propertyId") }
        val roomTypes = roomTypeRepository.findAllByPropertyId(propertyId)

        return AccommodationDetailResult(
            accommodationId = accommodationId,
            name = property.name,
            description = property.description,
            category = property.category.name,
            addressCity = property.addressCity,
            addressDistrict = property.addressDistrict,
            addressDetail = property.addressDetail,
            checkInTime = property.checkInTime?.toString(),
            checkOutTime = property.checkOutTime?.toString(),
            source = AccommodationSource.INTERNAL,
            roomTypes = roomTypes.map { roomType ->
                val ratePlans = ratePlanRepository.findAllByRoomTypeIdAndIsActiveTrue(roomType.id)
                RoomTypeDetail(
                    roomTypeId = roomType.id.toString(),
                    name = roomType.name,
                    maxOccupancy = roomType.maxOccupancy,
                    bedType = roomType.bedType.name,
                    sizeSqm = roomType.sizeSqm,
                    ratePlans = ratePlans.map { rp ->
                        RatePlanDetail(
                            ratePlanId = rp.id.toString(),
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

    @Transactional(readOnly = true)
    override fun getRates(accommodationId: String, checkIn: LocalDate, checkOut: LocalDate): List<AccommodationRateResult> {
        val propertyId = accommodationId.removePrefix(SupplierPrefixes.INTERNAL).toLong()
        val roomTypes = roomTypeRepository.findAllByPropertyId(propertyId)
        val nights = checkIn.datesUntil(checkOut).count()

        return roomTypes.flatMap { roomType ->
            val inventories = roomInventoryRepository.findAllByRoomTypeIdAndDateBetween(
                roomType.id, checkIn, checkOut.minusDays(1),
            )
            if (inventories.size.toLong() != nights || !inventories.all { it.isAvailable() }) {
                return@flatMap emptyList()
            }
            val availableCount = inventories.minOf { it.availableCount }

            ratePlanRepository.findAllByRoomTypeIdAndIsActiveTrue(roomType.id).map { ratePlan ->
                val breakdown = rateCalculationService.calculateTotalPrice(ratePlan, checkIn, checkOut)
                AccommodationRateResult(
                    roomTypeId = roomType.id.toString(),
                    roomTypeName = roomType.name,
                    ratePlanId = ratePlan.id.toString(),
                    ratePlanName = ratePlan.name,
                    cancelPolicy = ratePlan.cancelPolicy.name,
                    breakfastIncluded = ratePlan.breakfastIncluded,
                    pricePerNight = breakdown.totalPrice.divide(BigDecimal(nights)),
                    totalPrice = breakdown.totalPrice,
                    availableCount = availableCount,
                )
            }
        }
    }
}
