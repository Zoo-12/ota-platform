# 과정 기록서 (Progress Journal)

> 작성 기간: 2026-04-16
> 목적: 설계 의사결정 과정, 기술적 문제 해결 과정, AI 활용 내역을 기록한다.

---

## Day 1 - Phase 1: 도메인 리서치 및 아키텍처 설계

### 수행 내용
- OTA 플랫폼 도메인 구조 리서치 (Booking.com, Expedia, Airbnb 사례 분석)
- 핵심 도메인 4개 식별: Property, Inventory, Booking, Supplier
- 아키텍처 스타일 결정 및 문서화
- ERD 초안 설계
- `docs/architecture.md`, `docs/erd.md`, `docs/ai-usage-log.md` 작성

### 의사결정

**[마이크로서비스 vs 모듈형 모놀리스]**
- 선택: 모듈형 모놀리스
- 이유: 마이크로서비스는 분산 트랜잭션, 네트워크 경계 등 복잡도가 높아 7일 과제 범위에서 오버엔지니어링이다. 예약-재고 간 트랜잭션이 로컬 트랜잭션으로 처리 가능한 것도 모놀리스를 선택한 이유다. 단, 도메인 간 직접 엔티티 참조를 금지하고 ID 참조만 허용해 나중에 분리 가능하도록 설계했다.

**[재고 모델: 날짜별 독립 행]**
- 선택: `RoomInventory(room_type_id, date, available_count)` — 날짜별 1행
- 이유: Booking.com, Expedia 공식 개발자 문서에서 확인한 표준 구조다. 날짜별 독립 행이면 `SELECT FOR UPDATE` 잠금 범위가 최소화되고, 특정 날짜만 가용 불가로 설정하는 `stopSell` 같은 운영 기능도 자연스럽게 지원된다.

**[요금 체계: RatePlan + DailyRate 분리]**
- 선택: `RatePlan`(취소 정책, 기본가) + `DailyRate`(날짜별 오버라이드) 2레이어
- 이유: 요금 플랜(조건/정책)과 실제 판매 가격(날짜별)은 변경 주기와 담당자가 다르다. 리서치 결과 실제 OTA도 이 구조를 사용한다.

**[데이터베이스: PostgreSQL → MySQL 변경]**
- 선택: MySQL 8.0
- 이유: 초기 PostgreSQL로 설계했으나, 기존 운영 인프라와의 일관성을 고려해 MySQL로 변경했다. 동시성 테스트(SELECT FOR UPDATE) 측면에서 두 DB 모두 동등하게 지원하므로 기능적 차이는 없다.

**[Supplier 통합: 어댑터 패턴]**
- 선택: `AccommodationPort` 인터페이스 + `InternalAdapter`, `MockSupplierAAdapter`
- 이유: 외부 Supplier마다 API 스펙이 다르다. 어댑터 패턴으로 각 공급사를 동일 인터페이스로 추상화하면 새 Supplier 추가 시 기존 코드를 변경하지 않고 어댑터만 추가하면 된다. 실제 Supplier 대신 Mock으로 통합 검색 흐름 전체를 검증할 수 있는 장점도 있다.

---

## Day 1 - Phase 2: Spring Boot 프로젝트 세팅

### 수행 내용
- Kotlin 2.1.20 + Spring Boot 3.5.0 + Gradle Kotlin DSL 프로젝트 생성
- JDK 21 (Temurin) 설치 및 jenv 등록
- 의존성 설정: JPA, MySQL, Redis, Flyway, SpringDoc, Testcontainers
- `docker-compose.yml` 작성 (MySQL 8.0, Redis 7)
- `application.yml`, `application-local.yml` 설정
- Flyway 마이그레이션 `V1__init_schema.sql` 작성

### 의사결정

**[JDK 21 선택]**
- 선택: JDK 21 (Temurin)
- 이유: 과제 요구사항 명시 (`Java 21+`). LTS 버전으로 Virtual Thread 등 최신 기능 지원.

**[Flyway로 스키마 관리]**
- 선택: Flyway + `ddl-auto: validate`
- 이유: JPA `ddl-auto: create`는 운영 환경에서 위험하다. Flyway로 명시적 마이그레이션 파일을 관리하고, JPA는 검증만 수행하도록 설정했다. 실제 서비스와 동일한 방식이다.

**[Testcontainers 선택]**
- 선택: Testcontainers (MySQL + Redis)
- 이유: H2 인메모리 DB는 `SELECT FOR UPDATE` 동작이 실제 MySQL과 다를 수 있다. 과제 안내에서도 동시성 검증 시 실제 RDBMS 사용을 권고했다. Testcontainers로 CI 환경에서도 실제 MySQL 컨테이너를 띄워 테스트하면 신뢰성이 높다.

---

## Day 1 - Phase 3: 도메인 엔티티 설계 및 구현

### 수행 내용
- Property 도메인: `Partner`, `Property`, `RoomType`, `RatePlan`, `DailyRate`
- Inventory 도메인: `RoomInventory`
- Booking 도메인: `Customer`, `Booking`, `BookingRoom`
- Supplier 도메인: `ExternalSupplier`
- `BaseEntity` (JPA Auditing), Repository 8개, 도메인 서비스 2개 작성

