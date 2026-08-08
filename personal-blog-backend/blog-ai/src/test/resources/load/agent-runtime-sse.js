// k6 smoke/load profile for the unified Agent Runtime SSE endpoint.
// Run: k6 run -e BASE_URL=http://localhost:8084/api -e TOKEN=... agent-runtime-sse.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    sse: { executor: 'constant-vus', vus: Number(__ENV.VUS || 100), duration: __ENV.DURATION || '30s' },
  },
};

export default function () {
  const response = http.post(`${__ENV.BASE_URL || 'http://localhost:8084/api'}/agent/runs/stream`, JSON.stringify({
    taskType: 'RAG_QA', question: __ENV.QUESTION || '请介绍博客中的 Java 文章',
  }), { headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream', Authorization: `Bearer ${__ENV.TOKEN || ''}` }, timeout: '120s' });
  check(response, { 'SSE status is 200': r => r.status === 200, 'SSE content type': r => (r.headers['Content-Type'] || '').includes('text/event-stream') });
  sleep(1);
}
