package com.ota.platform.booking.api

import com.ota.platform.supplier.port.AccommodationDetailResult
import com.ota.platform.supplier.port.AccommodationRateResult
import com.ota.platform.supplier.port.AccommodationSearchResult
import com.ota.platform.supplier.port.RatePlanDetail
import com.ota.platform.supplier.port.RoomTypeDetail
import java.math.BigDecimal

data class AccommodationSearchResponse(
    val accommodationId: String,
    val name: String,
    val category: String,
    val addressCity: String,
    val minPrice: BigDecimal,
    val source: String,
)

data class AccommodationRateResponse(
    val roomTypeId: String,
    val roomTypeName: String,
    val ratePlanId: String,
    val ratePlanName: String,
    val cancelPolicy: String,
    val breakfastIncluded: Boolean,
    val pricePerNight: BigDecimal,
    val totalPrice: BigDecimal,
    val availableCount: Int,
)

data class AccommodationDetailResponse(
    val accommodationId: String,
    val name: String,
    val description: String?,
    val category: String,
    val addressCity: String,
    val addressDistrict: String?,
    val addressDetail: String?,
    val checkInTime: String?,
    val checkOutTime: String?,
    val source: String,
    val roomTypes: List<RoomTypeDetailResponse>,
)

data class RoomTypeDetailResponse(
    val roomTypeId: String,
    val name: String,
    val maxOccupancy: Int,
    val bedType: String,
    val sizeSqm: Double?,
    val ratePlans: List<RatePlanDetailResponse>,
)

data class RatePlanDetailResponse(
    val ratePlanId: String,
    val name: String,
    val cancelPolicy: String,
    val breakfastIncluded: Boolean,
    val basePrice: BigDecimal,
)

fun AccommodationSearchResult.toResponse() = AccommodationSearchResponse(
    accommodationId = accommodationId,
    name = name,
    category = category,
    addressCity = addressCity,
    minPrice = minPrice,
    source = source.name,
)

fun AccommodationRateResult.toResponse() = AccommodationRateResponse(
    roomTypeId = roomTypeId,
    roomTypeName = roomTypeName,
    ratePlanId = ratePlanId,
    ratePlanName = ratePlanName,
    cancelPolicy = cancelPolicy,
    breakfastIncluded = breakfastIncluded,
    pricePerNight = pricePerNight,
    totalPrice = totalPrice,
    availableCount = availableCount,
)

fun AccommodationDetailResult.toResponse() = AccommodationDetailResponse(
    accommodationId = accommodationId,
    name = name,
    description = description,
    category = category,
    addressCity = addressCity,
    addressDistrict = addressDistrict,
    addressDetail = addressDetail,
    checkInTime = checkInTime,
    checkOutTime = checkOutTime,
    source = source.name,
    roomTypes = roomTypes.map { it.toResponse() },
)

fun RoomTypeDetail.toResponse() = RoomTypeDetailResponse(
    roomTypeId = roomTypeId,
    name = name,
    maxOccupancy = maxOccupancy,
    bedType = bedType,
    sizeSqm = sizeSqm,
    ratePlans = ratePlans.map { it.toResponse() },
)

fun RatePlanDetail.toResponse() = RatePlanDetailResponse(
    ratePlanId = ratePlanId,
    name = name,
    cancelPolicy = cancelPolicy,
    breakfastIncluded = breakfastIncluded,
    basePrice = basePrice,
)
