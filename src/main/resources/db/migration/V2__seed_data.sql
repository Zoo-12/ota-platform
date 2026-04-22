-- ============================================================
-- V2: 샘플 시드 데이터
-- ============================================================

-- ── 파트너 ────────────────────────────────────────────────────
INSERT INTO partners (id, name, email, phone, business_number, status)
VALUES (1, '그랜드서울파트너스', 'partner1@grand-seoul.com', '02-1234-5678', '123-45-67890', 'ACTIVE'),
       (2, '제주힐링리조트그룹', 'partner2@jeju-healing.com', '064-999-1234', '234-56-78901', 'ACTIVE');

-- ── 숙소 ──────────────────────────────────────────────────────
INSERT INTO properties (id, partner_id, name, description, category, status,
                        address_city, address_district, address_detail,
                        check_in_time, check_out_time)
VALUES (1, 1, '그랜드 서울 호텔', '서울 도심의 럭셔리 호텔', 'HOTEL', 'ACTIVE',
        '서울', '중구', '을지로 100', '15:00', '11:00'),
       (2, 1, '명동 비즈니스 호텔', '출장객을 위한 합리적인 비즈니스 호텔', 'HOTEL', 'ACTIVE',
        '서울', '중구', '명동길 50', '14:00', '12:00'),
       (3, 2, '제주 힐링 리조트', '제주 오션뷰 프리미엄 리조트', 'RESORT', 'ACTIVE',
        '제주', '서귀포시', '중문관광로 200', '16:00', '11:00'),
       (4, 1, '강남 스타일 호텔', '트렌디한 강남 라이프스타일 호텔', 'HOTEL', 'PENDING_APPROVAL',
        '서울', '강남구', '테헤란로 300', '15:00', '11:00');

-- ── 객실 타입 ──────────────────────────────────────────────────
-- 그랜드 서울 호텔
INSERT INTO room_types (id, property_id, name, description, max_occupancy, bed_type, size_sqm)
VALUES (1, 1, '디럭스 더블', '도심 뷰의 쾌적한 더블룸', 2, 'DOUBLE', 32.0),
       (2, 1, '스위트 킹', '넓은 거실과 킹사이즈 침대', 2, 'KING', 58.0);

-- 명동 비즈니스 호텔
INSERT INTO room_types (id, property_id, name, description, max_occupancy, bed_type, size_sqm)
VALUES (3, 2, '스탠다드 싱글', '혼자 출장에 최적화된 싱글룸', 1, 'SINGLE', 18.0),
       (4, 2, '비즈니스 트윈', '2인 출장객을 위한 트윈룸', 2, 'TWIN', 26.0);

-- 제주 힐링 리조트
INSERT INTO room_types (id, property_id, name, description, max_occupancy, bed_type, size_sqm)
VALUES (5, 3, '오션뷰 킹', '제주 바다가 보이는 킹룸', 2, 'KING', 48.0),
       (6, 3, '패밀리 스위트', '가족 여행을 위한 넓은 스위트', 4, 'QUEEN', 75.0);

-- ── 요금제 ─────────────────────────────────────────────────────
-- 그랜드 서울 (room_type 1,2)
INSERT INTO rate_plans (id, room_type_id, name, cancel_policy, breakfast_included, base_price, is_active)
VALUES (1, 1, '무료취소',    'FREE_CANCEL',    0, 150000.00, 1),
       (2, 1, '조식포함',    'FREE_CANCEL',    1, 180000.00, 1),
       (3, 2, '부분환불',    'PARTIAL_REFUND', 0, 350000.00, 1),
       (4, 2, '조식포함',    'FREE_CANCEL',    1, 390000.00, 1);