### 의사결정

**[엔티티 필드 구조: 생성자 파라미터 vs 본문 필드]**
- 선택: 생성자 파라미터에는 어노테이션 없이, 본문에서 `var field = param`으로 선언 + `protected set`
- 이유: Kotlin `allOpen` 플러그인이 `@Entity` 클래스를 open으로 만들면 `private set`이 금지된다. `protected set`은 JPA 프록시(서브클래스)가 접근 가능하면서도 외부에서 직접 상태 변경을 막는다. 생성자 파라미터에 어노테이션을 붙이면 어노테이션 위치 지정이 복잡해지므로 본문 분리가 더 명확하다.

**[RoomInventoryService에서 비관적 락 처리]**
- 선택: `@Lock(PESSIMISTIC_WRITE)` + JPQL `IN :dates`로 체크인~체크아웃 날짜 전체 한 번에 잠금
- 이유: 날짜별로 순차적으로 잠금을 획득하면 교착 상태(Deadlock) 가능성이 있다. 여러 날짜를 한 쿼리로 잠금하면 잠금 순서가 일정해져 안전하다.

**[BookingRoom에서 BaseEntity 제외]**
- 선택: `BookingRoom`은 `BaseEntity` 미상속, `createdAt`만 보유
- 이유: 예약 날짜별 상세 레코드는 생성 후 변경되지 않는 불변 데이터다. `updated_at`이 의미없는 컬럼이 되므로 처음부터 제외하는 것이 설계적으로 명확하다.

**[RateCalculationService 도메인 서비스 분리]**
- 선택: `RateCalculationService`를 별도 도메인 서비스로 분리
- 이유: 요금 계산 로직(DailyRate 우선 + basePrice 폴백)은 Property 도메인의 순수 비즈니스 규칙이다. UseCase에 직접 넣으면 Booking UseCase도 동일 로직을 반복 구현해야 한다. 도메인 서비스로 분리하면 재사용성이 높아진다.

---

## Day 1 - Phase 4: UseCase, Controller 구현

### 수행 내용
- Supplier 통합 검색: `AccommodationPort`, `InternalAccommodationAdapter`, `MockSupplierAAdapter`, `AccommodationSearchService`
- Property UseCase 5개: `PartnerUseCase`, `PropertyUseCase`, `RoomTypeUseCase`, `RatePlanUseCase`, `InventoryUseCase`
- Booking UseCase 3개: `CreateBookingUseCase`, `CancelBookingUseCase`, `GetBookingUseCase`
- Controller: `ExtranetController`, `CustomerController`, `AdminController`
- 공통: `ApiResponse<T>`, `GlobalExceptionHandler`, `SecurityConfig`, `RedisConfig`

### 의사결정

**[Command 객체 패턴]**
- 선택: Controller → UseCase 간 데이터 전달에 Command 객체 사용 (`CreateBookingCommand` 등)
- 이유: HTTP Request DTO가 UseCase에 직접 들어오면 UseCase가 HTTP 레이어에 의존하게 된다. Command 객체로 분리하면 UseCase는 HTTP를 모르는 순수 비즈니스 로직이 된다.

**[CreateBookingUseCase 트랜잭션 설계]**
- 선택: 요금 계산 → 재고 잠금/차감 → Booking 저장을 단일 `@Transactional` 안에서 처리
- 이유: 요금 계산과 재고 차감, 예약 저장이 원자적으로 처리되어야 한다. 요금 계산 후 재고 차감 실패 시 요금만 계산된 불완전한 상태가 남으면 안 된다.

**[AccommodationSearchService 에러 격리]**
- 선택: 각 어댑터 호출을 `runCatching`으로 감싸 개별 어댑터 장애가 전체 검색에 영향 미치지 않도록 처리
- 이유: 외부 Supplier API가 다운되더라도 자사 내부 숙소 검색 결과는 정상 반환되어야 한다. 장애 격리(Fault Isolation)는 외부 연동에서 필수적이다.

**[Admin 권한 처리]**
- 설계만 수행, 구현 미완: 실제 서비스에서는 JWT 기반 인증 + `ROLE_ADMIN` 권한 체크가 필요하다. 과제 범위 내에서는 `SecurityConfig`에서 모든 요청을 허용하는 수준으로만 구현했으며, 인증/인가 설계는 `docs/architecture.md`에 기술했다.

---

## Day 1 - Phase 5: 통합 테스트 작성 및 디버깅

### 수행 내용
- `AbstractIntegrationTest`: Testcontainers MySQL + Redis 기반 컨텍스트 공유
- `TestFixtures`: 공통 테스트 데이터 셋업 헬퍼
- `ExtranetApiIntegrationTest`: 숙소 등록, 재고 초기화, 요금 오버라이드, stopSell 테스트 (4개)
- `BookingIntegrationTest`: 예약 성공, 취소+재고복원, 중복취소 예외, 재고없음 예외 테스트 (4개)
- `ConcurrentBookingIntegrationTest`: 재고 1개 × 10명, 재고 3개 × 10명 동시 예약 테스트 (2개)
- `AccommodationSearchIntegrationTest`: 통합 검색, 재고 제외, 최저가 정렬, 도시 필터 테스트 (4개)
- **최종: 16개 테스트 전체 통과**

