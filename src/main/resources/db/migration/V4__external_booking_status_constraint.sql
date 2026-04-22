-- external_booking.status 컬럼에 허용 값 제약 추가
-- ExternalBookingStatus enum: CONFIRMED | CANCELLED
ALTER TABLE external_booking
    ADD CONSTRAINT chk_external_booking_status
        CHECK (status IN ('CONFIRMED', 'CANCELLED'));
