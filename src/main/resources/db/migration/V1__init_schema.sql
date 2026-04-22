-- ============================================================
-- V1: 초기 스키마 생성 (단수형 테이블명 적용)
-- ============================================================

-- 파트너
CREATE TABLE partner
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    phone           VARCHAR(20)  NOT NULL,
    business_number VARCHAR(20)  NOT NULL UNIQUE,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 숙소
CREATE TABLE property
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    partner_id       BIGINT       NOT NULL,
    name             VARCHAR(200) NOT NULL,
    description      TEXT,
    category         VARCHAR(30)  NOT NULL,
    status           VARCHAR(30)  NOT NULL DEFAULT 'PENDING_APPROVAL',
    address_city     VARCHAR(100) NOT NULL,
    address_district VARCHAR(100),
    address_detail   VARCHAR(255),
    latitude         DOUBLE,
    longitude        DOUBLE,
    check_in_time    TIME,
    check_out_time   TIME,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_property_partner FOREIGN KEY (partner_id) REFERENCES partner (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_property_status_city ON property (status, address_city);
CREATE INDEX idx_property_partner ON property (partner_id);

-- 객실 타입
CREATE TABLE room_type
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id   BIGINT       NOT NULL,
    name          VARCHAR(100) NOT NULL,
    description   TEXT,
    max_occupancy INT          NOT NULL,
    bed_type      VARCHAR(20)  NOT NULL,
    size_sqm      DOUBLE,
    amenities     JSON,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_room_type_property FOREIGN KEY (property_id) REFERENCES property (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_room_type_property ON room_type (property_id);

-- 요금 플랜
CREATE TABLE rate_plan
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_type_id       BIGINT         NOT NULL,
    name               VARCHAR(100)   NOT NULL,
    cancel_policy      VARCHAR(30)    NOT NULL,
    breakfast_included TINYINT(1)     NOT NULL DEFAULT 0,
    base_price         DECIMAL(12, 2) NOT NULL,
    is_active          TINYINT(1)     NOT NULL DEFAULT 1,
    created_at         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_rate_plan_room_type FOREIGN KEY (room_type_id) REFERENCES room_type (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_rate_plan_room_type ON rate_plan (room_type_id);

-- 날짜별 요금 (base_price 오버라이드)
CREATE TABLE daily_rate
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    rate_plan_id BIGINT         NOT NULL,
    date         DATE           NOT NULL,
    price        DECIMAL(12, 2) NOT NULL,
    created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_daily_rate_rate_plan FOREIGN KEY (rate_plan_id) REFERENCES rate_plan (id),
    CONSTRAINT uq_daily_rate UNIQUE (rate_plan_id, date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 날짜별 재고 (핵심 테이블 — 예약 시 SELECT FOR UPDATE 대상)
CREATE TABLE room_inventory
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_type_id    BIGINT   NOT NULL,
    date            DATE     NOT NULL,
    total_count     INT      NOT NULL,
    available_count INT      NOT NULL,
    stop_sell       TINYINT(1) NOT NULL DEFAULT 0,
    min_stay        INT               DEFAULT 1,
    max_stay        INT,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_room_inventory_room_type FOREIGN KEY (room_type_id) REFERENCES room_type (id),
    CONSTRAINT uq_room_inventory UNIQUE (room_type_id, date),
    CONSTRAINT chk_available_count CHECK (available_count >= 0),
    CONSTRAINT chk_total_count CHECK (total_count >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_room_inventory_room_date ON room_inventory (room_type_id, date, available_count);

-- 고객
CREATE TABLE customer
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    name       VARCHAR(100) NOT NULL,
    phone      VARCHAR(20),
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 예약
CREATE TABLE booking
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id     BIGINT         NOT NULL,
    property_id     BIGINT         NOT NULL,
    room_type_id    BIGINT         NOT NULL,
    rate_plan_id    BIGINT         NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'CONFIRMED',
    check_in        DATE           NOT NULL,
    check_out       DATE           NOT NULL,
    guest_count     INT            NOT NULL,
    total_price     DECIMAL(12, 2) NOT NULL,
    guest_name      VARCHAR(100)   NOT NULL,
    guest_phone     VARCHAR(20),
    special_request TEXT,
    cancelled_at    DATETIME,
    cancel_reason   TEXT,
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_customer  FOREIGN KEY (customer_id)  REFERENCES customer (id),
    CONSTRAINT fk_booking_property  FOREIGN KEY (property_id)  REFERENCES property (id),
    CONSTRAINT fk_booking_room_type FOREIGN KEY (room_type_id) REFERENCES room_type (id),
    CONSTRAINT fk_booking_rate_plan FOREIGN KEY (rate_plan_id) REFERENCES rate_plan (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_booking_customer ON booking (customer_id, status);
CREATE INDEX idx_booking_property ON booking (property_id, check_in, check_out);

-- 예약 날짜별 상세 (재고 차감 추적)
CREATE TABLE booking_room
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id        BIGINT         NOT NULL,
    room_inventory_id BIGINT         NOT NULL,
    date              DATE           NOT NULL,
    price_snapshot    DECIMAL(12, 2) NOT NULL,
    created_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_room_booking   FOREIGN KEY (booking_id)        REFERENCES booking (id),
    CONSTRAINT fk_booking_room_inventory FOREIGN KEY (room_inventory_id) REFERENCES room_inventory (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_booking_room_booking ON booking_room (booking_id);

-- 외부 공급사
CREATE TABLE external_supplier
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    adapter_type VARCHAR(50)  NOT NULL UNIQUE,
    api_endpoint VARCHAR(500),
    is_active    TINYINT(1)   NOT NULL DEFAULT 1,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Mock Supplier 초기 데이터
INSERT INTO external_supplier (name, adapter_type, api_endpoint, is_active)
VALUES ('Mock Supplier A', 'MOCK_SUPPLIER_A', 'http://mock-supplier-a.internal/api', 1);
