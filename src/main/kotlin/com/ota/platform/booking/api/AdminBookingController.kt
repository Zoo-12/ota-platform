package com.ota.platform.booking.api

import com.ota.platform.booking.application.GetBookingUseCase
import com.ota.platform.booking.infrastructure.BookingRepository
import com.ota.platform.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin - 예약")
@RestController
@RequestMapping("/api/admin/bookings")
class AdminBookingController(
    private val getBookingUseCase: GetBookingUseCase,
    private val bookingRepository: BookingRepository,
) {
    @Operation(summary = "전체 예약 목록 조회")
    @GetMapping
    fun list(
        @RequestParam(required = false) propertyId: Long?,
    ): ApiResponse<List<BookingResponse>> {
        val bookings = if (propertyId != null) {
            bookingRepository.findAllByPropertyId(propertyId)
        } else {
            bookingRepository.findAll()
        }
        return ApiResponse.ok(bookings.map { it.toResponse() })
    }

    @Operation(summary = "예약 상세 조회")
    @GetMapping("/{bookingId}")
    fun get(@PathVariable bookingId: Long): ApiResponse<BookingResponse> {
        return ApiResponse.ok(getBookingUseCase.getById(bookingId).toResponse())
    }
}
