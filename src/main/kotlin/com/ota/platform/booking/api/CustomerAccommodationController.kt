package com.ota.platform.booking.api

import com.ota.platform.common.response.ApiResponse
import com.ota.platform.supplier.application.AccommodationSearchService
import com.ota.platform.supplier.port.AccommodationRateResult
import com.ota.platform.supplier.port.AccommodationSearchQuery
import com.ota.platform.supplier.port.AccommodationSearchResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
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
    ): ApiResponse<List<AccommodationSearchResponse>> {
        val results = accommodationSearchService.search(
            AccommodationSearchQuery(
                city = city,
                checkIn = checkIn,
                checkOut = checkOut,
                guestCount = guestCount,
            ),
        )
        return ApiResponse.ok(results.map { it.toResponse() })
    }

    @Operation(summary = "숙소 요금 조회")
    @GetMapping("/{accommodationId}/rates")
    fun getRates(
        @PathVariable accommodationId: String,
        @RequestParam checkIn: LocalDate,
        @RequestParam checkOut: LocalDate,
    ): ApiResponse<List<AccommodationRateResponse>> {
        val rates = accommodationSearchService.getRates(accommodationId, checkIn, checkOut)
        return ApiResponse.ok(rates.map { it.toResponse() })
    }
}

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
