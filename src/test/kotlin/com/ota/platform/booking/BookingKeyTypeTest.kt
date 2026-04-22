package com.ota.platform.booking

import com.ota.platform.booking.domain.BookingKeyType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("BookingKeyType 단위 테스트")
class BookingKeyTypeTest {

    @Test
    @DisplayName("INTERNAL 타입으로 예약 키 생성 - 'INT-{id}' 형식")
    fun `INTERNAL key 생성`() {
        val key = BookingKeyType.INTERNAL.key(42L)
        assertThat(key).isEqualTo("INT-42")
    }

    @Test
    @DisplayName("EXTERNAL 타입으로 예약 키 생성 - 'EXT-{id}' 형식")
    fun `EXTERNAL key 생성`() {
        val key = BookingKeyType.EXTERNAL.key(7L)
        assertThat(key).isEqualTo("EXT-7")
    }

    @Test
    @DisplayName("'INT-123' 파싱 → (INTERNAL, 123)")
    fun `INTERNAL 키 파싱`() {
        val (type, id) = BookingKeyType.parse("INT-123")
        assertThat(type).isEqualTo(BookingKeyType.INTERNAL)
        assertThat(id).isEqualTo(123L)
    }

    @Test
    @DisplayName("'EXT-5' 파싱 → (EXTERNAL, 5)")
    fun `EXTERNAL 키 파싱`() {
        val (type, id) = BookingKeyType.parse("EXT-5")
        assertThat(type).isEqualTo(BookingKeyType.EXTERNAL)
        assertThat(id).isEqualTo(5L)
    }

    @Test
    @DisplayName("key() 후 parse() 해도 동일한 값 반환 — 라운드트립 보장")
    fun `key-parse 라운드트립`() {
        val originalId = 999L
        val (type, parsedId) = BookingKeyType.parse(BookingKeyType.INTERNAL.key(originalId))
        assertThat(type).isEqualTo(BookingKeyType.INTERNAL)
        assertThat(parsedId).isEqualTo(originalId)
    }

    @Test
    @DisplayName("유효하지 않은 prefix의 키 파싱 시 IllegalArgumentException 발생")
    fun `잘못된 키 형식 파싱 실패`() {
        assertThatThrownBy { BookingKeyType.parse("UNKNOWN-1") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("유효하지 않은 예약 키 형식")
    }

    @Test
    @DisplayName("빈 문자열 파싱 시 IllegalArgumentException 발생")
    fun `빈 문자열 파싱 실패`() {
        assertThatThrownBy { BookingKeyType.parse("") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
