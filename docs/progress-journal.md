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
- 이유: 마이크로서비스는 분산 트랜잭션, 네트워크 경계 등 복잡도가 높아 7일 구현 범위에서 오버엔지니어링이다. 예약-재고 간 트랜잭션이 로컬 트랜잭션으로 처리 가능한 것도 모놀리스를 선택한 이유다. 단, 도메인 간 직접 엔티티 참조를 금지하고 ID 참조만 허용해 나중에 분리 가능하도록 설계했다.

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
- 이유: 요구사항 명시 (`Java 21+`). LTS 버전으로 Virtual Thread 등 최신 기능 지원.

**[Flyway로 스키마 관리]**
- 선택: Flyway + `ddl-auto: validate`
- 이유: JPA `ddl-auto: create`는 운영 환경에서 위험하다. Flyway로 명시적 마이그레이션 파일을 관리하고, JPA는 검증만 수행하도록 설정했다. 실제 서비스와 동일한 방식이다.

**[Testcontainers 선택]**
- 선택: Testcontainers (MySQL + Redis)
- 이유: H2 인메모리 DB는 `SELECT FOR UPDATE` 동작이 실제 MySQL과 다를 수 있다. 동시성 검증 신뢰성을 위해 실제 RDBMS가 필수다. Testcontainers로 CI 환경에서도 실제 MySQL 컨테이너를 띄워 테스트하면 신뢰성이 높다.

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
- 설계만 수행, 구현 미완: 실제 서비스에서는 JWT 기반 인증 + `ROLE_ADMIN` 권한 체크가 필요하다. 현재 구현 범위에서는 `SecurityConfig`에서 모든 요청을 허용하는 수준으로만 구현했으며, 인증/인가 설계는 `docs/architecture.md`에 기술했다.

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

## Day 2 - 로컬 환경 구축 / 캐싱 / 상세 API / 프론트엔드 / 로깅 전략

### 수행 내용
- docker-compose healthcheck 추가 및 `start-local.sh` 작성 (인프라 → 서버 순차 기동 자동화)
- Flyway V2 시드 데이터 작성 (파트너 2, 숙소 4, 객실 6종, 요금제 12개, 90일치 재고)
- `@Cacheable` 적용: 검색 / 요금 조회 / 숙소 상세 (Redis, TTL 5분)
- 숙소 상세 조회 API 추가: `GET /api/customer/accommodations/{id}` (객실 타입 + 요금제 포함)
- Admin 숙소 재활성화 API 추가 (`INACTIVE → ACTIVE`)
- Next.js 14 프론트엔드 구축: 고객 검색/예약, Extranet 5단계 마법사, Admin 관리 화면
- 에러 처리 및 로깅 전략 구현: `RequestLoggingFilter`, `GlobalExceptionHandler` 강화, `logback-spring.xml`
- 프론트 TraceBar: 최근 10개 요청의 traceId / 상태코드 / 응답시간 표시

### 의사결정

**[Flyway 시드 데이터 전략]**
- 선택: 별도 스크립트 대신 V2 마이그레이션으로 관리
- 이유: Flyway가 버전 순서를 보장하므로 스키마 생성(V1) 이후 시드(V2)가 반드시 순서대로 실행된다. Recursive CTE로 오늘 날짜 기준 90일치 재고를 자동 생성해 서버 기동 시점과 무관하게 유효한 데이터가 유지된다.

**[Redis 캐싱 직렬화 방식 선택]**
- 선택: `Jackson2JsonRedisSerializer<Any>` + `activateDefaultTypingAsProperty("@class")`
- 이유: Spring Data Redis 3.5.0에서 `GenericJackson2JsonRedisSerializer` 기본 생성자가 타입 정보를 포함하지 않는 방식으로 동작해 역직렬화 시 `LinkedHashMap`으로 반환되는 문제가 발생했다. `activateDefaultTypingAsProperty`로 `@class` 필드를 JSON에 명시적으로 포함시켜 정확한 타입 복원을 보장하는 방식으로 해결했다.

**[숙소 상세 API: 기존 Port 인터페이스 확장]**
- 선택: 별도 레이어 없이 `AccommodationPort`에 `getDetail()` 추가
- 이유: 이미 내부/외부 Supplier를 동일 인터페이스로 추상화한 구조가 있으므로 새 메서드를 추가하는 것이 일관성 있다. 새 서비스를 만들면 어댑터 패턴의 이점이 없어진다.

