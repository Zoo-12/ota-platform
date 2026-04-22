# AI 활용 기록

> 이 프로젝트에서 AI(Claude Code)를 활용한 내역을 기록합니다.
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
- AI가 마이크로서비스 분리를 권장했으나, 7일 구현 범위를 고려해 **모듈형 모놀리스**로 조정
- 동적 가격(RMS 연동)은 실제 구현 범위 밖이므로 설계 문서에만 기술하고 구현은 단순 Daily Rate 조회로 처리
- Supplier 통합에서 AI가 여러 어댑터를 제안했으나, Mock Supplier 1개만 구현하고 나머지는 인터페이스로만 정의하는 방향으로 범위 축소
- H2 대신 MySQL 사용 — 동시성 테스트 신뢰성을 위한 직접 판단

---

## 2026-04-18

### 활용 내용: 로컬 환경 구축 / 숙소 상세 API / Redis 캐싱 / Next.js 프론트엔드

**AI에게 요청한 내용:**
- docker-compose healthcheck 및 start-local.sh 작성
- Flyway V2 시드 데이터 SQL 작성 (파트너, 숙소, 객실, 요금제, 90일치 재고)
- `@Cacheable` 적용 (검색, 요금 조회, 숙소 상세)
- 숙소 상세 조회 API 추가 (`GET /api/customer/accommodations/{id}`)
- Next.js 14 프론트엔드 전체 구현 (고객 / Extranet / Admin)

**본인 판단 및 수정 사항:**
- Redis 캐싱 직렬화 오류(`LinkedHashMap cannot be cast`) 발생 → Spring Data Redis 3.5.0 동작 변경을 직접 파악하고 `Jackson2JsonRedisSerializer` + `activateDefaultTypingAsProperty("@class")` 조합으로 해결 지시

---

## 2026-04-19

### 활용 내용: 에러 처리 및 로깅 전략 설계·구현

**AI에게 요청한 내용:**
- 실무 수준의 로깅 전략 구조 제안 (요청/응답 로깅, 에러 계층화, 구조화 로깅)
- `RequestLoggingFilter` 구현 (MDC traceId 전파, 요청/응답 로그, 슬로우 요청 감지)
- `GlobalExceptionHandler` 계층화 (4xx/5xx 분리, 프레임워크 예외 추가)
- `logback-spring.xml` 프로필별 설정 (local 텍스트, production JSON 구조화 로깅)
- 프론트엔드 traceId 표시 컴포넌트 구현 (최근 10개 요청 목록)

**본인 판단 및 수정 사항:**
- AI가 SlowQueryLog를 AOP로 구현하는 방안을 제안했으나, 필터 레벨에서 처리 시간을 이미 측정하고 있어 중복이라 판단 — 필터 단의 슬로우 요청 감지(3초 임계치)로 단순화
- 브라우저에서 `X-Trace-Id` 헤더를 JS로 읽지 못하는 문제가 발생 → CORS `Access-Control-Expose-Headers` 설정 누락이 원인임을 직접 파악하고 수정 지시
- 프론트 TraceBar의 기본 상태를 닫힘으로 구현했으나, 테스트 편의를 위해 기본 열림으로 변경 요청 — 테스트 도구로서의 UX를 직접 판단
- `logstash-logback-encoder` 버전은 Spring Boot 3.5 호환성을 직접 확인 후 8.0으로 확정

---

## 2026-04-19 (2)

### 활용 내용: 이벤트 기반 아키텍처 구현

**AI에게 요청한 내용:**
- 예약 생성/취소 시 이벤트 발행 구조 설계 및 구현
- `BookingCreatedEvent`, `BookingCancelledEvent` 클래스 작성
- `CreateBookingUseCase`, `CancelBookingUseCase`에 `ApplicationEventPublisher` 연동
- `@TransactionalEventListener` + `@Async` 리스너 구현

**본인 판단 및 수정 사항:**
- AI가 `@Async`로 구현했으나, JDK 21 환경에서 코루틴 또는 Virtual Thread 사용 가능성을 직접 질문
- 코루틴은 단순 이벤트 후처리 수준에서 오버엔지니어링이라는 판단 하에 Virtual Thread 활성화로 결정
- `spring.threads.virtual.enabled: true` 한 줄로 `@Async`가 Virtual Thread 위에서 실행됨을 확인 후 적용

---

## 2026-04-20

### 활용 내용: 프론트엔드 통합 / UI 개선

**AI에게 요청한 내용:**
- Next.js 정적 빌드 후 Spring Boot static 폴더에 포함하는 구조 설계 및 build-frontend.sh 작성
- 숙소 승인/비활성화/재활성화 시 Redis 검색 캐시 무효화 (@CacheEvict)
- 파트너 목록 조회 API 추가 및 Extranet 화면에 자동 조회 적용
- Admin 화면 탭 진입 시 자동 조회
- Extranet 각 단계 하단 다음 버튼 추가, 파트너 미선택 시 안내 문구
- TraceBar 드래그 리사이즈, Request/Response Body 상세 펼치기
- 화면의 영어 enum 값 전체 한국어 치환 (labels.ts 유틸)

