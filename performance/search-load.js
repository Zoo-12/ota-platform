/**
 * 숙소 검색 부하 테스트
 *
 * 목적: 대규모 동시 요금 조회 요청 처리 능력 측정 (필수 요구사항 3번)
 * - 캐시 없는 첫 요청 vs 캐시 히트 반복 요청 비교
 *
 * 실행: k6 run performance/search-load.js
 */

import http from "k6/http";
import { check, sleep } from "k6";
import { Trend } from "k6/metrics";

const searchTime = new Trend("search_response_ms", true);

export const options = {
  stages: [
    { duration: "10s", target: 10 },   // 램프업
    { duration: "30s", target: 100 },  // 최대 부하
    { duration: "10s", target: 0 },    // 램프다운
  ],
  thresholds: {
    "search_response_ms": ["p(95)<500", "p(99)<1000"],
    http_req_failed: ["rate<0.01"],
  },
};

const BASE = "http://localhost:8080";
const CITIES = ["서울", "제주"];

const today = new Date();
function formatDate(offsetDays) {
  const d = new Date(today);
  d.setDate(d.getDate() + offsetDays);
  return d.toISOString().split("T")[0];
}

export default function () {
  const city = CITIES[Math.floor(Math.random() * CITIES.length)];
  const offset = Math.floor(Math.random() * 30) + 1;
  const checkIn = formatDate(offset);
  const checkOut = formatDate(offset + 2);

  const url = `${BASE}/api/customer/accommodations/search?city=${encodeURIComponent(city)}&checkIn=${checkIn}&checkOut=${checkOut}&guestCount=2`;

  const res = http.get(url, {
    headers: { Accept: "application/json" },
    tags: { endpoint: "search" },
  });

  searchTime.add(res.timings.duration);

  check(res, {
    "status 200": (r) => r.status === 200,
    "has results": (r) => {
      const body = JSON.parse(r.body);
      return body.success === true;
    },
  });

  sleep(0.5);
}
