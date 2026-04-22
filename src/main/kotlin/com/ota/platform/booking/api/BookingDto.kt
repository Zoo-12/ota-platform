package com.ota.platform.booking.api

import com.ota.platform.booking.application.BookingDetailResult
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate

data class CreateBookingRequest(
    @field:NotNull val customerId: Long,
    val accommodationId: String?,
    @field:NotBlank val roomTypeId: String,
    @field:NotBlank val ratePlanId: String,
    @field:NotNull val checkIn: LocalDate,
    @field:NotNull val checkOut: LocalDate,
    @field:Min(1) val guestCount: Int,
    @field:NotBlank val guestName: String,
    val guestPhone: String?,
    val specialRequest: String?,
    val totalPrice: BigDecimal?,
)

data class CancelBookingRequest(
    @field:NotNull val customerId: Long,
    val reason: String?,
)

data class CreateBookingResponse(
    val bookingKey: String,
)

data class BookingResponse(
    val bookingKey: String,
    val source: String,
    val propertyId: String?,
    val propertyName: String?,
    val roomTypeId: String?,
    val roomTypeName: String?,
    val ratePlanId: String?,
    val ratePlanName: String?,
    val externalBookingNo: String?,
    val checkIn: String,
    val checkOut: String,
    val guestCount: Int,
    val totalPrice: BigDecimal?,
    val guestName: String,
    val status: String,
    val cancelledAt: String?,
    val cancelReason: String?,
    val createdAt: String,
)

data class BookingDetailResponse(
    val bookingKey: String,
    val source: String,
    val propertyId: String?,
    val propertyName: String?,
    val roomTypeId: String?,
    val roomTypeName: String?,
    val ratePlanId: String?,
    val ratePlanName: String?,
    val externalBookingNo: String?,
    val checkIn: String,
    val checkOut: String,
    val guestCount: Int,
    val totalPrice: BigDecimal?,
    val guestName: String,
    val guestPhone: String?,
    val specialRequest: String?,
    val status: String,
    val cancelledAt: String?,
    val cancelReason: String?,
    val createdAt: String,
)

fun BookingDetailResult.toResponse() = BookingResponse(
    bookingKey = bookingKey,
    source = source,
    propertyId = propertyId,
    propertyName = propertyName,
    roomTypeId = roomTypeId,
    roomTypeName = roomTypeName,
    ratePlanId = ratePlanId,
    ratePlanName = ratePlanName,
    externalBookingNo = externalBookingNo,
    checkIn = checkIn.toString(),
    checkOut = checkOut.toString(),
    guestCount = guestCount,
    totalPrice = totalPrice,
    guestName = guestName,
    status = status,
    cancelledAt = cancelledAt?.toString(),
    cancelReason = cancelReason,
    createdAt = createdAt.toString(),
)

fun BookingDetailResult.toDetailResponse() = BookingDetailResponse(
    bookingKey = bookingKey,
    source = source,
    propertyId = propertyId,
    propertyName = propertyName,
    roomTypeId = roomTypeId,
    roomTypeName = roomTypeName,
    ratePlanId = ratePlanId,
    ratePlanName = ratePlanName,
    externalBookingNo = externalBookingNo,
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
    createdAt = createdAt.toString(),
)
