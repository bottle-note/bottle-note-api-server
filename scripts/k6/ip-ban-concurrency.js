import http from 'k6/http';
import { check, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || '').replace(/\/$/, '');
const MODE = __ENV.MODE || 'fixed';
const AUTHORIZATION = __ENV.AUTHORIZATION;
const READ_PATHS = (__ENV.READ_PATHS || '')
  .split(',')
  .map((path) => path.trim())
  .filter(Boolean);

const DEFAULT_READ_PATHS = [
  '/api/v1/app-info',
  '/api/v1/regions',
  '/api/v1/alcohols/categories',
  '/api/v1/alcohols/categories?type=WHISKY',
  '/api/v1/alcohols/categories?type=BRANDY',
  '/api/v1/banners?limit=5',
  '/api/v1/banners?limit=10',
  '/api/v1/popular/week?top=5',
  '/api/v1/popular/week?top=10',
];

const statusNetwork = new Counter('status_network');
const status2xx = new Counter('status_2xx');
const status3xx = new Counter('status_3xx');
const status4xx = new Counter('status_4xx');
const status5xx = new Counter('status_5xx');
const statusOther = new Counter('status_other');
const networkFailures = new Rate('network_failures');
const serverFailures = new Rate('server_failures');
const clientFailures = new Rate('client_failures');
const responseDuration = new Trend('scenario_response_duration', true);
const fixedHttp200 = new Rate('fixed_http_200');

const thresholds = {
  http_req_duration: ['p(95)<1000', 'p(99)<2000'],
  network_failures: ['rate==0'],
  server_failures: ['rate==0'],
  scenario_response_duration: ['p(95)<1000', 'p(99)<2000'],
};

if (MODE === 'fixed') {
  thresholds.fixed_http_200 = ['rate==1'];
}

export const options = {
  scenarios: {
    concurrent_500: {
      executor: 'per-vu-iterations',
      vus: 500,
      iterations: 1,
      maxDuration: '2m',
    },
  },
  thresholds,
};

function requestParams() {
  const headers = {};
  if (AUTHORIZATION) {
    headers.Authorization = AUTHORIZATION;
  }
  return { headers, tags: { mode: MODE } };
}

function recordStatus(response) {
  responseDuration.add(response.timings.duration);
  if (response.status === 0) {
    statusNetwork.add(1);
    networkFailures.add(1);
    serverFailures.add(0);
    clientFailures.add(0);
    return;
  }

  networkFailures.add(0);
  if (response.status >= 500 && response.status < 600) {
    status5xx.add(1);
    serverFailures.add(1);
    clientFailures.add(0);
  } else if (response.status >= 400 && response.status < 500) {
    status4xx.add(1);
    serverFailures.add(0);
    clientFailures.add(1);
  } else if (response.status >= 300 && response.status < 400) {
    status3xx.add(1);
    serverFailures.add(0);
    clientFailures.add(0);
  } else if (response.status >= 200 && response.status < 300) {
    status2xx.add(1);
    serverFailures.add(0);
    clientFailures.add(0);
  } else {
    statusOther.add(1);
    serverFailures.add(0);
    clientFailures.add(0);
  }
}

function selectReadPath() {
  const paths = READ_PATHS.length > 0 ? READ_PATHS : DEFAULT_READ_PATHS;
  return paths[Math.floor(Math.random() * paths.length)];
}

export function setup() {
  if (!BASE_URL) {
    throw new Error('BASE_URL 환경 변수가 필요합니다.');
  }
  if (MODE !== 'fixed' && MODE !== 'random') {
    throw new Error('MODE는 fixed 또는 random 이어야 합니다.');
  }
}

export default function () {
  const path = MODE === 'fixed' ? '/actuator/health' : selectReadPath();
  const response = group(MODE, () => http.get(`${BASE_URL}${path}`, requestParams()));

  recordStatus(response);
  if (MODE === 'fixed') {
    const isOk = response.status === 200;
    fixedHttp200.add(isOk);
    check(response, { 'fixed status is 200': () => isOk });
  } else {
    check(response, { 'response is not a network failure': () => response.status !== 0 });
  }
}
