/**
 * 요금 조회 캐시 효과 성능 테스트
 *
 * 목적: 동일 조건으로 반복 요청 시 Redis 캐시가 응답시간을 얼마나 단축하는지 측정
 *
 * 실행: k6 run performance/rate-query-cache.js
 */

import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Counter } from "k6/metrics";

const responseTime = new Trend("response_time_ms", true);
const cacheHitCount = new Counter("cache_hit");
const cacheMissCount = new Counter("cache_miss");

export const options = {
  scenarios: {
    // 1단계: 워밍업 — 캐시 미스 구간 (VU 1명, 요청 5회)
    warmup: {
      executor: "per-vu-iterations",
      vus: 1,
      iterations: 5,
      startTime: "0s",
      tags: { phase: "warmup" },
    },
    // 2단계: 부하 — 캐시 히트 구간 (VU 50명, 30초)
    load: {
      executor: "constant-vus",
      vus: 50,
      duration: "30s",
      startTime: "5s",
      tags: { phase: "load" },
    },
  },
  thresholds: {
    // 캐시 히트 시 95%가 100ms 이내
    "response_time_ms{phase:load}": ["p(95)<100"],
    // 전체 요청 성공률 99% 이상
    http_req_failed: ["rate<0.01"],
  },
};

const BASE = "http://localhost:8080";

// 시드 데이터의 내부 숙소 ID (V2__seed_data.sql 기준)
const ACCOMMODATION_ID = "INTERNAL:1";

const today = new Date();
const checkIn = formatDate(today, 7);   // 7일 후
const checkOut = formatDate(today, 9);  // 9일 후

function formatDate(base, offsetDays) {
  const d = new Date(base);
  d.setDate(d.getDate() + offsetDays);
  return d.toISOString().split("T")[0];
}

export default function () {
  const url = `${BASE}/api/customer/accommodations/${encodeURIComponent(ACCOMMODATION_ID)}/rates?checkIn=${checkIn}&checkOut=${checkOut}`;

  const res = http.get(url, {
    headers: { Accept: "application/json" },
  });

  responseTime.add(res.timings.duration);

  const isHit = res.headers["X-Trace-Id"] !== undefined && res.timings.duration < 50;
  if (isHit) {
    cacheHitCount.add(1);
  } else {
    cacheMissCount.add(1);
  }

  check(res, {
    "status is 200": (r) => r.status === 200,
    "has data": (r) => {
      const body = JSON.parse(r.body);
      return body.success === true && Array.isArray(body.data);
    },
  });

  sleep(0.1);
}
