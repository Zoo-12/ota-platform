package com.ota.platform.booking.api

import com.ota.platform.booking.application.BookingDetailResult
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate

data class CreateBookingRequest(
    @field:NotNull val customerId: Long,
    @field:NotNull val roomTypeId: Long,
    @field:NotNull val ratePlanId: Long,
    @field:NotNull val checkIn: LocalDate,
    @field:NotNull val checkOut: LocalDate,
    @field:Min(1) val guestCount: Int,
    @field:NotBlank val guestName: String,
    val guestPhone: String?,
    val specialRequest: String?,
)

data class CancelBookingRequest(
    @field:NotNull val customerId: Long,
    val reason: String?,
)

data class BookingResponse(
    val id: Long,
    val propertyId: Long,
    val propertyName: String,
    val roomTypeId: Long,
    val roomTypeName: String,
    val ratePlanId: Long,
    val ratePlanName: String,
    val checkIn: String,
    val checkOut: String,
    val guestCount: Int,
    val totalPrice: BigDecimal,
    val guestName: String,
    val status: String,
    val cancelledAt: String?,
    val cancelReason: String?,
)

data class BookingDetailResponse(
    val id: Long,
    val propertyId: Long,
    val propertyName: String,
    val roomTypeId: Long,
    val roomTypeName: String,
    val ratePlanId: Long,
    val ratePlanName: String,
    val checkIn: String,
    val checkOut: String,
    val guestCount: Int,
    val totalPrice: BigDecimal,
    val guestName: String,
    val guestPhone: String?,
    val specialRequest: String?,
    val status: String,
    val cancelledAt: String?,
    val cancelReason: String?,
)

fun BookingDetailResult.toResponse() = BookingResponse(
    id = id,
    propertyId = propertyId,
    propertyName = propertyName,
    roomTypeId = roomTypeId,
    roomTypeName = roomTypeName,
    ratePlanId = ratePlanId,
    ratePlanName = ratePlanName,
    checkIn = checkIn.toString(),
    checkOut = checkOut.toString(),
    guestCount = guestCount,
    totalPrice = totalPrice,
    guestName = guestName,
    status = status,
    cancelledAt = cancelledAt?.toString(),
    cancelReason = cancelReason,
)

fun BookingDetailResult.toDetailResponse() = BookingDetailResponse(
    id = id,
    propertyId = propertyId,
    propertyName = propertyName,
    roomTypeId = roomTypeId,
    roomTypeName = roomTypeName,
    ratePlanId = ratePlanId,
    ratePlanName = ratePlanName,
    checkIn = checkIn.toString(),
    checkOut = checkOut.toString(),
    guestCount = guestCount,
    totalPrice = totalPrice,
    guestName = guestName,
    guestPhone = guestPhone,
    specialRequest = specialRequest,
    status = status,
    cancelledAt = cancelledAt?.toString(),
    cancelReason = cancelReason,
)