**본인 판단 및 수정 사항:**
- 프론트를 별도 포트(3000)로 운영하는 대신 Spring Boot static 리소스로 통합하는 방향 직접 결정 → 서버 하나만 실행하면 프론트+백엔드 모두 접근 가능
- TraceBar 높이 고정 대신 드래그 리사이즈 요청 → 사용성 직접 판단
- 영어 enum 값 노출이 사용자 경험에 좋지 않다는 점 직접 파악 후 한국어 치환 요청

---

## 2026-04-20 (2)

### 활용 내용: 테스트 보강 / 버그 수정 / 파트너 승인 플로우 구현

**AI에게 요청한 내용:**

- 기존 통합 테스트 전체 설명 및 누락된 테스트 파악
- 도메인 단위 테스트 작성 (`RoomInventoryTest`, `PropertyTest`)
- 예약 엣지 케이스 통합 테스트 작성 (`BookingEdgeCaseIntegrationTest`)
- `start-local.sh` 실행 방식 단순화 (프론트엔드 빌드 단계 분리)
- Swagger UI가 Petstore 기본 예제를 표시하는 버그 수정
- 브라우저 새로고침 시 `/admin/`, `/extranet/` 경로에서 500 오류 수정
- 파트너 승인 플로우 구현 (Extranet 등록 → Admin 승인 → 다음 단계 진행)

**본인 판단 및 수정 사항:**

- Mock 대신 Testcontainers를 선택한 이유를 정리하고 architecture.md 테스트 전략 섹션에 반영 요청
- 단위 테스트와 통합 테스트를 구분하여 Spring 컨텍스트 없이 검증 가능한 도메인 로직은 순수 단위 테스트로 분리하도록 방향 제시
- Swagger 버그의 원인을 `SpaRoutingFilter`가 `/api-docs` 경로를 가로채는 문제로 직접 파악 후 수정 지시
- 새로고침 500 오류의 원인을 `@Controller` 포워딩 시 순환 참조로 직접 파악 → `OncePerRequestFilter` 방식으로 전환 지시
- 파트너 승인 UX 흐름(등록 → 모달 → Admin 이동 → 승인 → 돌아와서 선택)을 직접 설계하고 구현 요청

## 2026-04-22

### 활용 내용: 도메인 간 의존성 포트/어댑터 패턴 분리 및 캐싱 전략 개선

**AI에게 요청한 내용:**
- 현재 구조의 크로스 도메인 의존성 분석 (Repository 직접 참조 현황 파악)
- MSA 분리 전략 설계 (서비스 경계 및 분리 순서 계획)
- 도메인 간 모든 크로스 도메인 호출을 포트/어댑터 패턴으로 전환
  - `booking/port` — `RoomTypePort`, `RatePlanPort`, `InventoryPort` 생성
  - `booking/adapter` — 각 포트의 JPA 구현체 생성
  - `property/port` — `InventoryPort` 생성
  - `property/adapter` — `InventoryAdapter` 생성
- `AccommodationSearchService`의 구체 Adapter 직접 주입 문제 수정 (`canHandle()` 패턴 적용)
- 캐시 무효화(`@CacheEvict`) 누락 문제 수정 및 캐시별 TTL 분리
- 캐시 이름 상수 추출 (`CacheNames` 오브젝트)

**본인 판단 및 수정 사항:**
- AI가 MSA 분리 로드맵을 제안했으나, 즉각 분리가 아닌 모놀리스 내 포트/어댑터 선 적용으로 방향 결정 — 분리 시 UseCase 코드 변경 없이 Adapter 구현체만 교체하면 되는 구조 확보
- 비관적 락 선택 이유를 "예약 트래픽이 검색 대비 현저히 낮아 락 경합 비용이 낮다"로 직접 정리하여 문서화 요청
- Redis 분산 락 전환 시점 기준(커넥션 풀 고갈 우려 시)을 직접 판단하여 아키텍처 문서에 반영

## 2026-04-22 (2)

### 활용 내용: 아키텍처 설계 토론 — 도메인 경계 및 MSA 분리 전략

**AI에게 질문한 내용:**
- `InternalAccommodationAdapter`가 포트/어댑터 패턴 없이 property 도메인 레포지토리를 직접 참조하는 것이 맞는지 검토 요청
- MSA 전환 시 supplier 서비스 배포 단위 및 경계 확인
- 나머지 도메인도 포트/어댑터로 분리한 이유가 MSA를 고려하지 않으면 의미 없는 것인지 질문

