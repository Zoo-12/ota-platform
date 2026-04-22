package com.ota.platform.booking.api

import com.ota.platform.booking.application.GetBookingDetailUseCase
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
    private val getBookingDetailUseCase: GetBookingDetailUseCase,
) {
    @Operation(summary = "전체 예약 목록 조회")
    @GetMapping
    fun list(
        @RequestParam(required = false) propertyId: Long?,
    ): ApiResponse<List<BookingResponse>> {
        val results = if (propertyId != null) {
            getBookingDetailUseCase.getByPropertyId(propertyId)
        } else {
            getBookingDetailUseCase.getAll()
        }
        return ApiResponse.ok(results.map { it.toResponse() })
    }

    @Operation(summary = "예약 상세 조회")
    @GetMapping("/{bookingKey}")
    fun get(@PathVariable bookingKey: String): ApiResponse<BookingDetailResponse> =
        ApiResponse.ok(getBookingDetailUseCase.getById(bookingKey).toDetailResponse())
}
