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

<!-- 이후 AI 활용 내역은 날짜별로 추가 -->
