import http from 'k6/http';
import { check, fail, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const EVENT_ID = __ENV.EVENT_ID || '1';
const USER_COUNT = Number(__ENV.USER_COUNT || 2000);
const MAX_VUS = Number(__ENV.MAX_VUS || 2000);
const RAMP_UP = __ENV.RAMP_UP || '5s';
const HOLD = __ENV.HOLD || '30s';
const RAMP_DOWN = __ENV.RAMP_DOWN || '5s';
const USER_PREFIX = __ENV.USER_PREFIX || 'loadtest-';
const USER_PASSWORD = __ENV.USER_PASSWORD || 'loadtest1234!';
const LOGIN_BATCH_SIZE = Number(__ENV.LOGIN_BATCH_SIZE || 100);
const SETUP_TIMEOUT = __ENV.SETUP_TIMEOUT || '300s';
const POLL_INTERVAL_SEC = Number(__ENV.POLL_INTERVAL_SEC || 0.45);
const EXPECTED_TOTAL_REQUESTS = Number(__ENV.EXPECTED_TOTAL_REQUESTS || 171250);
const REMAINING_PATH = __ENV.REMAINING_PATH || '/tickets/events';
const RESERVE_PATH = __ENV.RESERVE_PATH || `/tickets/${EVENT_ID}/reserve`;
const SUMMARY_PATH = __ENV.SUMMARY_PATH || '';
const ALLOW_TOKEN_REUSE = parseBoolean(__ENV.ALLOW_TOKEN_REUSE, false);

const DEFAULT_ALLOWED_RESERVE_STATUSES = [400, 409];
const ALLOWED_RESERVE_STATUSES = parseStatusList(__ENV.ALLOWED_RESERVE_STATUSES, DEFAULT_ALLOWED_RESERVE_STATUSES);
const ALLOWED_RESERVE_STATUS_SET = new Set([200, ...ALLOWED_RESERVE_STATUSES]);
const JSON_HEADERS = { 'Content-Type': 'application/json' };

const loginRequests = new Counter('login_requests');
const reserveRequests = new Counter('reserve_requests');
const remainingRequests = new Counter('remaining_requests');
const reserveAttemptedUsers = new Counter('reserve_attempted_users');
const loginUnexpectedRate = new Rate('login_unexpected_rate');
const reserveUnexpectedRate = new Rate('reserve_unexpected_rate');
const remainingUnexpectedRate = new Rate('remaining_unexpected_rate');

let reserveAttempted = false;

export const options = {
  scenarios: {
    reserve_and_remaining: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: RAMP_UP, target: MAX_VUS },
        { duration: HOLD, target: MAX_VUS },
        { duration: RAMP_DOWN, target: 0 },
      ],
      gracefulRampDown: '0s',
    },
  },
  setupTimeout: SETUP_TIMEOUT,
  thresholds: {
    login_unexpected_rate: ['rate==0'],
    remaining_unexpected_rate: ['rate<0.01'],
    reserve_unexpected_rate: ['rate<0.05'],
    http_req_duration: ['p(95)<2000'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max', 'count'],
};

export function setup() {
  if (!ALLOW_TOKEN_REUSE && USER_COUNT < MAX_VUS) {
    fail(`USER_COUNT(${USER_COUNT}) must be >= MAX_VUS(${MAX_VUS}) when ALLOW_TOKEN_REUSE=false`);
  }

  console.log(
    `[k6] scenario config: userCount=${USER_COUNT}, maxVUs=${MAX_VUS}, stages=${RAMP_UP}/${HOLD}/${RAMP_DOWN}, pollIntervalSec=${POLL_INTERVAL_SEC}, reservePath=${RESERVE_PATH}, remainingPath=${REMAINING_PATH}`
  );

  ensureTicketEventsEndpointIsHealthy();
  const tokens = loginAllUsers();

  return { tokens };
}

export default function (data) {
  const token = data.tokens[(__VU - 1) % data.tokens.length];
  const authHeaders = { Authorization: `Bearer ${token}` };

  if (!reserveAttempted) {
    reserveAttemptedUsers.add(1);
    const reserveResponse = http.post(`${BASE_URL}${RESERVE_PATH}`, null, {
      headers: authHeaders,
      tags: { endpoint: 'reserve' },
    });
    reserveRequests.add(1);

    const reserveExpected = ALLOWED_RESERVE_STATUS_SET.has(reserveResponse.status);
    reserveUnexpectedRate.add(reserveExpected ? 0 : 1);
    check(reserveResponse, {
      'reserve status is expected': () => reserveExpected,
    });

    reserveAttempted = true;
  }

  const remainingResponse = http.get(`${BASE_URL}${REMAINING_PATH}`, {
    headers: authHeaders,
    tags: { endpoint: 'remaining' },
  });
  remainingRequests.add(1);

  const remainingExpected = remainingResponse.status === 200;
  remainingUnexpectedRate.add(remainingExpected ? 0 : 1);
  check(remainingResponse, {
    'remaining status is 200': (res) => res.status === 200,
  });

  sleep(POLL_INTERVAL_SEC);
}

export function handleSummary(data) {
  const totalHttpRequests = metricCount(data, 'http_reqs');
  const reserveCount = metricCount(data, 'reserve_requests');
  const remainingCount = metricCount(data, 'remaining_requests');
  const loginCount = metricCount(data, 'login_requests');
  const workloadRequests = reserveCount + remainingCount;
  const expectedDiff = workloadRequests - EXPECTED_TOTAL_REQUESTS;
  const expectedDiffRate = EXPECTED_TOTAL_REQUESTS > 0
    ? ((expectedDiff / EXPECTED_TOTAL_REQUESTS) * 100).toFixed(2)
    : '0.00';

  const summary = {
    timestamp: new Date().toISOString(),
    scenario: {
      maxVUs: MAX_VUS,
      stages: {
        rampUp: RAMP_UP,
        hold: HOLD,
        rampDown: RAMP_DOWN,
      },
      userCount: USER_COUNT,
      pollIntervalSec: POLL_INTERVAL_SEC,
      reservePath: RESERVE_PATH,
      remainingPath: REMAINING_PATH,
      expectedTotalRequests: EXPECTED_TOTAL_REQUESTS,
    },
    totals: {
      totalHttpRequests,
      workloadRequests,
      loginRequests: loginCount,
      reserveRequests: reserveCount,
      remainingRequests: remainingCount,
      expectedDiff,
      expectedDiffRatePercent: Number(expectedDiffRate),
    },
    checks: data.root_group && data.root_group.checks ? data.root_group.checks : [],
    metrics: data.metrics,
  };

  const lines = [
    '[k6] reserve+remaining summary',
    `- total_http_requests=${totalHttpRequests}`,
    `- workload_requests=${workloadRequests}`,
    `- expected_total_requests=${EXPECTED_TOTAL_REQUESTS}`,
    `- diff=${expectedDiff} (${expectedDiffRate}%)`,
    `- login_requests=${loginCount}`,
    `- reserve_requests=${reserveCount}`,
    `- remaining_requests=${remainingCount}`,
  ];

  const output = {
    stdout: `${lines.join('\n')}\n`,
  };

  if (SUMMARY_PATH) {
    output[SUMMARY_PATH] = JSON.stringify(summary, null, 2);
  }

  return output;
}

function ensureTicketEventsEndpointIsHealthy() {
  const response = http.get(`${BASE_URL}/tickets/events`, {
    tags: { endpoint: 'events-health' },
  });

  if (response.status !== 200) {
    loginUnexpectedRate.add(1);
    fail(`/tickets/events health check failed. status=${response.status}`);
  }

  loginUnexpectedRate.add(0);
}

function loginAllUsers() {
  const tokens = new Array(USER_COUNT);

  for (let start = 0; start < USER_COUNT; start += LOGIN_BATCH_SIZE) {
    const end = Math.min(start + LOGIN_BATCH_SIZE, USER_COUNT);
    const batchRequests = [];

    for (let i = start; i < end; i += 1) {
      const studentId = buildStudentId(i + 1);
      const body = JSON.stringify({
        studentId,
        password: USER_PASSWORD,
      });
      batchRequests.push([
        'POST',
        `${BASE_URL}/user/login`,
        body,
        { headers: JSON_HEADERS, tags: { endpoint: 'login' } },
      ]);
    }

    const responses = http.batch(batchRequests);
    for (let offset = 0; offset < responses.length; offset += 1) {
      const response = responses[offset];
      const userIndex = start + offset;
      const studentId = buildStudentId(userIndex + 1);
      loginRequests.add(1);

      if (response.status !== 200) {
        loginUnexpectedRate.add(1);
        fail(`login failed for studentId=${studentId}, status=${response.status}, body=${response.body}`);
      }

      const payload = safeJsonParse(response.body);
      if (!payload || !payload.accessToken) {
        loginUnexpectedRate.add(1);
        fail(`accessToken missing in login response for studentId=${studentId}`);
      }

      loginUnexpectedRate.add(0);
      tokens[userIndex] = payload.accessToken;
    }
  }

  return tokens;
}

function buildStudentId(sequence) {
  return `${USER_PREFIX}${String(sequence).padStart(6, '0')}`;
}

function safeJsonParse(raw) {
  try {
    return JSON.parse(raw);
  } catch (e) {
    return null;
  }
}

function parseStatusList(raw, fallback) {
  if (!raw) {
    return fallback;
  }

  const parsed = raw
    .split(',')
    .map((item) => Number(item.trim()))
    .filter((value) => Number.isFinite(value));

  return parsed.length > 0 ? parsed : fallback;
}

function parseBoolean(raw, fallback) {
  if (raw === undefined || raw === null) {
    return fallback;
  }

  const normalized = String(raw).trim().toLowerCase();
  if (normalized === 'true' || normalized === '1' || normalized === 'yes') {
    return true;
  }
  if (normalized === 'false' || normalized === '0' || normalized === 'no') {
    return false;
  }
  return fallback;
}

function metricCount(data, metricName) {
  const metric = data.metrics[metricName];
  if (!metric || !metric.values || metric.values.count === undefined) {
    return 0;
  }
  return metric.values.count;
}
