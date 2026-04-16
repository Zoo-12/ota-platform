# AI 활용 기록

> 이 과제에서 AI(Claude Code)를 활용한 내역을 기록합니다.
> 활용 원칙: AI가 생성한 내용을 그대로 사용하지 않고, 직접 검토 후 판단을 더해 반영합니다.

---

## 2026-04-16

### 활용 내용: 도메인 설계 방향 결정

**리서치 내용:**

Booking.com, Expedia 공식 개발자 문서를 찾아 내용 분석을 의뢰하여 실제 OTA의 도메인 구조를 파악했다.

- **Booking.com** — [Managing roomrates & Rate plans](https://developers.booking.com/connectivity/docs/room-type-and-rate-plan-management/understanding-room-types-and-rate-plans)
  > "A roomrate is a unique combination of room type, rate plan and conditions. Creating a roomrate helps in creating inventory (availability) and rates (prices) later."
  → Room Type과 Rate Plan을 분리해서 관리하는 것이 표준 구조임을 확인. 재고(availability)와 요금(price)도 별도로 관리함.

- **Expedia** — [Product API Documentation](https://developers.expediagroup.com/supply/lodging/docs/property_mgmt_apis/image/learn/)
  → Property 하위에 Room Type → Rate Plan 순으로 구성하도록 강제. Per Day Pricing, Occupancy-based Pricing 등을 Rate Plan 단위로 분리 관리함을 확인.

**리서치 후 AI에게 설계 방향 제안을 요청한 내용:**
- 도메인 구조 초안 작성 (Property → RoomType → RatePlan → DailyRate 계층)
- 날짜별 재고 관리 패턴 (`RoomInventory(room_type_id, date)`)
- Supplier 통합을 위한 어댑터 패턴 설계
- 동시성 제어 전략 (비관적 락 vs 낙관적 락 비교)

**본인 판단 및 수정 사항:**
- AI가 마이크로서비스 분리를 권장했으나, 7일 과제 범위를 고려해 **모듈형 모놀리스**로 조정
- 동적 가격(RMS 연동)은 실제 구현 범위 밖이므로 설계 문서에만 기술하고 구현은 단순 Daily Rate 조회로 처리
- Supplier 통합에서 AI가 여러 어댑터를 제안했으나, Mock Supplier 1개만 구현하고 나머지는 인터페이스로만 정의하는 방향으로 범위 축소
- H2 대신 MySQL 사용 — 동시성 테스트 신뢰성을 위한 직접 판단 (과제 안내서에도 동일 권고 확인)

---

<!-- 이후 AI 활용 내역은 날짜별로 추가 -->