**[traceId 전파 전략]**
- 선택: 외부 `X-Trace-Id` 헤더 수신 시 재사용, 없으면 서버에서 UUID 16자리 생성 → MDC 저장 → 응답 헤더로 반환
- 이유: 클라이언트(프론트)가 traceId를 직접 생성해 넘길 수 있어 프론트-백 로그 연결이 가능하다. MDC에 저장하면 모든 로그에 자동 포함되어 단일 요청 추적이 용이하다.

**[로깅 레벨 전략]**
- 4xx: WARN — 예상 가능한 비즈니스 오류, 운영 알림 불필요
- 5xx: ERROR — 즉시 확인 필요, 운영 알림 대상
- 슬로우 요청(3초 이상): WARN + `[SLOW]` 태그

**[프로필별 로깅 포맷 분리]**
- local: 사람이 읽기 쉬운 텍스트 포맷 (`[traceId]` 포함)
- production: JSON 구조화 로깅 (`logstash-logback-encoder`) + 파일 롤링 (100MB/30일/3GB 상한)
- 이유: JSON 포맷은 ELK 스택 등 로그 수집 시스템과 바로 연동 가능하다. 로컬에서 JSON을 보면 가독성이 크게 떨어지므로 프로필로 분리하는 것이 적절하다.

### 기술적 문제 해결

**문제 1: Redis 역직렬화 — `LinkedHashMap cannot be cast`**
- 원인: `@Cacheable` 적용 후 첫 요청(캐시 미스)은 성공하지만 두 번째 요청(캐시 히트) 시 Redis에서 읽은 JSON이 타입 정보 없이 `LinkedHashMap`으로 역직렬화됨
- 해결 과정: `GenericJackson2JsonRedisSerializer` 기본 생성자 → 커스텀 ObjectMapper에 `activateDefaultTyping(NON_FINAL)` → `As.PROPERTY` 방식 시도 순으로 진행했으나, Spring Data Redis 3.5.0에서 내부 `JacksonObjectReader`와 포맷 충돌 발생. `Jackson2JsonRedisSerializer<Any>` + `activateDefaultTypingAsProperty("@class")` 조합으로 최종 해결

**문제 2: 브라우저에서 `X-Trace-Id` 헤더 미수신**
- 원인: CORS 정책상 브라우저는 기본적으로 커스텀 응답 헤더를 JavaScript에서 읽지 못함
- 해결: `SecurityConfig`의 CORS 설정에 `exposedHeaders = listOf("X-Trace-Id")` 추가 (`Access-Control-Expose-Headers`)

---

---

## Day 3 - 이벤트 기반 아키텍처 / 성능 테스트 / UI 개선

### 수행 내용
- `BookingCreatedEvent`, `BookingCancelledEvent` 도메인 이벤트 클래스 작성
- `CreateBookingUseCase`, `CancelBookingUseCase`에 `ApplicationEventPublisher` 연동
- `@TransactionalEventListener` + `@Async` 리스너 구현 (예약 후처리 비동기 처리)
- k6를 활용한 성능 테스트 작성 및 실행 (`docs/performance-test.md`)
  - 요금 조회 캐시 효과 테스트 (VU 50명 × 30초, 379 req/s, p95 24ms)
  - 동시 예약 부하 테스트 (VU 20명, 재고 정합성 검증)
- Next.js 프론트엔드 통합: Spring Boot static 리소스로 빌드 산출물 포함
- Extranet UI 개선: 각 단계 하단 다음 버튼, 파트너 미선택 안내 문구
- TraceBar 드래그 리사이즈, Request/Response Body 상세 펼치기 기능
- 화면 내 영어 enum 값 전체 한국어 치환 (`labels.ts` 유틸)

### 의사결정

**[이벤트 리스너에서 Virtual Thread 선택]**
- 선택: `@Async` + `spring.threads.virtual.enabled: true`
- 이유: JDK 21 환경에서 코루틴 사용도 고려했으나, 단순 이벤트 후처리 수준에서 코루틴은 오버엔지니어링이다. Virtual Thread 활성화 한 줄로 `@Async`가 Virtual Thread 위에서 실행되어 블로킹 I/O 비용을 줄일 수 있다.

