# OTA 숙박 플랫폼 도메인 리서치 기록

> 작성일: 2026-04-16
> 목적: OTA(Online Travel Agency) 숙박 플랫폼의 핵심 도메인 구조 파악 및 설계 근거 마련

---

## 1. 참고 자료

- **Booking.com** — [Managing roomrates & Rate plans](https://developers.booking.com/connectivity/docs/room-type-and-rate-plan-management/understanding-room-types-and-rate-plans)
- **Expedia** — [Product API Documentation](https://developers.expediagroup.com/supply/lodging/docs/property_mgmt_apis/image/learn/)

---

## 2. 리서치를 통해 확인한 핵심 사실

### 2.1 요금 체계 (Rate Plan + Daily Rate 분리)

Booking.com 공식 문서에서 확인:
> "A roomrate is a unique combination of room type, rate plan and conditions. Creating a roomrate helps in creating inventory (availability) and rates (prices) later."

Room Type과 Rate Plan을 분리해서 관리하는 것이 표준 구조이며, 재고(availability)와 요금(price)도 별도로 관리한다.

### 2.2 Property → RoomType → RatePlan 계층 구조

Expedia Product API 구조를 보면, Property 하위에 Room Type → Rate Plan 순으로 구성하도록 강제하고 있다. Per Day Pricing, Occupancy-based Pricing 등을 Rate Plan 단위로 분리 관리함을 확인했다.

---

## 3. 리서치 결과가 설계에 반영된 부분

| 리서치 발견 | 설계 반영 |
|------------|----------|
| Room Type과 Rate Plan 분리가 표준 | `RoomType`, `RatePlan`, `DailyRate` 엔티티 분리 |
| 재고와 요금을 별도 관리 | `RoomInventory(room_type_id, date)` + `DailyRate` 독립 테이블 |
| Property → RoomType → RatePlan 계층 | 동일한 계층 구조로 도메인 설계 |
