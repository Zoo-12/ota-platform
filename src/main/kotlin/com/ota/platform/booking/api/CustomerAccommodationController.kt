package com.ota.platform.booking.api

import com.ota.platform.common.response.ApiResponse
import com.ota.platform.supplier.application.AccommodationSearchService
import com.ota.platform.supplier.port.AccommodationSearchQuery
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Tag(name = "Customer - 숙소 검색")
@RestController
@RequestMapping("/api/customer/accommodations")
class CustomerAccommodationController(
    private val accommodationSearchService: AccommodationSearchService,
) {
    @Operation(summary = "숙소 검색 (내부 + 외부 Supplier 통합)")
    @GetMapping("/search")
    fun search(
        @RequestParam city: String,
        @RequestParam checkIn: LocalDate,
        @RequestParam checkOut: LocalDate,
        @RequestParam(defaultValue = "1") guestCount: Int,
    ): ApiResponse<List<AccommodationSearchResponse>> =
        ApiResponse.ok(
            accommodationSearchService.search(
                AccommodationSearchQuery(city = city, checkIn = checkIn, checkOut = checkOut, guestCount = guestCount),
            ).map { it.toResponse() },
        )

    @Operation(summary = "숙소 상세 조회 (객실 타입 + 요금제 포함)")
    @GetMapping("/{accommodationId}")
    fun getDetail(
        @PathVariable accommodationId: String,
    ): ApiResponse<AccommodationDetailResponse> =
        ApiResponse.ok(accommodationSearchService.getDetail(accommodationId).toResponse())

    @Operation(summary = "숙소 요금 조회")
    @GetMapping("/{accommodationId}/rates")
    fun getRates(
        @PathVariable accommodationId: String,
        @RequestParam checkIn: LocalDate,
        @RequestParam checkOut: LocalDate,
    ): ApiResponse<List<AccommodationRateResponse>> =
        ApiResponse.ok(accommodationSearchService.getRates(accommodationId, checkIn, checkOut).map { it.toResponse() })
}