**[@TransactionalEventListener 사용 이유]**
- 선택: `@EventListener` 대신 `@TransactionalEventListener(phase = AFTER_COMMIT)`
- 이유: 예약 트랜잭션이 커밋되기 전에 이벤트가 처리되면 예약이 최종 실패하더라도 이메일/알림이 발송되는 문제가 생긴다. 커밋 후 실행을 보장해야 후처리의 신뢰성이 높아진다.

**[프론트를 Spring Boot static 리소스로 통합]**
- 선택: Next.js `output: 'export'`로 정적 빌드 후 `src/main/resources/static`에 포함
- 이유: 서버를 하나만 실행하면 프론트+백엔드 모두 접근 가능하다. 별도 포트(3000)로 운영하면 CORS 설정이 복잡해지고 실행 절차가 늘어난다. 데모 및 평가 환경에서 단순성이 더 중요하다.

**[성능 테스트 도구 선택]**
- 선택: k6
- 이유: 스크립트 기반으로 시나리오를 코드로 관리할 수 있고, VU/RPS 제어가 직관적이다. Gatling 대비 설정이 가볍고 CI 통합도 용이하다.

---

## Day 4 - 테스트 보강 / 버그 수정 / 파트너 승인 플로우

### 수행 내용
- 도메인 단위 테스트 작성: `RoomInventoryTest`, `PropertyTest`
- 예약 엣지 케이스 통합 테스트: `BookingEdgeCaseIntegrationTest`
- `start-local.sh` 실행 방식 단순화 (프론트엔드 빌드 단계 분리)
- Swagger UI가 Petstore 기본 예제를 표시하는 버그 수정
- 브라우저 새로고침 시 `/admin/`, `/extranet/` 경로 500 오류 수정
- 파트너 승인 플로우 구현: Extranet 등록 → Admin 승인 → 다음 단계 진행

### 의사결정

**[단위 테스트와 통합 테스트 분리 전략]**
- 선택: Spring 컨텍스트 없이 검증 가능한 도메인 로직은 순수 단위 테스트(`RoomInventoryTest`, `PropertyTest`), 레이어 간 연동이 필요한 흐름은 통합 테스트
- 이유: 통합 테스트는 컨텍스트 로딩 비용이 크다. 도메인 규칙(재고 차감 로직, 엔티티 상태 전환 등)은 DB 없이도 검증 가능하므로 단위 테스트로 분리하면 피드백 속도가 빠르다.

**[파트너 승인 UX 흐름 설계]**
- 선택: 파트너 등록 → 모달 안내("Admin 승인 필요") → Admin 화면으로 이동 → 승인 → 돌아와서 파트너 선택
- 이유: 승인 없이 다음 단계로 진행하면 미승인 파트너 데이터가 생성될 수 있다. 단계 진입 전 승인 여부를 프론트에서 검증하는 것이 더 명확한 UX다.

### 기술적 문제 해결

**문제 1: Swagger UI가 Petstore 예제 표시**
- 원인: `SpaRoutingFilter`가 `/api-docs`, `/swagger-ui` 경로도 SPA 라우팅 대상으로 가로채 `/index.html`로 포워딩
- 해결: 필터에서 `/api-docs`, `/swagger-ui`, `/actuator` 경로를 명시적으로 제외

**문제 2: SPA 경로 새로고침 시 500 오류**
- 원인: `@Controller`에서 `/index.html`로 포워딩 시 필터가 재진입해 무한 포워딩 발생
- 해결: `OncePerRequestFilter` 방식으로 전환하여 동일 요청 내 재진입 방지

---

## Day 5 - 도메인 간 의존성 리팩터링 / 아키텍처 설계 정리

### 수행 내용
- 크로스 도메인 Repository 직접 참조 전수 조사
- 도메인 간 의존성 전체를 포트/어댑터 패턴으로 분리
  - `booking/port`: `RoomTypePort`, `RatePlanPort`, `InventoryPort`
  - `booking/adapter`: 각 포트의 JPA 구현체
  - `property/port`: `InventoryPort`
  - `property/adapter`: `InventoryAdapter`
