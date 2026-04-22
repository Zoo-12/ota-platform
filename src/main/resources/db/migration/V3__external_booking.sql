-- ============================================================
-- V3: 외부 공급사 예약 테이블 생성
-- ============================================================

CREATE TABLE external_booking
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id          BIGINT         NOT NULL,
    accommodation_id     VARCHAR(255)   NOT NULL,
    external_booking_no  VARCHAR(255)   NOT NULL,
    source               VARCHAR(50)    NOT NULL,
    check_in             DATE           NOT NULL,
    check_out            DATE           NOT NULL,
    guest_count          INT            NOT NULL,
    total_price          DECIMAL(12, 2),
    guest_name           VARCHAR(255)   NOT NULL,
    guest_phone          VARCHAR(20),
    status               VARCHAR(20)    NOT NULL DEFAULT 'CONFIRMED',
    created_at           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ext_booking_customer (customer_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
