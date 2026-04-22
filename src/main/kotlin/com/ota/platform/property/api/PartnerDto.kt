package com.ota.platform.property.api

import jakarta.validation.constraints.NotBlank

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
