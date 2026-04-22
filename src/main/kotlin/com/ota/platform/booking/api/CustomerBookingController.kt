package com.ota.platform.booking.api

import com.ota.platform.booking.application.CancelBookingCommand
import com.ota.platform.booking.application.CancelBookingUseCase
import com.ota.platform.booking.application.CancelExternalBookingCommand
import com.ota.platform.booking.application.CancelExternalBookingUseCase
import com.ota.platform.booking.application.CreateBookingCommand
import com.ota.platform.booking.application.CreateBookingUseCase
import com.ota.platform.booking.application.CreateExternalBookingCommand
import com.ota.platform.booking.application.CreateExternalBookingUseCase
import com.ota.platform.booking.application.GetBookingDetailUseCase
import com.ota.platform.booking.domain.BookingKeyType
import com.ota.platform.common.response.ApiResponse
import com.ota.platform.supplier.adapter.SupplierPrefixes
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Customer - 예약")
@RestController
@RequestMapping("/api/customer/bookings")
class CustomerBookingController(
    private val createBookingUseCase: CreateBookingUseCase,
    private val createExternalBookingUseCase: CreateExternalBookingUseCase,
    private val cancelBookingUseCase: CancelBookingUseCase,
    private val cancelExternalBookingUseCase: CancelExternalBookingUseCase,
    private val getBookingDetailUseCase: GetBookingDetailUseCase,
) {
    @Operation(summary = "예약 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateBookingRequest): ApiResponse<CreateBookingResponse> {
        val accommodationId = request.accommodationId

        if (accommodationId != null && !accommodationId.startsWith(SupplierPrefixes.INTERNAL)) {
            val bookingKey = createExternalBookingUseCase.create(
                CreateExternalBookingCommand(
                    customerId = request.customerId,
                    accommodationId = accommodationId,
                    checkIn = request.checkIn,
                    checkOut = request.checkOut,
                    guestCount = request.guestCount,
                    totalPrice = request.totalPrice,
                    guestName = request.guestName,
                    guestPhone = request.guestPhone,
                ),
            )
            return ApiResponse.ok(CreateBookingResponse(bookingKey = bookingKey))
        }

        val id = createBookingUseCase.create(
            CreateBookingCommand(
                customerId = request.customerId,
                roomTypeId = request.roomTypeId.toLong(),
                ratePlanId = request.ratePlanId.toLong(),
                checkIn = request.checkIn,
                checkOut = request.checkOut,
                guestCount = request.guestCount,
                guestName = request.guestName,
                guestPhone = request.guestPhone,
                specialRequest = request.specialRequest,
            ),
        )
        return ApiResponse.ok(CreateBookingResponse(bookingKey = BookingKeyType.INTERNAL.key(id)))
    }

    @Operation(summary = "내 예약 목록 조회")
    @GetMapping
    fun list(@RequestParam customerId: Long): ApiResponse<List<BookingResponse>> =
        ApiResponse.ok(getBookingDetailUseCase.getByCustomer(customerId).map { it.toResponse() })

    @Operation(summary = "예약 상세 조회")
    @GetMapping("/{bookingKey}")
    fun get(@PathVariable bookingKey: String): ApiResponse<BookingDetailResponse> =
        ApiResponse.ok(getBookingDetailUseCase.getById(bookingKey).toDetailResponse())

    @Operation(summary = "예약 취소")
    @DeleteMapping("/{bookingKey}")
    fun cancel(
        @PathVariable bookingKey: String,
        @Valid @RequestBody request: CancelBookingRequest,
    ): ApiResponse<Unit> {
        val (type, bookingId) = BookingKeyType.parse(bookingKey)
        when (type) {
            BookingKeyType.EXTERNAL -> cancelExternalBookingUseCase.cancel(
                CancelExternalBookingCommand(customerId = request.customerId, bookingId = bookingId),
            )
            BookingKeyType.INTERNAL -> cancelBookingUseCase.cancel(
                CancelBookingCommand(customerId = request.customerId, bookingId = bookingId, reason = request.reason),
            )
        }
        return ApiResponse.ok()
    }
}
