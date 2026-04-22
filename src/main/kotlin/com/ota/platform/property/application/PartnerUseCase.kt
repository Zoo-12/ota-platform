package com.ota.platform.property.application

import com.ota.platform.common.exception.BadRequestException
import com.ota.platform.common.exception.NotFoundException
import com.ota.platform.property.domain.Partner
import com.ota.platform.property.domain.PartnerStatus
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

    @Transactional(readOnly = true)
    fun list(status: PartnerStatus?): List<Partner> =
        if (status != null) partnerRepository.findAllByStatus(status) else partnerRepository.findAll()

    @Transactional
    fun approve(partnerId: Long) {
        val partner = partnerRepository.findById(partnerId)
            .orElseThrow { NotFoundException("Partner", partnerId) }
        if (!partner.isPending()) {
            throw BadRequestException("승인 대기 상태의 파트너만 승인할 수 있습니다.")
        }
        partner.activate()
        partnerRepository.save(partner)
    }
}

data class RegisterPartnerCommand(
    val name: String,
    val email: String,
    val phone: String,
    val businessNumber: String,
)