### 의사결정

**[H2 대신 Testcontainers MySQL 사용]**
- 선택: Testcontainers로 실제 MySQL 8.0 컨테이너 사용
- 이유: `SELECT FOR UPDATE`(비관적 락)는 H2가 MySQL과 동작이 다르다. 동시성 테스트 신뢰성 확보를 위해 실제 DB가 필수다.

**[동시성 테스트에서 @Transactional 미사용]**
- 선택: `ConcurrentBookingIntegrationTest`에 `@Transactional` 미적용
- 이유: `@Transactional`이 붙으면 테스트 메서드 전체가 하나의 트랜잭션으로 감싸지는데, 스레드별로 독립 트랜잭션이 필요한 동시성 테스트에는 부적합하다. 각 스레드가 별도 트랜잭션을 시작해야 비관적 락 경합이 실제로 발생한다.

**[CountDownLatch로 동시 출발 보장]**
- 선택: `startLatch.countDown()` 한 번으로 N개 스레드를 동시에 출발시킴
- 이유: 스레드를 순차적으로 시작하면 앞선 트랜잭션이 이미 완료된 후 다음 스레드가 시작되어 경합이 발생하지 않는다. `CountDownLatch(1)`로 모든 스레드가 준비된 후 동시에 시작해야 의미있는 동시성 테스트가 된다.

### 기술적 문제 해결

**문제 1: `scale has no meaning for SQL floating point types` (Hibernate 6)**
- 원인: `Double` 타입 필드에 `@Column(precision=10, scale=7)` 지정 — Hibernate 6은 부동소수점 타입에 `scale` 지정을 거부
- 해결: `latitude`, `longitude`, `sizeSqm` 필드의 `@Column`에서 `precision`, `scale` 제거. SQL 스키마도 `DECIMAL` → `DOUBLE`로 변경하여 타입 일치

**문제 2: `Schema-validation: missing column [updated_at] in table [booking_rooms]`**
- 원인: `BookingRoom`이 `BaseEntity`를 상속해 `updated_at`을 기대하지만, SQL에 해당 컬럼 없음
- 해결: `BookingRoom`은 불변 레코드이므로 `BaseEntity` 상속 제거, `createdAt`만 직접 선언

**문제 3: `MysqlDataTruncation` — businessNumber 컬럼 초과**
- 원인: `TestFixtures`에서 `System.nanoTime()` (19자리)을 suffix로 사용 → `"123-45-{19자리}"` = 26자 > `VARCHAR(20)`
- 해결: `(1..999999).random()`으로 6자리 이내 랜덤 숫자 사용

**문제 4: `MySQLContainer<Nothing>` 빌더 메서드 호출 불가**
- 원인: Kotlin에서 `Nothing`을 제네릭 타입으로 지정하면 빌더 메서드가 `Nothing`을 반환해 호출 불가
- 해결: `MySQLContainer("mysql:8.0").apply { ... }` 패턴으로 변경. `apply` 블록은 수신 객체를 직접 참조해 반환 타입 문제를 우회

---

## 미구현 항목 및 이유

| 항목 | 미구현 이유 |
|------|-----------|
| 결제(Payment) 연동 | PG사 연동 복잡도 높음. 비즈니스 핵심 흐름(예약/취소)에 집중하기 위해 제외 |
| JWT 인증/인가 | 인증 구현보다 도메인 로직 완성도를 높이는 것이 과제 평가에 더 적합하다 판단 |
| 동적 가격 산정(RMS) | `DailyRate` 직접 입력으로 대체. 자동 가격 산정 알고리즘은 과제 범위 초과 |
| 알림(Notification) | 이벤트 기반 설계(`Booking` 상태 변경 → 이벤트 발행)는 `architecture.md`에 설계만 기술 |
| 성능 테스트 | 기능 구현 및 통합 테스트 완성 후 시간 여유가 있을 때 추가 예정 |

---

## AI 활용 요약

이 과제 전반에 걸쳐 Claude Code(AI)를 활용했다. 주요 활용 내역은 `docs/ai-usage-log.md`에 기록했다.

핵심 원칙: **AI가 제안한 내용을 그대로 사용하지 않고, 각 결정마다 직접 검토 후 수정 또는 채택했다.**

주요 수정 사항:
- AI 제안 마이크로서비스 → 모듈형 모놀리스로 변경 (과제 범위 고려)
- AI 제안 PostgreSQL → MySQL로 변경 (인프라 일관성)
- AI 생성 테스트 픽스처의 `System.nanoTime()` → `(1..999999).random()`으로 수정 (실제 실행 시 발생한 버그)
- Hibernate 6 + Kotlin `allOpen` 조합에서 발생하는 `private set` 제약 → `protected set` 패턴 적용 (컴파일 에러 직접 해결)
