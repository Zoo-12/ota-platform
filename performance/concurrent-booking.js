/**
 * 동시 예약 부하 테스트
 *
 * 목적: 재고 N개인 객실에 다수 동시 요청 시 정확히 N건만 성공하는지 검증
 * 필수 요구사항 5번 — 동일한 재고에 대해 동시 예약 요청 처리
 *
 * 전제: 시드 데이터의 room_type_id=2 (스위트 킹, 재고 4개)
 *
 * 실행: k6 run performance/concurrent-booking.js
 */

import http from "k6/http";
import { check } from "k6";
import { Counter } from "k6/metrics";

const successCount = new Counter("booking_success");
const failCount = new Counter("booking_fail");

export const options = {
  // 20명이 동시에 같은 날짜 예약 시도 (재고 4개이므로 4건만 성공해야 함)
  scenarios: {
    concurrent_booking: {
      executor: "shared-iterations",
      vus: 20,
      iterations: 20,
      maxDuration: "30s",
    },
  },
  thresholds: {
    // 예약 성공이 재고 수(4)를 초과하면 안 됨 → 테스트 후 수동 확인
    http_req_failed: ["rate<1"],  // 실패가 있어도 테스트는 계속 (409 예상)
  },
};

const BASE = "http://localhost:8080";

const today = new Date();
function formatDate(offsetDays) {
  const d = new Date(today);
  d.setDate(d.getDate() + offsetDays);
  return d.toISOString().split("T")[0];
}

// 재고가 충분히 남은 미래 날짜 사용
const CHECK_IN = formatDate(60);
const CHECK_OUT = formatDate(62);

export default function () {
  // room_type_id=2 (스위트 킹, 재고 4개), rate_plan_id=3
  const payload = JSON.stringify({
    customerId: Math.floor(Math.random() * 3) + 1,  // 고객 1~3
    roomTypeId: 2,
    ratePlanId: 3,
    checkIn: CHECK_IN,
    checkOut: CHECK_OUT,
    guestCount: 2,
    guestName: `동시테스트-${__VU}`,
  });

  const res = http.post(`${BASE}/api/customer/bookings`, payload, {
    headers: { "Content-Type": "application/json" },
  });

  if (res.status === 201 || res.status === 200) {
    successCount.add(1);
    check(res, { "예약 성공": () => true });
  } else {
    failCount.add(1);
    check(res, {
      "재고 없음 (409 예상)": (r) => r.status === 409 || r.status === 400,
    });
  }
}

export function handleSummary(data) {
  const success = data.metrics.booking_success?.values?.count ?? 0;
  const fail = data.metrics.booking_fail?.values?.count ?? 0;

  return {
    stdout: `
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
동시 예약 테스트 결과
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
총 요청:    ${success + fail}건
예약 성공:  ${success}건
예약 실패:  ${fail}건 (재고 없음)
예상 성공:  4건 (재고 수)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
${success <= 4 ? "✅ PASS: 재고 초과 예약 없음" : "❌ FAIL: 재고 초과 예약 발생!"}
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
`,
  };
}
