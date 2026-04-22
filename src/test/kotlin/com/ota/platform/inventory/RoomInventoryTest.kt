package com.ota.platform.inventory

import com.ota.platform.common.exception.ConflictException
import com.ota.platform.inventory.domain.RoomInventory
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * RoomInventory 도메인 단위 테스트.
 *
 * Spring 컨텍스트 없이 도메인 로직만 검증한다.
 * DB 저장·조회와 무관하게 decrease/increase/stopSell 규칙을 빠르게 확인하는 용도.
 */
@DisplayName("RoomInventory 도메인 단위 테스트")
class RoomInventoryTest {

    private fun inventory(total: Int, available: Int = total, stopSell: Boolean = false): RoomInventory {
        val inv = RoomInventory(
            roomTypeId = 1L,
            date = LocalDate.of(2030, 1, 1),
            totalCount = total,
            availableCount = available,
        )
        if (stopSell) inv.updateStopSell(true)
        return inv
    }

    @Test
    @DisplayName("재고가 있으면 decrease 호출 시 availableCount가 1 감소한다")
    fun `decrease - 재고 있을 때 차감 성공`() {
        val inv = inventory(total = 3, available = 2)

        inv.decrease()

        assertThat(inv.availableCount).isEqualTo(1)
    }

    @Test
    @DisplayName("availableCount가 0이면 decrease 호출 시 ConflictException이 발생한다")
    fun `decrease - 재고 0일 때 예외`() {
        val inv = inventory(total = 2, available = 0)

        assertThatThrownBy { inv.decrease() }
            .isInstanceOf(ConflictException::class.java)
            .hasMessageContaining("재고가 없습니다")
    }

    @Test
    @DisplayName("stopSell=true이면 availableCount > 0 이어도 decrease 시 ConflictException이 발생한다")
    fun `decrease - stopSell 활성화 시 예외`() {
        val inv = inventory(total = 5, available = 5, stopSell = true)

        assertThatThrownBy { inv.decrease() }
            .isInstanceOf(ConflictException::class.java)
    }

    @Test
    @DisplayName("availableCount < totalCount 이면 increase 호출 시 1 증가한다")
    fun `increase - 정상 복원`() {
        val inv = inventory(total = 3, available = 2)

        inv.increase()

        assertThat(inv.availableCount).isEqualTo(3)
    }

    @Test
    @DisplayName("availableCount == totalCount 이면 increase 호출 시 IllegalStateException이 발생한다")
    fun `increase - 총 수량 초과 시 예외`() {
        val inv = inventory(total = 2, available = 2)

        assertThatThrownBy { inv.increase() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("총 수량을 초과")
    }

    @Test
    @DisplayName("availableCount > 0 이고 stopSell=false 이면 isAvailable은 true")
    fun `isAvailable - 정상 재고`() {
        assertThat(inventory(total = 1, available = 1).isAvailable()).isTrue()
    }

    @Test
    @DisplayName("availableCount == 0 이면 isAvailable은 false")
    fun `isAvailable - 재고 소진`() {
        assertThat(inventory(total = 1, available = 0).isAvailable()).isFalse()
    }

    @Test
    @DisplayName("stopSell=true 이면 availableCount > 0 이어도 isAvailable은 false")
    fun `isAvailable - stopSell 활성화`() {
        assertThat(inventory(total = 5, available = 5, stopSell = true).isAvailable()).isFalse()
    }

    @Test
    @DisplayName("updateStopSell(true) 호출 후 stopSell 필드가 true로 변경된다")
    fun `updateStopSell - true 설정`() {
        val inv = inventory(total = 3)
        inv.updateStopSell(true)
        assertThat(inv.stopSell).isTrue()
    }

    @Test
    @DisplayName("updateStopSell(false) 호출 후 stopSell 필드가 false로 변경된다")
    fun `updateStopSell - false 해제`() {
        val inv = inventory(total = 3, stopSell = true)
        inv.updateStopSell(false)
        assertThat(inv.stopSell).isFalse()
    }
}