**논의 결과 및 본인 판단:**
- `InternalAccommodationAdapter`는 supplier 도메인이 property+inventory 레포지토리를 직접 참조하는 구조 — 원칙상으로는 포트/어댑터로 분리하는 것이 일관성에 맞지만, 해당 어댑터 자체가 이미 "supplier ↔ property 경계를 넘는 어댑터" 역할이므로 현재 구조 유지로 결정
- MSA 분리 시 적절한 서비스 경계는 **[booking 서비스] | [property+inventory+supplier 서비스]** 2개로 판단 — supplier가 property/inventory 데이터를 직접 읽는 구조상 세 도메인은 자연스럽게 같은 배포 단위에 속함
- 포트/어댑터 패턴은 MSA 외에도 모놀리스 내 도메인 로직 격리, 테스트 mock 교체, 향후 분리 시 경계 명확화 등의 가치가 있으므로 다른 도메인의 분리는 여전히 유효하다고 판단

---

## 2026-04-22 (3)

### 활용 내용: 테스트 커버리지 측정 (JaCoCo)

**AI에게 요청한 내용:**
- JaCoCo 플러그인 설정 및 커버리지 리포트 생성
- 측정 대상 제외 범위 결정 (Controller, Config, DTO 등)

**본인 판단 및 수정 사항:**
- 결과: Instruction 54%, Branch 37%
- 수치가 낮은 이유를 직접 분석 — 통합 테스트 위주 설계로 JaCoCo가 실제 실행 경로를 일부 미집계하는 구조임을 파악. 핵심 비즈니스 로직은 실제 DB/캐시 환경에서 검증되고 있으므로 수치 대비 실질적 신뢰도는 높다고 판단
- 커버리지 수치 및 해석을 `docs/architecture.md` 테스트 전략 섹션에 반영

---

## 2026-04-22 (4)

### 활용 내용: API 레이어 리팩터링 — 컨트롤러 로직 분리 및 DTO 구조 정리

**AI에게 요청한 내용:**
- 컨트롤러에 남아있는 비즈니스 로직(레포지토리 직접 참조, 데이터 조립)을 UseCase로 분리
- 컨트롤러 파일 하단에 정의된 request/response 클래스를 전용 DTO 파일로 분리
- Admin 숙소 상세 및 예약 상세 API에 이름 정보 포함 (ID → 실제 이름)
- 시드 데이터 요금제 이름 개선

**본인 판단 및 수정 사항:**
- 리팩터링 범위를 직접 결정 — 컨트롤러는 UseCase 호출 + 응답 반환만 담당, 데이터 조립 로직은 모두 UseCase로 이동
- `GetBookingDetailUseCase`, `GetPropertyDetailUseCase` 신규 생성으로 중복 제거 (CustomerBookingController, AdminBookingController에서 동일 로직 사용)
- `PartnerUseCase.list()`, `PropertyUseCase.list/approve/deactivate/reactivate()` 추가로 컨트롤러의 레포지토리 직접 참조 제거
- DTO 파일 구조를 도메인별로 분리: `PartnerDto`, `PropertyDto`, `RoomTypeDto`, `RatePlanDto`, `InventoryDto`, `BookingDto`, `AccommodationDto`, `PropertyAdminDto`
- `BookingResponse`(목록)와 `BookingDetailResponse`(상세)를 명확히 분리해 nullable 필드 제거
- 요금제 이름을 정책 중심으로 변경 (`무료취소`, `환불불가`, `부분환불`, `조식포함`)

---

## 2026-04-22 (5)

### 활용 내용: Extranet UX 개선 / 예약 검증 강화 / 테스트 보강 / 문서 정리

**AI에게 요청한 내용:**
- Extranet 파트너 → 숙소 → 객실 → 요금제 순 드릴다운 네비게이션 구현
- 숙소 승인 상태 표시 및 Admin 승인 안내 문구 추가
- 숙소/객실/요금제 수정 기능 구현 (선택 시 수정 폼 이동)
- 미승인 숙소 예약 차단 기능 및 테스트 추가
- 미승인 파트너 숙소 등록 차단 테스트 추가
- 비활성 숙소 예약 차단 테스트 추가
- traceId가 개발 편의용임을 문서에 명시
- RoomTypeUseCase 캐시 무효화 누락 수정

**본인 판단 및 수정 사항:**
- traceId를 응답에 노출하는 것은 개발/테스트 편의용이며, 실제 운영 환경에서는 보안상 노출하지 않는 것이 맞음 → 아키텍처 문서에 명시
- 프론트에서 수정 버튼을 별도로 두지 않고 각 항목 클릭 시 수정 화면으로 이동하는 UX로 통일 (직접 판단)
- 예약 완료 시 예약 목록으로 자동 이동 + 최신순 정렬 적용 (직접 판단)
- `RoomTypeUseCase.register/update`에 `@CacheEvict` 누락 발견하여 추가 지시
- 시드 데이터 요금제 이름을 정책 중심으로 변경 (직접 판단)
