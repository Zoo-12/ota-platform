package com.ota.platform.booking.api

import com.ota.platform.booking.application.CancelBookingCommand
import com.ota.platform.booking.application.CancelBookingUseCase
import com.ota.platform.booking.application.CreateBookingCommand
import com.ota.platform.booking.application.CreateBookingUseCase
import com.ota.platform.booking.application.GetBookingUseCase
import com.ota.platform.booking.domain.Booking
import com.ota.platform.common.response.ApiResponse
import com.ota.platform.common.response.RegisterResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
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
import java.math.BigDecimal
import java.time.LocalDate

@Tag(name = "Customer - 예약")
@RestController
@RequestMapping("/api/customer/bookings")
class CustomerBookingController(
    private val createBookingUseCase: CreateBookingUseCase,
    private val cancelBookingUseCase: CancelBookingUseCase,
    private val getBookingUseCase: GetBookingUseCase,
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
    fun list(@RequestParam customerId: Long): ApiResponse<List<BookingResponse>> {
        val bookings = getBookingUseCase.getByCustomer(customerId)
        return ApiResponse.ok(bookings.map { it.toResponse() })
    }

    @Operation(summary = "예약 상세 조회")
    @GetMapping("/{bookingId}")
    fun get(@PathVariable bookingId: Long): ApiResponse<BookingResponse> {
        val booking = getBookingUseCase.getById(bookingId)
        return ApiResponse.ok(booking.toResponse())
    }

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

fun Booking.toResponse() = BookingResponse(
    id = id,
    propertyId = propertyId,
    roomTypeId = roomTypeId,
    ratePlanId = ratePlanId,
    checkIn = checkIn.toString(),
    checkOut = checkOut.toString(),
    guestCount = guestCount,
    totalPrice = totalPrice,
    guestName = guestName,
    status = status.name,
    cancelledAt = cancelledAt?.toString(),
    cancelReason = cancelReason,
)

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
    val roomTypeId: Long,
    val ratePlanId: Long,
    val checkIn: String,
    val checkOut: String,
    val guestCount: Int,
    val totalPrice: BigDecimal,
    val guestName: String,
    val status: String,
    val cancelledAt: String?,
    val cancelReason: String?,
)
