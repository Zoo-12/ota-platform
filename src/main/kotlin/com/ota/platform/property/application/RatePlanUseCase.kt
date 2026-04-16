package com.ota.platform.property.application

import com.ota.platform.common.exception.NotFoundException
import com.ota.platform.property.domain.CancelPolicy
import com.ota.platform.property.domain.DailyRate
import com.ota.platform.property.domain.RatePlan
import com.ota.platform.property.infrastructure.DailyRateRepository
import com.ota.platform.property.infrastructure.RatePlanRepository
import com.ota.platform.property.infrastructure.RoomTypeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
class RatePlanUseCase(
    private val ratePlanRepository: RatePlanRepository,
    private val roomTypeRepository: RoomTypeRepository,
    private val dailyRateRepository: DailyRateRepository,
) {

    @Transactional
    fun register(command: RegisterRatePlanCommand): Long {
        if (!roomTypeRepository.existsById(command.roomTypeId)) {
            throw NotFoundException("RoomType", command.roomTypeId)
        }
        val ratePlan = RatePlan(
            roomTypeId = command.roomTypeId,
            name = command.name,
            cancelPolicy = command.cancelPolicy,
            breakfastIncluded = command.breakfastIncluded,
            basePrice = command.basePrice,
        )
        return ratePlanRepository.save(ratePlan).id
    }

    @Transactional
    fun update(ratePlanId: Long, command: UpdateRatePlanCommand) {
        val ratePlan = findById(ratePlanId)
        ratePlan.update(
            name = command.name,
            cancelPolicy = command.cancelPolicy,
            breakfastIncluded = command.breakfastIncluded,
            basePrice = command.basePrice,
        )
    }

    @Transactional
    fun setDailyRate(ratePlanId: Long, date: LocalDate, price: BigDecimal) {
        val existing = dailyRateRepository.findByRatePlanIdAndDate(ratePlanId, date)
        if (existing != null) {
            existing.updatePrice(price)
        } else {
            dailyRateRepository.save(DailyRate(ratePlanId = ratePlanId, date = date, price = price))
        }
    }

    @Transactional(readOnly = true)
    fun getByRoomType(roomTypeId: Long): List<RatePlan> =
        ratePlanRepository.findAllByRoomTypeIdAndIsActiveTrue(roomTypeId)

    private fun findById(ratePlanId: Long): RatePlan =
        ratePlanRepository.findById(ratePlanId)
            .orElseThrow { NotFoundException("RatePlan", ratePlanId) }
}

data class RegisterRatePlanCommand(
    val roomTypeId: Long,
    val name: String,
    val cancelPolicy: CancelPolicy,
    val breakfastIncluded: Boolean,
    val basePrice: BigDecimal,
)

data class UpdateRatePlanCommand(
    val name: String,
    val cancelPolicy: CancelPolicy,
    val breakfastIncluded: Boolean,
    val basePrice: BigDecimal,
)
