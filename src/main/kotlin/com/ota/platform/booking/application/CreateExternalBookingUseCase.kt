package com.ota.platform.booking.application

import com.ota.platform.booking.domain.BookingKeyType
import com.ota.platform.booking.domain.ExternalBooking
import com.ota.platform.booking.infrastructure.ExternalBookingRepository
import com.ota.platform.supplier.adapter.SupplierPrefixes
import com.ota.platform.supplier.port.AccommodationSource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * 외부 공급사 예약 생성 UseCase.
 * accommodationId의 prefix를 기반으로 공급사를 판별하고,
 * ExternalBooking 레코드(외부 예약 번호 포함)를 저장한다.
 *
 * ── 향후 실제 공급사 연동 시 SAGA 패턴 적용 계획 ──────────────────────────
 *
 * 현재는 로컬에서 예약 번호를 생성하지만, 실제 공급사 API 연동 시 아래 흐름으로 전환한다.
 *
 * [Phase 1] 로컬 DB에 PENDING 상태로 먼저 저장 (트랜잭션 커밋)
 *   - ExternalBooking(status = PENDING) 을 즉시 커밋하여 추적 가능한 상태로 만든다.
 *   - 이후 외부 API 호출이 실패해도 row가 남아 운영팀이 모니터링·재처리할 수 있다.
 *
 * [Phase 2] 외부 공급사 API 호출 (트랜잭션 외부)
 *   - SupplierBookingPort.reserve() 를 통해 각 공급사 FeignClient를 호출한다.
 *   - FeignClient 예시:
 *       @FeignClient(name = "supplier-a", url = "\${supplier.a.base-url}")
 *       interface SupplierABookingClient {
 *           @PostMapping("/v1/bookings")
 *           fun reserve(@RequestBody req: SupplierAReserveRequest): SupplierAReserveResponse
 *       }
 *
 * [Phase 3a] 성공 → CONFIRMED 전이 (별도 트랜잭션 커밋)
 *   - 공급사가 발급한 externalBookingNo 로 갱신, status = CONFIRMED
 *
 * [Phase 3b] 실패 → 보상 트랜잭션 (FAILED 전이)
 *   - status = FAILED 로 갱신하여 재처리 대상임을 표시
 *   - SupplierBookingException(502) 을 던져 클라이언트에 알림
 *
 * 각 Phase는 Propagation.REQUIRES_NEW 트랜잭션으로 분리하여 독립적으로 커밋된다.
 * ────────────────────────────────────────────────────────────────────────────
 */
@Service
class CreateExternalBookingUseCase(
    private val externalBookingRepository: ExternalBookingRepository,
) {
    @Transactional
    fun create(command: CreateExternalBookingCommand): String {
        val source = resolveSource(command.accommodationId)
        val bookingNoPrefix = resolveBookingNoPrefix(source)

        // TODO: 실제 연동 시 여기서 외부 공급사 API(FeignClient)를 호출하고
        //       공급사가 발급한 예약 번호를 externalBookingNo로 사용한다.
        //       그 전에 PENDING 상태로 먼저 저장(SAGA Phase 1)하고,
        //       API 성공 시 CONFIRMED, 실패 시 보상 트랜잭션으로 FAILED 처리한다.
        val externalBookingNo = "$bookingNoPrefix-${UUID.randomUUID().toString().take(8).uppercase()}"

        val saved = externalBookingRepository.save(
            ExternalBooking(
                customerId = command.customerId,
                accommodationId = command.accommodationId,
                externalBookingNo = externalBookingNo,
                source = source,
                checkIn = command.checkIn,
                checkOut = command.checkOut,
                guestCount = command.guestCount,
                totalPrice = command.totalPrice,
                guestName = command.guestName,
                guestPhone = command.guestPhone,
            ),
        )
        return BookingKeyType.EXTERNAL.key(saved.id)
    }

    private fun resolveSource(accommodationId: String): String = when {
        accommodationId.startsWith(SupplierPrefixes.SUPPLIER_A) -> AccommodationSource.SUPPLIER_A.name
        else -> "SUPPLIER_UNKNOWN"
    }

    private fun resolveBookingNoPrefix(source: String): String = when (source) {
        AccommodationSource.SUPPLIER_A.name -> "SA"
        else -> "EXT"
    }
}

data class CreateExternalBookingCommand(
    val customerId: Long,
    val accommodationId: String,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val guestCount: Int,
    val totalPrice: BigDecimal?,
    val guestName: String,
    val guestPhone: String?,
)
