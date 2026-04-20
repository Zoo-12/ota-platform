# OTA 숙박 플랫폼 백엔드

가상의 OTA(Online Travel Agency) 숙박 플랫폼 백엔드 시스템입니다.
숙소 파트너, 고객, 내부 운영팀, 외부 공급사(Supplier)를 아우르는 API 서버를 구현했습니다.

---

## 기술 스택

| 항목 | 내용 |
|------|------|
| Language | Kotlin 2.1.20 |
| Framework | Spring Boot 3.5.0 |
| Build | Gradle Kotlin DSL |
| Database | MySQL 8.0 |
| Cache | Redis 7 |
| ORM | Spring Data JPA + Hibernate 6 |
| Migration | Flyway |
| API 문서 | SpringDoc (Swagger UI) |
| Test | JUnit 5 + Testcontainers |
| 성능 테스트 | k6 |
| Frontend | Next.js 14 + TypeScript (정적 빌드, Spring Boot 서빙) |

---

## 빠른 시작

### 사전 요구사항

- JDK 21
- Docker Desktop

### 실행

```bash
# 인프라(MySQL, Redis) 기동 → 서버 자동 시작
./start-local.sh
```

서버가 뜨면 아래 URL로 접근 가능합니다.

| URL | 설명 |
|-----|------|
| http://localhost:8080 | 고객 서비스 (숙소 검색 · 예약) |
| http://localhost:8080/extranet | Extranet (파트너 센터 — 숙소 · 객실 · 요금 · 재고 관리) |
| http://localhost:8080/admin | Admin (숙소 승인 · 예약 모니터링) |
| http://localhost:8080/swagger-ui.html | API 문서 (Swagger UI) |

API를 직접 호출하지 않아도 **Next.js 14로 구현된 프론트엔드**를 통해 전체 기능을 UI에서 바로 테스트할 수 있습니다.

> 프론트엔드는 `src/main/resources/static/`의 정적 파일로 Spring Boot와 함께 서빙됩니다.

---

## 주요 기능

### 필수 구현
- **Extranet**: 숙소 파트너가 숙소 / 객실 / 요금제 / 재고를 등록하고 관리
- **고객 검색**: 조건에 맞는 숙소 검색 및 요금 조회 (Redis 캐싱, TTL 5분)
- **예약/취소**: 고객 예약 생성 및 취소, 재고 자동 차감/복원
- **동시성 제어**: 비관적 락(`SELECT FOR UPDATE`)으로 동시 예약 시 재고 초과 방지
- **Supplier 통합**: 어댑터 패턴으로 외부 공급사 상품을 자사 검색에 통합

### 가산점 구현
- Admin 숙소 승인/비활성화/재활성화, 예약 모니터링
- 에러 처리 및 로깅 전략 (traceId MDC, 계층화된 예외 처리, 프로필별 구조화 로깅)
- 이벤트 기반 아키텍처 (`BookingCreatedEvent`, `BookingCancelledEvent`, AFTER_COMMIT)
- Virtual Thread 활성화 (JDK 21)
- k6 성능 테스트 (캐시 효과, 부하 테스트, 동시 예약 정합성)
- Next.js 14 프론트엔드

---

## API 구조

```
/api/extranet/partners                         파트너 등록/조회
/api/extranet/partners/{id}/properties         숙소 등록/관리
/api/extranet/properties/{id}/room-types       객실 타입 관리
/api/extranet/room-types/{id}/rate-plans       요금제 관리
/api/extranet/room-types/{id}/inventories      재고 설정/조회

/api/customer/accommodations/search            숙소 검색 (내부 + Supplier 통합)
/api/customer/accommodations/{id}              숙소 상세 조회
/api/customer/accommodations/{id}/rates        요금 조회
/api/customer/bookings                         예약 생성/조회
/api/customer/bookings/{id}                    예약 취소

/api/admin/properties                          숙소 목록/승인/비활성화
/api/admin/bookings                            예약 모니터링
```

---

## 테스트

```bash
# 단위 + 통합 테스트 (Testcontainers — Docker 필요)
./gradlew test

# 성능 테스트 (서버 실행 중이어야 함)
./performance/run-all.sh
```

### 통합 테스트 결과 (16개 전체 통과)

| 테스트 | 내용 |
|--------|------|
| ExtranetApiIntegrationTest | 숙소 등록, 재고 초기화, 요금 오버라이드, stopSell |
| BookingIntegrationTest | 예약 성공/취소/재고복원/중복취소/재고없음 |
| ConcurrentBookingIntegrationTest | 재고 1개 × 10명, 재고 3개 × 10명 동시 예약 |
| AccommodationSearchIntegrationTest | 통합 검색, 재고 제외, 최저가 정렬, 도시 필터 |

### 성능 테스트 결과

| 테스트 | 핵심 지표 | 결과 |
|--------|---------|------|
| 요금 조회 캐시 | p95 응답시간 | 24ms (목표 100ms) ✅ |
| 숙소 검색 부하 | p95 응답시간 (VU 100명) | 14ms (목표 500ms) ✅ |
| 동시 예약 정합성 | 재고 초과 예약 | 0건 ✅ |

---

## 프로젝트 구조

```
src/main/kotlin/com/ota/platform/
├── property/       숙소, 객실, 요금제 도메인 (Extranet, Admin API)
├── inventory/      날짜별 재고 관리 및 동시성 제어
├── booking/        예약 생성/취소, 도메인 이벤트
├── supplier/       외부 Supplier 어댑터 패턴 (통합 검색)
└── common/         공통 필터, 예외 처리, 응답 형식, 설정

docs/
├── architecture.md  아키텍처 설계 (인증/인가, 로깅 전략 포함)
├── erd.md           ERD
├── domain-research.md  OTA 도메인 리서치
├── performance-test.md  k6 성능 테스트 결과
├── progress-journal.md  과정 기록서
└── ai-usage-log.md  AI 활용 기록
```

---

## 설계 문서

- [아키텍처 설계](docs/architecture.md)
- [ERD](docs/erd.md)
- [도메인 리서치](docs/domain-research.md)
- [성능 테스트 결과](docs/performance-test.md)
- [과정 기록서](docs/progress-journal.md)
- [AI 활용 기록](docs/ai-usage-log.md)
