package com.ota.platform.property.api

import com.ota.platform.common.response.ApiResponse
import com.ota.platform.property.application.BulkUpdateInventoryCommand
import com.ota.platform.property.application.InventoryUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Tag(name = "Extranet - 재고")
@RestController
@RequestMapping("/api/extranet/room-types/{roomTypeId}/inventories")
class ExtranetInventoryController(
    private val inventoryUseCase: InventoryUseCase,
) {
    @Operation(summary = "기간별 재고 일괄 설정")
    @PutMapping
    fun bulkUpdate(
        @PathVariable roomTypeId: Long,
        @Valid @RequestBody request: BulkUpdateInventoryRequest,
    ): ApiResponse<Unit> {
        inventoryUseCase.bulkUpdate(
            BulkUpdateInventoryCommand(
                roomTypeId = roomTypeId,
                from = request.from,
                to = request.to,
                totalCount = request.totalCount,
                stopSell = request.stopSell,
                minStay = request.minStay,
                maxStay = request.maxStay,
            ),
        )
        return ApiResponse.ok()
    }

    @Operation(summary = "기간별 재고 조회")
    @GetMapping
    fun list(
        @PathVariable roomTypeId: Long,
        @RequestParam from: LocalDate,
        @RequestParam to: LocalDate,
    ): ApiResponse<List<InventoryResponse>> =
        ApiResponse.ok(inventoryUseCase.getInventories(roomTypeId, from, to).map {
            InventoryResponse(it.date.toString(), it.totalCount, it.availableCount, it.stopSell)
        })
}