- `AccommodationSearchService`의 구체 Adapter 직접 주입 문제 수정 (`canHandle()` 패턴으로 통일)
- 캐시 무효화(`@CacheEvict`) 누락 항목 수정 및 캐시별 TTL 분리
- 캐시 이름 상수 추출 (`CacheNames` 오브젝트)
- MSA 전환 시 서비스 경계 설계 검토

### 의사결정

**[포트/어댑터 패턴 분리 범위]**
- 선택: booking↔property/inventory, property↔inventory 간 의존성을 포트/어댑터로 분리. `InternalAccommodationAdapter`의 property/inventory 직접 참조는 현행 유지
- 이유: `InternalAccommodationAdapter` 자체가 "supplier ↔ property 경계를 넘는 어댑터" 역할이므로 내부에 추가 추상화 레이어를 두는 것은 오버엔지니어링이다. 나머지 크로스 도메인 의존은 인프라 레이어 직접 참조이므로 분리가 필요하다.

**[MSA 분리 시 서비스 경계 결정]**
- 선택: `[booking 서비스]` | `[property + inventory + supplier 서비스]` 2개 서비스로 분리
- 이유: supplier의 `InternalAccommodationAdapter`가 property/inventory 데이터를 직접 읽는 구조상 세 도메인은 자연스럽게 같은 배포 단위에 속한다. booking은 예약 트랜잭션만 담당해 분리 경계가 명확하다. 포트/어댑터로 분리해두면 MSA 전환 시 레포지토리 구현체를 HTTP 어댑터로 교체하는 것만으로 전환 가능하다.

**[캐시 TTL 분리 전략]**
- 선택: 숙소 상세(긴 TTL) / 검색 결과(중간 TTL) / 요금(짧은 TTL) 분리
- 이유: 데이터 변경 빈도가 다르다. 요금과 재고는 자주 바뀌므로 TTL을 짧게 유지해야 캐시 오염을 방지할 수 있다. 상세 정보는 상대적으로 변경이 적어 긴 TTL이 적합하다.

---

## 미구현 항목 및 이유

| 항목 | 미구현 이유 |
|------|-----------|
| 결제(Payment) 연동 | PG사 연동 복잡도 높음. 비즈니스 핵심 흐름(예약/취소)에 집중하기 위해 제외 |
| JWT 인증/인가 | 인증 구현보다 도메인 로직 완성도를 높이는 것이 우선이라 판단. 설계는 `architecture.md`에 기술 |
| 동적 가격 산정(RMS) | `DailyRate` 직접 입력으로 대체. 자동 가격 산정 알고리즘은 구현 범위 초과 |
| 알림(Notification) | 이벤트 기반 구조(`@TransactionalEventListener`)는 구현 완료. 실제 이메일/푸시 발송 연동은 제외 |
| 성능 테스트 | ~~예정~~ → **완료** (`docs/performance-test.md`). k6로 캐시 효과 및 동시 예약 부하 검증 |

---

## AI 활용 요약

이 프로젝트 전반에 걸쳐 Claude Code(AI)를 활용했다. 주요 활용 내역은 `docs/ai-usage-log.md`에 기록했다.

핵심 원칙: **AI가 제안한 내용을 그대로 사용하지 않고, 각 결정마다 직접 검토 후 수정 또는 채택했다.**

주요 수정 및 판단 사항:
- AI 제안 마이크로서비스 → 모듈형 모놀리스로 변경 (구현 범위 및 복잡도 고려)
- AI 제안 PostgreSQL → MySQL로 변경 (인프라 일관성)
- AI 생성 테스트 픽스처의 `System.nanoTime()` → `(1..999999).random()`으로 수정 (실제 실행 시 발생한 버그)
- Hibernate 6 + Kotlin `allOpen` 조합에서 발생하는 `private set` 제약 → `protected set` 패턴 적용 (컴파일 에러 직접 해결)
- AI 제안 SlowQueryLog AOP 구현 → 필터 단 슬로우 요청 감지로 단순화 (중복 제거)
- AI 제안 `@Async` 그대로 수용 → Virtual Thread 활성화로 JDK 21 기능 활용 (직접 질문하여 결정)
- 포트/어댑터 분리 범위에서 `InternalAccommodationAdapter` 제외 결정 (추가 추상화가 오버엔지니어링이라 판단)
- MSA 서비스 경계를 2개(booking / property+inventory+supplier)로 직접 설계
