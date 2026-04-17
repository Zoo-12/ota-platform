package com.ota.platform.property.api

import com.ota.platform.common.response.ApiResponse
import com.ota.platform.common.response.RegisterResponse
import com.ota.platform.property.application.PartnerUseCase
import com.ota.platform.property.application.RegisterPartnerCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Extranet - 파트너")
@RestController
@RequestMapping("/api/extranet/partners")
class ExtranetPartnerController(
    private val partnerUseCase: PartnerUseCase,
) {
    @Operation(summary = "파트너 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterPartnerRequest): ApiResponse<RegisterResponse> {
        val id = partnerUseCase.register(
            RegisterPartnerCommand(
                name = request.name,
                email = request.email,
                phone = request.phone,
                businessNumber = request.businessNumber,
            ),
        )
        return ApiResponse.ok(RegisterResponse(id))
    }

    @Operation(summary = "파트너 조회")
    @GetMapping("/{partnerId}")
    fun get(@PathVariable partnerId: Long): ApiResponse<PartnerResponse> {
        val partner = partnerUseCase.getById(partnerId)
        return ApiResponse.ok(
            PartnerResponse(
                id = partner.id,
                name = partner.name,
                email = partner.email,
                phone = partner.phone,
                businessNumber = partner.businessNumber,
                status = partner.status.name,
            ),
        )
    }
}

data class RegisterPartnerRequest(
    @field:NotBlank val name: String,
    @field:NotBlank val email: String,
    @field:NotBlank val phone: String,
    @field:NotBlank val businessNumber: String,
)

data class PartnerResponse(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String,
    val businessNumber: String,
    val status: String,
)
