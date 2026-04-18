package com.ota.platform.booking.event

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 예약 도메인 이벤트 리스너.
 *
 * @TransactionalEventListener(phase = AFTER_COMMIT) 사용 이유:
 * - 트랜잭션이 성공적으로 커밋된 후에만 이벤트 처리
 * - DB 저장 실패 시 이벤트 발행 자체가 일어나지 않으므로 일관성 보장
 *
 * @Async 사용 이유:
 * - 알림 발송 등 후처리가 예약 API 응답을 지연시키지 않도록 비동기 처리
 * - 실제 운영에서는 Kafka/RabbitMQ 등 메시지 브로커로 대체 가능
 */
@Component
class BookingEventListener {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleBookingCreated(event: BookingCreatedEvent) {
        log.info(
            "[BookingCreatedEvent] 예약 완료 — bookingId={}, customerId={}, propertyId={}, " +
                "guest={}, checkIn={}, checkOut={}, totalPrice={}",
            event.bookingId,
            event.customerId,
            event.propertyId,
            event.guestName,
            event.checkIn,
            event.checkOut,
            event.totalPrice,
        )

        // TODO (실제 구현 시):
        // - 고객에게 예약 확인 알림 발송 (SMS / 이메일 / 푸시)
        // - 파트너에게 신규 예약 알림
        // - 예약 통계 업데이트
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleBookingCancelled(event: BookingCancelledEvent) {
        log.info(
            "[BookingCancelledEvent] 예약 취소 — bookingId={}, customerId={}, propertyId={}, reason={}",
            event.bookingId,
            event.customerId,
            event.propertyId,
            event.reason,
        )

        // TODO (실제 구현 시):
        // - 고객에게 취소 확인 알림 발송
        // - 환불 처리 요청
        // - 파트너에게 취소 알림
    }
}