-- 명동 비즈니스 (room_type 3,4)
INSERT INTO rate_plans (id, room_type_id, name, cancel_policy, breakfast_included, base_price, is_active)
VALUES (5, 3, '무료취소',    'FREE_CANCEL',    0,  89000.00, 1),
       (6, 3, '환불불가',    'NON_REFUNDABLE', 0,  75000.00, 1),
       (7, 4, '부분환불',    'PARTIAL_REFUND', 0, 120000.00, 1),
       (8, 4, '조식포함',    'FREE_CANCEL',    1, 145000.00, 1);

-- 제주 힐링 리조트 (room_type 5,6)
INSERT INTO rate_plans (id, room_type_id, name, cancel_policy, breakfast_included, base_price, is_active)
VALUES ( 9, 5, '무료취소',    'FREE_CANCEL',    0, 280000.00, 1),
       (10, 5, '환불불가',    'NON_REFUNDABLE', 0, 240000.00, 1),
       (11, 6, '부분환불',    'PARTIAL_REFUND', 0, 480000.00, 1),
       (12, 6, '조식포함',    'FREE_CANCEL',    1, 540000.00, 1);

-- ── 재고 (향후 90일, Recursive CTE) ───────────────────────────
-- room_type 1 : 디럭스 더블 — 10실
INSERT INTO room_inventories (room_type_id, date, total_count, available_count)
WITH RECURSIVE d AS (
    SELECT CURDATE() AS dt, 0 AS n
    UNION ALL
    SELECT DATE_ADD(dt, INTERVAL 1 DAY), n + 1 FROM d WHERE n < 89
)
SELECT 1, dt, 10, 10 FROM d;

-- room_type 2 : 스위트 킹 — 4실
INSERT INTO room_inventories (room_type_id, date, total_count, available_count)
WITH RECURSIVE d AS (
    SELECT CURDATE() AS dt, 0 AS n
    UNION ALL
    SELECT DATE_ADD(dt, INTERVAL 1 DAY), n + 1 FROM d WHERE n < 89
)
SELECT 2, dt, 4, 4 FROM d;

-- room_type 3 : 스탠다드 싱글 — 15실
INSERT INTO room_inventories (room_type_id, date, total_count, available_count)
WITH RECURSIVE d AS (
    SELECT CURDATE() AS dt, 0 AS n
    UNION ALL
    SELECT DATE_ADD(dt, INTERVAL 1 DAY), n + 1 FROM d WHERE n < 89
)
SELECT 3, dt, 15, 15 FROM d;

-- room_type 4 : 비즈니스 트윈 — 10실
INSERT INTO room_inventories (room_type_id, date, total_count, available_count)
WITH RECURSIVE d AS (
    SELECT CURDATE() AS dt, 0 AS n
    UNION ALL
    SELECT DATE_ADD(dt, INTERVAL 1 DAY), n + 1 FROM d WHERE n < 89
)
SELECT 4, dt, 10, 10 FROM d;

-- room_type 5 : 오션뷰 킹 — 6실
INSERT INTO room_inventories (room_type_id, date, total_count, available_count)
WITH RECURSIVE d AS (
    SELECT CURDATE() AS dt, 0 AS n
    UNION ALL
    SELECT DATE_ADD(dt, INTERVAL 1 DAY), n + 1 FROM d WHERE n < 89
)
SELECT 5, dt, 6, 6 FROM d;

-- room_type 6 : 패밀리 스위트 — 3실
INSERT INTO room_inventories (room_type_id, date, total_count, available_count)
WITH RECURSIVE d AS (
    SELECT CURDATE() AS dt, 0 AS n
    UNION ALL
    SELECT DATE_ADD(dt, INTERVAL 1 DAY), n + 1 FROM d WHERE n < 89
)
SELECT 6, dt, 3, 3 FROM d;

-- ── 고객 ──────────────────────────────────────────────────────
INSERT INTO customers (id, email, name, phone)
VALUES (1, 'kim@example.com',  '김철수', '010-1111-2222'),
       (2, 'lee@example.com',  '이영희', '010-3333-4444'),
       (3, 'park@example.com', '박민준', '010-5555-6666');
