package com.ota.platform.booking.api

import com.ota.platform.booking.application.CancelBookingCommand
import com.ota.platform.booking.application.CancelBookingUseCase
import com.ota.platform.booking.application.CreateBookingCommand
import com.ota.platform.booking.application.CreateBookingUseCase
import com.ota.platform.booking.application.GetBookingDetailUseCase
import com.ota.platform.common.response.ApiResponse
import com.ota.platform.common.response.RegisterResponse
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
    private val cancelBookingUseCase: CancelBookingUseCase,
    private val getBookingDetailUseCase: GetBookingDetailUseCase,
) {
    @Operation(summary = "예약 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateBookingRequest): ApiResponse<RegisterResponse> {
        val id = createBookingUseCase.create(
            CreateBookingCommand(
                customerId = request.customerId,
                roomTypeId = request.roomTypeId,
                ratePlanId = request.ratePlanId,
                checkIn = request.checkIn,
                checkOut = request.checkOut,
                guestCount = request.guestCount,
                guestName = request.guestName,
                guestPhone = request.guestPhone,
                specialRequest = request.specialRequest,
            ),
        )
        return ApiResponse.ok(RegisterResponse(id))
    }

    @Operation(summary = "내 예약 목록 조회")
    @GetMapping
    fun list(@RequestParam customerId: Long): ApiResponse<List<BookingResponse>> =
        ApiResponse.ok(getBookingDetailUseCase.getByCustomer(customerId).map { it.toResponse() })

    @Operation(summary = "예약 상세 조회")
    @GetMapping("/{bookingId}")
    fun get(@PathVariable bookingId: Long): ApiResponse<BookingDetailResponse> =
        ApiResponse.ok(getBookingDetailUseCase.getById(bookingId).toDetailResponse())

    @Operation(summary = "예약 취소")
    @DeleteMapping("/{bookingId}")
    fun cancel(
        @PathVariable bookingId: Long,
        @Valid @RequestBody request: CancelBookingRequest,
    ): ApiResponse<Unit> {
        cancelBookingUseCase.cancel(
            CancelBookingCommand(
                customerId = request.customerId,
                bookingId = bookingId,
                reason = request.reason,
            ),
        )
        return ApiResponse.ok()
    }
}
