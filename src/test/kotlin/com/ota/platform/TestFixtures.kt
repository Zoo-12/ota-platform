package com.ota.platform

import com.ota.platform.booking.domain.Customer
import com.ota.platform.booking.infrastructure.CustomerRepository
import com.ota.platform.inventory.domain.RoomInventory
import com.ota.platform.inventory.infrastructure.RoomInventoryRepository
import com.ota.platform.property.domain.BedType
import com.ota.platform.property.domain.CancelPolicy
import com.ota.platform.property.domain.Partner
import com.ota.platform.property.domain.Property
import com.ota.platform.property.domain.PropertyCategory
import com.ota.platform.property.domain.RatePlan
import com.ota.platform.property.domain.RoomType
import com.ota.platform.property.infrastructure.PartnerRepository
import com.ota.platform.property.infrastructure.PropertyRepository
import com.ota.platform.property.infrastructure.RatePlanRepository
import com.ota.platform.property.infrastructure.RoomTypeRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

@Component
class TestFixtures(
    private val partnerRepository: PartnerRepository,
    private val propertyRepository: PropertyRepository,
    private val roomTypeRepository: RoomTypeRepository,
    private val ratePlanRepository: RatePlanRepository,
    private val roomInventoryRepository: RoomInventoryRepository,
    private val customerRepository: CustomerRepository,
) {
    @Transactional
    fun createPartner(name: String = "테스트 파트너"): Partner {
        val suffix = (1..999999).random()
        return partnerRepository.save(
            Partner(
                name = name,
                email = "partner-$suffix@test.com",
                phone = "010-1234-5678",
                businessNumber = "123-45-$suffix",
            ),
        )
    }

    @Transactional
    fun createPendingProperty(partnerId: Long, city: String = "서울"): Property {
        return propertyRepository.save(
            Property(
                partnerId = partnerId,
                name = "테스트 호텔",
                description = null,
                category = PropertyCategory.HOTEL,
                addressCity = city,
                addressDistrict = "강남구",
                addressDetail = "테헤란로 1",
                latitude = 37.5,
                longitude = 127.0,
                checkInTime = LocalTime.of(15, 0),
                checkOutTime = LocalTime.of(11, 0),
            ),
        )
    }

    @Transactional
    fun createActiveProperty(partnerId: Long, city: String = "서울"): Property {
        val property = createPendingProperty(partnerId, city)
        property.approve()
        return propertyRepository.save(property)
    }

    @Transactional
    fun createRoomType(propertyId: Long): RoomType {
        return roomTypeRepository.save(
            RoomType(
                propertyId = propertyId,
                name = "스탠다드 룸",
                description = null,
                maxOccupancy = 2,
                bedType = BedType.DOUBLE,
                sizeSqm = 25.0,
                amenities = null,
            ),
        )
    }

    @Transactional
    fun createRatePlan(roomTypeId: Long, basePrice: BigDecimal = BigDecimal("100000")): RatePlan {
        return ratePlanRepository.save(
            RatePlan(
                roomTypeId = roomTypeId,
                name = "기본 요금",
                cancelPolicy = CancelPolicy.FREE_CANCEL,
                breakfastIncluded = false,
                basePrice = basePrice,
            ),
        )
    }

    @Transactional
    fun createInventory(roomTypeId: Long, date: LocalDate, totalCount: Int = 1): RoomInventory {
        return roomInventoryRepository.save(
            RoomInventory(
                roomTypeId = roomTypeId,
                date = date,
                totalCount = totalCount,
                availableCount = totalCount,
            ),
        )
    }

    @Transactional
    fun createInventoryRange(roomTypeId: Long, from: LocalDate, to: LocalDate, totalCount: Int = 1): List<RoomInventory> {
        return from.datesUntil(to.plusDays(1)).map { date ->
            roomInventoryRepository.save(
                RoomInventory(
                    roomTypeId = roomTypeId,
                    date = date,
                    totalCount = totalCount,
                    availableCount = totalCount,
                ),
            )
        }.toList()
    }

    @Transactional
    fun createCustomer(): Customer {
        val suffix = (1..9999999).random()
        return customerRepository.save(
            Customer(
                email = "customer-$suffix@test.com",
                name = "테스트 고객",
                phone = "010-9876-5432",
            ),
        )
    }
}
