import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8080';

export const options = {
  vus: Number(__ENV.VUS || 80),
  duration: __ENV.DURATION || '30s',
};

export default function () {
  const payload = JSON.stringify({
    visitorId: `k6-${__VU}-${__ITER}`,
    path: '/article/1',
  });
  const res = http.post(`${baseUrl}/api/stat/view`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });
  check(res, { 'status 2xx': (r) => r.status >= 200 && r.status < 300 });
  sleep(0.05);
}
