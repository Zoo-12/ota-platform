package com.ota.platform.property

import com.ota.platform.property.domain.Property
import com.ota.platform.property.domain.PropertyCategory
import com.ota.platform.property.domain.PropertyStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalTime

/**
 * Property 도메인 단위 테스트.
 *
 * 숙소 상태 머신(PENDING_APPROVAL → ACTIVE → INACTIVE → ACTIVE)을
 * Spring 컨텍스트 없이 순수하게 검증한다.
 */
@DisplayName("Property 도메인 단위 테스트")
class PropertyTest {

    private fun newProperty(): Property = Property(
        partnerId = 1L,
        name = "테스트 호텔",
        description = null,
        category = PropertyCategory.HOTEL,
        addressCity = "서울",
        addressDistrict = "강남구",
        addressDetail = "테헤란로 1",
        latitude = 37.5,
        longitude = 127.0,
        checkInTime = LocalTime.of(15, 0),
        checkOutTime = LocalTime.of(11, 0),
    )

    @Test
    @DisplayName("신규 생성 숙소의 초기 상태는 PENDING_APPROVAL이다")
    fun `초기 상태 PENDING_APPROVAL`() {
        val property = newProperty()
        assertThat(property.status).isEqualTo(PropertyStatus.PENDING_APPROVAL)
    }

    @Test
    @DisplayName("approve() 호출 시 상태가 ACTIVE로 변경된다")
    fun `approve - PENDING_APPROVAL에서 ACTIVE로`() {
        val property = newProperty()

        property.approve()

        assertThat(property.status).isEqualTo(PropertyStatus.ACTIVE)
    }

    @Test
    @DisplayName("deactivate() 호출 시 상태가 INACTIVE로 변경된다")
    fun `deactivate - ACTIVE에서 INACTIVE로`() {
        val property = newProperty().apply { approve() }

        property.deactivate()

        assertThat(property.status).isEqualTo(PropertyStatus.INACTIVE)
    }

    @Test
    @DisplayName("reactivate() 호출 시 INACTIVE에서 ACTIVE로 복원된다")
    fun `reactivate - INACTIVE에서 ACTIVE로`() {
        val property = newProperty().apply { approve(); deactivate() }

        property.reactivate()

        assertThat(property.status).isEqualTo(PropertyStatus.ACTIVE)
    }

    @Test
    @DisplayName("update() 호출 시 이름과 도시 등 필드가 변경된다")
    fun `update - 필드 변경`() {
        val property = newProperty().apply { approve() }

        property.update(
            name = "변경된 호텔",
            description = "새 설명",
            addressCity = "부산",
            addressDistrict = "해운대구",
            addressDetail = "해운대로 1",
            checkInTime = LocalTime.of(16, 0),
            checkOutTime = LocalTime.of(12, 0),
        )

        assertThat(property.name).isEqualTo("변경된 호텔")
        assertThat(property.addressCity).isEqualTo("부산")
    }
}
