# OTA 숙박 플랫폼 아키텍처 문서

> 작성일: 2026-04-16
> 버전: v1.0

---

## 1. 시스템 개요

가상의 OTA(Online Travel Agency) 숙박 플랫폼으로, 숙소 파트너, 고객, 내부 운영팀, 외부 공급자(Supplier)를 아우르는 백엔드 시스템이다.

### 이해관계자

| 이해관계자 | 접점 | 설명 |
|-----------|------|------|
| 숙소 파트너 | Extranet API | 숙소, 객실, 요금, 재고 등록 및 관리 |
| 고객 | Customer API | 숙소 검색, 요금 조회, 예약, 취소 |
| 내부 운영자 | Admin API | 숙소, 예약, 파트너 모니터링 및 관리 |
| 외부 Supplier | Supplier Adapter | 외부 공급사 상품을 플랫폼에 통합 |

---

## 2. 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────────────┐
│                          Client Layer                               │
│   Extranet (파트너)   Customer App (고객)   Admin (운영자)               │
└──────────────┬──────────────────┬──────────────────┬────────────────┘
               │                  │                  │
┌──────────────▼──────────────────▼──────────────────▼───────────────┐
│                           API Layer                                │
│   /api/extranet/**       /api/customer/**      /api/admin/**       │
│   (파트너 등록/관리)      (검색/예약)            (운영 관리)               │
└──────────────┬──────────────────┬──────────────────┬───────────────┘
               │                  │                  │
┌──────────────▼──────────────────▼──────────────────▼───────────────┐
│                         Domain Layer                               │
│                                                                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌───────────┐  │
│  │  Property   │  │  Inventory  │  │   Booking   │  │ Supplier  │  │
│  │   Domain    │  │   Domain    │  │   Domain    │  │  Domain   │  │
│  └─────────────┘  └─────────────┘  └─────────────┘  └───────────┘  │
└──────────────────────────────┬─────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                      Infrastructure Layer                           │
│   MySQL (메인 DB)   Redis (캐시)   External Supplier APIs              │
└─────────────────────────────────────────────────────────────────────┘
```

### 아키텍처 스타일: 모듈형 모놀리스

마이크로서비스 대신 **모듈형 모놀리스**를 선택했다.

| 항목 | 마이크로서비스 | 모듈형 모놀리스 (선택) |
|------|--------------|----------------------|
| 배포 복잡도 | 높음 | 낮음 |
| 도메인 경계 | 네트워크 경계 | 패키지 경계 |
| 트랜잭션 | 분산 트랜잭션 필요 | 로컬 트랜잭션 |
| 구현 범위 적합성 | 오버엔지니어링 | 적합 |

도메인 간 경계는 명확하게 유지하되, 필요 시 마이크로서비스로 분리 가능하도록 인터페이스 기반으로 설계한다.

---

## 3. 도메인 구조

### 3.1 도메인 분리 기준

각 도메인은 **변경 이유(reason to change)**가 독립적인 단위로 분리하였다.

| 도메인 | 변경 이유 | 핵심 책임 |
|--------|----------|----------|
| Property | 파트너가 숙소 정보를 변경할 때 | 숙소, 객실 타입, 요금 플랜 관리 |
| Inventory | 재고 수량이 변동될 때 | 날짜별 가용 객실 수 추적 |
| Booking | 예약 정책이 변경될 때 | 예약 생성, 상태 관리, 취소 |
| Supplier | 외부 공급사가 추가/변경될 때 | 외부 상품 통합 및 변환 |

### 3.2 도메인 관계

```
Partner (파트너)
  │
  └──1:N──▶ Property (숙소)
              │
              └──1:N──▶ RoomType (객실 타입)
                           │
                           ├──1:N──▶ RatePlan (요금 플랜)
                           │             │
                           │             └──1:N──▶ DailyRate (날짜별 요금)
                           │
                           └──1:N──▶ RoomInventory (날짜별 재고)
                                         ▲
                                         │ SELECT FOR UPDATE
Customer (고객)                           │
  │                                      │
  └──1:N──▶ Booking (예약) ───────────────┘
                │
                └──1:N──▶ BookingRoom (예약 객실 상세)


ExternalSupplier (외부 공급사)
  │
  └── SupplierProperty (어댑터를 통해 Property와 동일 인터페이스로 노출)
```

### 3.3 도메인 간 의존 규칙

- 도메인 간 의존은 반드시 **Port 인터페이스**를 통해서만 허용 — 타 도메인의 Repository/Entity 직접 참조 금지
- 의존 방향은 단방향: `Booking → Inventory`, `Booking → Property`, `Property → Inventory`, `Supplier → Property/Inventory`
- 도메인 간 데이터 전달은 Port에 정의된 DTO(`RoomTypeInfo`, `InventoryData` 등)만 사용
- 도메인 간 직접 엔티티 참조 금지 — ID 참조만 허용

**Port/Adapter 구조:**

```
booking/port/RoomTypePort    ← booking/adapter/RoomTypeAdapter    (property 인프라 접근)
booking/port/RatePlanPort    ← booking/adapter/RatePlanAdapter    (property 인프라 접근)
booking/port/InventoryPort   ← booking/adapter/InventoryAdapter   (inventory 도메인 서비스 접근)
property/port/InventoryPort  ← property/adapter/InventoryAdapter  (inventory 인프라 접근)
supplier/port/AccommodationPort ← InternalAccommodationAdapter    (property/inventory 인프라 접근)
                                ← MockSupplierAAdapter             (외부 공급사 Mock)
```

---

## 4. 핵심 도메인 상세

### 4.1 Property Domain

파트너가 Extranet을 통해 숙소 정보를 등록하고 관리하는 도메인.

**주요 엔티티:**

```
Property
  - id, partnerId
  - name, description, address
  - category (HOTEL, PENSION, GUESTHOUSE, ...)
  - status (ACTIVE, INACTIVE, PENDING_APPROVAL)

RoomType
  - id, propertyId
  - name, description
  - maxOccupancy, bedType
  - amenities

RatePlan
  - id, roomTypeId
  - name
  - cancelPolicy (FREE_CANCEL, NON_REFUNDABLE, ...)
  - breakfastIncluded (boolean)
  - basePrice

DailyRate
  - id, ratePlanId, date
  - price
  (날짜별로 basePrice를 오버라이드할 수 있음)
```

**설계 결정:** RatePlan과 DailyRate를 분리한 이유는 Booking.com, Expedia의 실제 데이터 모델을 참고한 결과, 요금 플랜(조건/정책)과 날짜별 실제 가격은 변경 주기와 이유가 다르기 때문이다.

---

### 4.2 Inventory Domain

날짜별 객실 재고를 추적하는 도메인. 예약 시 동시성 이슈가 발생하는 핵심 지점이다.

**주요 엔티티:**

```
RoomInventory
  - id
  - roomTypeId
  - date
  - totalCount     (총 객실 수)
  - availableCount (가용 객실 수)
  - stopSell       (강제 판매 중단 여부)
  - version        (낙관적 락용, 현재는 비관적 락 사용)
```

**인덱스:** `(room_type_id, date)` UNIQUE — 날짜별 재고는 1행만 존재

---

### 4.3 Booking Domain

고객의 예약 생성부터 완료, 취소까지의 생명주기를 관리하는 도메인.

**주요 엔티티:**

```
Booking
  - id, customerId
  - status: PENDING → CONFIRMED → CANCELLED
  - checkIn, checkOut
  - totalPrice
  - createdAt

BookingRoom
  - id, bookingId
  - roomTypeId, ratePlanId
  - date
  - priceSnapshot  (예약 시점 가격 스냅샷)
```

**예약 흐름:**

```
1. 고객이 체크인/체크아웃 날짜 선택
2. 해당 날짜 범위의 RoomInventory 행에 SELECT FOR UPDATE (비관적 락)
3. availableCount > 0 검증
4. availableCount 감소
5. Booking 생성 (CONFIRMED)
6. 트랜잭션 커밋 → 락 해제

취소 시:
1. Booking 상태 → CANCELLED
2. RoomInventory.availableCount 복원
```

**동시성 전략 선택 근거:**

비관적 락(`SELECT FOR UPDATE`)을 선택한 이유:
- 초과예약(overbooking)은 절대 허용 불가 → 강한 일관성 필요
- 예약은 숙소 검색/요금 조회에 비해 트래픽이 현저히 낮을 것으로 예상됨 → 락 경합 비용이 낮아 비관적 락으로 처리
- 낙관적 락은 충돌 시 재시도 로직 필요 → 사용자 경험 불리

**향후 트래픽 증가 시 전환 전략:**

비관적 락은 대기 중인 트랜잭션이 DB 커넥션을 계속 점유하므로, 동시 요청이 많아지면 커넥션 풀 고갈 위험이 있다.
트래픽이 증가하거나 서버가 수평 확장될 경우 Redis 분산 락(Redisson)으로 전환을 고려한다.
Redis 분산 락은 락 획득 실패 시 DB에 접근하지 않아 DB 부하를 줄이고, 서버 여러 대에서도 동일한 락을 공유할 수 있다.

---

### 4.4 Supplier Domain

외부 공급사 상품을 플랫폼 내부와 통합하는 도메인. **어댑터 패턴(Adapter Pattern)**으로 설계.

**핵심 인터페이스:**

```kotlin
interface AccommodationPort {
    fun search(query: SearchQuery): List<AccommodationResult>
    fun getAvailability(accommodationId: String, dateRange: DateRange): AvailabilityResult
    fun getRates(accommodationId: String, dateRange: DateRange): List<RateResult>
}
```

**구현체:**

```
InternalAccommodationAdapter  → 자사 Property 도메인 조회
SupplierAAdapter              → 외부 Supplier A (예: Hotelbeds) API 호출
SupplierBAdapter              → 외부 Supplier B API 호출
```

**통합 검색 흐름:**

```
고객 검색 요청
      │
      ▼
AccommodationSearchService
      │
      ├──▶ InternalAccommodationAdapter.search()   → 자사 DB 조회
      │
      └──▶ SupplierAAdapter.search()               → 외부 API 실시간 호출
      │
      ▼
결과 병합 및 최저가 정렬 후 반환
```

**외부 API 연동 방식: 실시간 호출**

Supplier 상품 데이터는 자사 DB에 저장하지 않는다.
고객이 검색할 때마다 Supplier API를 실시간으로 호출하고, 응답을 내부 포맷(`AccommodationSearchResult`)으로 변환해 자사 결과와 병합한다.

```
[실제 구현 시 흐름]

고객 요청 → SupplierAAdapter
  │
  ├── 1. 내부 쿼리 → Supplier A API 규격으로 변환
  │       (city, checkIn, checkOut → Supplier A가 요구하는 파라미터)
  │
  ├── 2. HTTP 호출 (Feign Client 등)
  │       GET https://api.supplier-a.com/hotels/search?city=Seoul&...
  │
  ├── 3. Supplier A 응답 파싱
  │       { "hotel_id": "...", "hotel_name": "...", "price_from": 150000 }
  │
  └── 4. 내부 포맷으로 변환
          AccommodationSearchResult(accommodationId="SUPPLIER_A:...", ...)
```

**현재 구현: MockSupplierAAdapter**

실제 Supplier 계약 없이는 API 규격(엔드포인트, 인증, 응답 필드)을 알 수 없으므로,
`MockSupplierAAdapter`가 실제 HTTP 호출 대신 하드코딩된 데이터를 반환한다.
어댑터 패턴 덕분에 실제 계약 후 `MockSupplierAAdapter`를 실제 HTTP 클라이언트 구현체로 교체하면 나머지 코드는 변경 없이 동작한다.

| | MockSupplierAAdapter (현재) | 실제 SupplierAAdapter |
|--|--------------------------|----------------------|
| API 호출 | 없음 (하드코딩 데이터 반환) | Feign Client로 HTTP 호출 |
| 인증 | 없음 | API Key / OAuth |
| 응답 변환 | 없음 | Supplier 응답 스펙 → 내부 포맷 매핑 |
| 에러 처리 | 없음 | 타임아웃, 재시도, Circuit Breaker |

**Supplier 장애 격리**

`AccommodationSearchService`는 각 어댑터 호출을 `runCatching`으로 감싸,
특정 Supplier API가 다운되더라도 자사 내부 숙소 결과는 정상 반환된다.

---

## 5. 성능 설계

### 5.1 요금 조회 캐싱

대규모 동시 요금 조회를 처리하기 위해 Redis 캐싱을 적용한다. `RedisCacheManager`를 사용하므로 서버가 수평 확장되어도 모든 인스턴스가 동일한 캐시를 공유한다.

**캐시별 TTL 및 Eviction 전략:**

| 캐시 | TTL | Evict 시점 |
|------|-----|-----------|
| `accommodation-detail` | 1시간 | 숙소 정보 수정 시 |
| `accommodation-rates` | 5분 | 요금 플랜 등록/수정, DailyRate 변경 시 |
| `accommodation-search` | 2분 | 요금 변경, 재고 변경, 숙소 정보 수정 시 |

캐시 이름은 `CacheNames` 상수 객체에서 관리하여 오타를 방지한다.

**캐싱 적용 범위:**
- 숙소 검색, 요금 조회, 숙소 상세 — 캐시 적용
- 예약 처리 — 캐시 미적용 (실시간 DB 참조 필수)

### 5.2 읽기/쓰기 특성

| 작업 | 특성 | 전략 |
|------|------|------|
| 숙소 검색 | 읽기 집약, 높은 동시성 | Redis 캐싱 |
| 요금 조회 | 읽기 집약, 높은 동시성 | Redis 캐싱 (TTL 5분) |
| 예약 생성 | 쓰기, 강한 일관성 필요 | 비관적 락, DB 직접 쓰기 |
| 재고 변경 | 쓰기, 동시성 민감 | SELECT FOR UPDATE |

---

## 6. 패키지 구조

```
src/main/kotlin/com/ota/platform/
├── property/
│   ├── api/          # Controller (Extranet, Admin)
│   ├── application/  # UseCase
│   ├── domain/       # Entity, Domain Service
│   ├── infrastructure/ # JPA Repository
│   ├── port/         # InventoryPort (interface + DTO)
│   └── adapter/      # InventoryAdapter (inventory 인프라 접근)
│
├── inventory/
│   ├── domain/       # RoomInventory, RoomInventoryService
│   └── infrastructure/ # JPA Repository
│
├── booking/
│   ├── api/          # Controller (Customer API)
│   ├── application/  # UseCase
│   ├── domain/       # Booking, BookingRoom
│   ├── infrastructure/ # JPA Repository
│   ├── event/        # BookingCreatedEvent, BookingCancelledEvent
│   ├── port/         # RoomTypePort, RatePlanPort, InventoryPort (interface + DTO)
│   └── adapter/      # RoomTypeAdapter, RatePlanAdapter, InventoryAdapter
│
├── supplier/
│   ├── port/         # AccommodationPort (interface + DTO)
│   ├── adapter/      # InternalAccommodationAdapter, MockSupplierAAdapter
│   └── application/  # AccommodationSearchService (통합 검색)
│
└── common/
    ├── config/       # Spring 설정 (Redis, JPA, Security)
    ├── exception/    # 공통 예외 처리
    └── response/     # API 응답 형식
```

---

## 7. 기술 스택

| 항목 | 선택 | 이유 |
|------|------|------|
| Language | Kotlin | 간결한 문법, Null Safety, Spring 공식 지원 |
| Framework | Spring Boot 3.4 | 요구사항 명시 |
| Build | Gradle Kotlin DSL | 요구사항 명시 |
| Database | MySQL 8.0 | H2는 SELECT FOR UPDATE 동작이 다름 — 동시성 테스트 신뢰성 확보 |
| Cache | Redis | 대규모 요금 조회 캐싱 |
| ORM | Spring Data JPA + QueryDSL | 복잡한 검색 쿼리 대응 |
| API 문서 | SpringDoc (Swagger UI) | 가산점 항목 |
| 테스트 | JUnit 5 + Testcontainers | MySQL, Redis를 실제 컨테이너로 통합 테스트 |

---

## 8. API 구조

```
/api/extranet/properties          # 파트너: 숙소 등록/조회
/api/extranet/properties/{id}/rooms        # 객실 타입 관리
/api/extranet/properties/{id}/rate-plans   # 요금 플랜 관리
/api/extranet/properties/{id}/inventory    # 재고 설정

/api/customer/accommodations/search        # 숙소 검색 (내부 + Supplier 통합)
/api/customer/accommodations/{id}/rates    # 요금 조회
/api/customer/bookings                     # 예약 생성
/api/customer/bookings/{id}/cancel         # 예약 취소

/api/admin/properties                      # 숙소 관리
/api/admin/bookings                        # 예약 모니터링
/api/admin/partners                        # 파트너 관리
```

---

## 9. 에러 처리 및 로깅 전략

### 9.1 traceId 전파

모든 HTTP 요청에 고유한 `traceId`를 부여하여 요청 단위 로그 추적이 가능하도록 설계한다.

```
클라이언트 요청
    │
    ▼
RequestLoggingFilter
    ├── X-Trace-Id 헤더 있으면 재사용
    ├── 없으면 UUID 16자리 신규 생성
    ├── MDC.put("traceId", id)        → 이후 모든 로그에 자동 포함
    └── 응답 헤더 X-Trace-Id 로 반환  → 클라이언트가 CS 문의 시 제공
```

에러 응답 body에도 `traceId`를 포함시켜, 클라이언트가 서버 로그와 연결할 수 있도록 한다.

```json
{
  "success": false,
  "error": {
    "code": "NOT_FOUND",
    "message": "Property not found: 999",
    "traceId": "a1b2c3d4e5f6g7h8"
  }
}
```

### 9.2 요청/응답 로깅

`RequestLoggingFilter`가 모든 API 요청의 진입/종료 시점을 로깅한다.

```
→ GET /api/customer/accommodations/search?city=서울   (요청 시작)
← GET /api/customer/accommodations/search 200 45ms [OK]  (요청 종료)
← GET /api/customer/bookings 404 12ms [WARN]
← POST /api/customer/bookings 200 3250ms [SLOW]          (3초 초과 감지)
```

슬로우 요청 기준: **3,000ms** 이상 시 `[SLOW]` 태그 부착.

### 9.3 에러 처리 계층화

`GlobalExceptionHandler`에서 예외 유형별로 HTTP 상태 코드와 로그 레벨을 분리한다.

| 예외 유형 | HTTP 상태 | 로그 레벨 | 예시 |
|----------|----------|---------|------|
| `NotFoundException` | 404 | WARN | 존재하지 않는 리소스 조회 |
| `BadRequestException` | 400 | WARN | 잘못된 요청 파라미터 |
| `ConflictException` | 409 | WARN | 재고 부족으로 예약 실패 |
| `MethodArgumentNotValidException` | 400 | WARN | Bean Validation 실패 |
| `MissingServletRequestParameterException` | 400 | WARN | 필수 파라미터 누락 |
| `Exception` (그 외) | 500 | ERROR | 예상치 못한 서버 오류 |

- **4xx**: 예상 가능한 비즈니스 오류 → WARN. 운영 알림 불필요
- **5xx**: 즉시 확인이 필요한 서버 오류 → ERROR. 운영 알림 대상

### 9.4 프로필별 로깅 포맷

`logback-spring.xml`에서 실행 환경에 따라 로깅 포맷을 분리한다.

| 프로필 | 포맷 | 이유 |
|--------|------|------|
| `local`, `development` | 텍스트 (사람이 읽기 쉬운 형태) | 개발 생산성 |
| `alpha`, `beta`, `production` | JSON 구조화 로깅 + 파일 롤링 | ELK 스택 등 로그 수집 시스템 연동 |

**JSON 로그 예시 (production):**
```json
{
  "timestamp": "2026-04-19T14:30:00.123Z",
  "level": "INFO",
  "traceId": "a1b2c3d4e5f6g7h8",
  "logger": "RequestLoggingFilter",
  "message": "← GET /api/customer/accommodations/search 200 45ms [OK]"
}
```

**파일 롤링 정책:** 100MB 단위 분할, 30일 보관, 총 3GB 상한

---

## 10. 구현 범위 및 제외 항목

### 구현할 항목 (핵심 흐름)
- 숙소, 객실 타입, 요금 플랜 등록 (Extranet API)
- 숙소 검색 및 요금 조회 (Redis 캐싱 포함)
- 예약 생성, 취소 (동시성 처리 포함)
- Supplier 통합 (Mock Adapter 1개 구현)
- Admin 기본 조회 API

### 설계만 하고 구현 제외한 항목
- 결제(Payment) 연동 — 외부 PG사 연동 복잡도가 높아 설계 수준으로만 기술
- 동적 가격 산정(RMS) — Daily Rate 직접 입력으로 대체
- 알림(Notification) — 이벤트 설계만 기술
- 인증/인가 — Spring Security 설정 수준으로만 구현

### 제외 이유
7일이라는 기간 내에서 핵심 비즈니스 흐름(등록 → 검색 → 예약 → 취소)의 완성도를 높이는 것이 전체를 얕게 구현하는 것보다 낫다고 판단하였다.

---

## 11. 테스트 전략

### Mock 대신 Testcontainers를 선택한 이유

이 프로젝트의 핵심 비즈니스 로직은 **DB 동작에 직접 의존**한다. Mock으로 Repository를 대체하면 테스트가 통과해도 실제 환경에서 장애가 발생할 수 있는 영역이 존재한다.

| 검증 항목 | Mock의 한계 | Testcontainers 효과 |
|-----------|-------------|---------------------|
| **비관적 락** (SELECT FOR UPDATE) | H2는 MySQL과 락 동작이 달라 동시성 테스트가 의미 없음 | 실제 MySQL 락 경쟁을 재현하여 재고 초과 방지 검증 |
| **Flyway 마이그레이션** | Mock Repository는 DDL을 실행하지 않음 | 실제 스키마 적용 여부와 마이그레이션 무결성 검증 |
| **트랜잭션 격리** | Mock은 트랜잭션 격리 수준을 무시 | 실제 커밋/롤백 및 격리 수준 동작 확인 |
| **Redis 캐시** (직렬화/TTL) | Mock Cache는 직렬화 오류를 잡지 못함 | 실제 Redis로 캐시 동작과 TTL 검증 |
| **JPA 쿼리 어노테이션** | `@Lock(PESSIMISTIC_WRITE)` 등 효과 없음 | 실제 SQL 발행 및 실행 검증 |

가장 결정적인 이유: **동시 예약 시 재고 초과 방지**가 이 시스템의 핵심 요구사항이다. Mock으로 작성한 동시성 테스트는 락 경쟁 자체가 발생하지 않기 때문에, 테스트가 통과해도 실제 DB에서 재고 초과 예약이 발생할 수 있다.

### 테스트 구성

```
단위 테스트 (Spring 컨텍스트 없음)
  RoomInventoryTest     — decrease/increase/stopSell/isAvailable 도메인 로직
  PropertyTest          — 숙소 상태 머신 (PENDING_APPROVAL → ACTIVE → INACTIVE)

통합 테스트 (Testcontainers: MySQL 8.0 + Redis 7)
  BookingIntegrationTest           — 예약 생성/취소/재고 차감·복원
  ConcurrentBookingIntegrationTest — 비관적 락: 재고 1개×10명, 3개×10명 동시 요청
  BookingEdgeCaseIntegrationTest   — stopSell 예약 차단, DailyRate 요금 반영, ratePlan 소속 검증
  ExtranetApiIntegrationTest       — 객실 등록·재고 초기화·요금 오버라이드·stopSell 일괄 설정
  AccommodationSearchIntegrationTest — 내부+Supplier 통합 검색, 재고 필터, 최저가 정렬
```

Testcontainers는 `AbstractIntegrationTest`에서 MySQL·Redis 컨테이너를 **한 번만 기동**하고 전체 테스트 클래스가 공유한다(`companion object` 내 `init` 블록). 덕분에 컨테이너 재시작 비용 없이 격리된 DB 환경을 유지한다.
