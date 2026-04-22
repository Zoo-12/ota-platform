package com.ota.platform.property.api

import com.ota.platform.property.application.PropertyDetailResult
import java.math.BigDecimal

data class PropertyDetailResponse(
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
    val roomTypes: List<RoomTypeAdminResponse>,
)

data class RoomTypeAdminResponse(
    val id: Long,
    val name: String,
    val maxOccupancy: Int,
    val bedType: String,
    val sizeSqm: Double?,
    val ratePlans: List<RatePlanAdminResponse>,
)

data class RatePlanAdminResponse(
    val id: Long,
    val name: String,
    val cancelPolicy: String,
    val breakfastIncluded: Boolean,
    val basePrice: BigDecimal,
)

fun PropertyDetailResult.toResponse() = PropertyDetailResponse(
    id = id,
    partnerId = partnerId,
    name = name,
    description = description,
    category = category,
    status = status,
    addressCity = addressCity,
    addressDistrict = addressDistrict,
    addressDetail = addressDetail,
    checkInTime = checkInTime,
    checkOutTime = checkOutTime,
    roomTypes = roomTypes.map { rt ->
        RoomTypeAdminResponse(
            id = rt.id,
            name = rt.name,
            maxOccupancy = rt.maxOccupancy,
            bedType = rt.bedType,
            sizeSqm = rt.sizeSqm,
            ratePlans = rt.ratePlans.map { rp ->
                RatePlanAdminResponse(
                    id = rp.id,
                    name = rp.name,
                    cancelPolicy = rp.cancelPolicy,
                    breakfastIncluded = rp.breakfastIncluded,
                    basePrice = rp.basePrice,
                )
            },
        )
    },
)
