import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8080';

export const options = {
  vus: Number(__ENV.VUS || 30),
  duration: __ENV.DURATION || '30s',
};

export default function () {
  const headers =
    __ENV.DISTINCT_IP === '1' ? { 'X-Forwarded-For': `10.0.${__VU}.1` } : {};
  const res = http.get(`${baseUrl}/api/articles?page=1&size=10`, { headers });
  check(res, { 'status 200': (r) => r.status === 200 });
  sleep(__ENV.DISTINCT_IP === '1' ? 1 : 0.1);
}
