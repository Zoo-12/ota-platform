package com.ota.platform.property.application

import com.ota.platform.common.exception.BadRequestException
import com.ota.platform.common.exception.NotFoundException
import com.ota.platform.property.domain.Partner
import com.ota.platform.property.infrastructure.PartnerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PartnerUseCase(
    private val partnerRepository: PartnerRepository,
) {

    @Transactional
    fun register(command: RegisterPartnerCommand): Long {
        if (partnerRepository.existsByEmail(command.email)) {
            throw BadRequestException("이미 등록된 이메일입니다: ${command.email}")
        }
        if (partnerRepository.existsByBusinessNumber(command.businessNumber)) {
            throw BadRequestException("이미 등록된 사업자번호입니다: ${command.businessNumber}")
        }
        val partner = Partner(
            name = command.name,
            email = command.email,
            phone = command.phone,
            businessNumber = command.businessNumber,
        )
        return partnerRepository.save(partner).id
    }

    @Transactional(readOnly = true)
    fun getById(partnerId: Long): Partner =
        partnerRepository.findById(partnerId)
            .orElseThrow { NotFoundException("Partner", partnerId) }
}

data class RegisterPartnerCommand(
    val name: String,
    val email: String,
    val phone: String,
    val businessNumber: String,
)
