package com.ota.platform.booking.domain

/**
 * 예약 키 타입.
 * 내부(자사) 예약과 외부(공급사) 예약을 구분하는 prefix를 관리하며,
 * 키 생성/파싱 로직을 한 곳에 응집시킨다.
 *
 * 사용 예:
 *   BookingKeyType.INTERNAL.key(123L)  → "INT-123"
 *   BookingKeyType.EXTERNAL.key(5L)    → "EXT-5"
 *   BookingKeyType.parse("INT-123")    → Pair(INTERNAL, 123L)
 */
enum class BookingKeyType(val prefix: String) {
    INTERNAL("INT-"),
    EXTERNAL("EXT-"),
    ;

    fun key(id: Long) = "$prefix$id"

    companion object {
        /**
         * bookingKey 문자열을 파싱해 (타입, ID) 쌍을 반환한다.
         */
        fun parse(bookingKey: String): Pair<BookingKeyType, Long> =
            entries
                .firstOrNull { bookingKey.startsWith(it.prefix) }
                ?.let { type -> type to bookingKey.removePrefix(type.prefix).toLong() }
                ?: throw IllegalArgumentException("유효하지 않은 예약 키 형식: $bookingKey")
    }
}
