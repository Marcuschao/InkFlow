import axios from 'axios';
import { useAuthStore } from '../stores/auth';
import { useToastStore } from '../stores/toast';
import {
  resolveApiErrorCode,
  resolveApiErrorMessage,
  resolveApiErrorPayload,
} from './apiError';

const service = axios.create({
  baseURL: import.meta.env.VITE_APP_API_BASE_URL || '/api',
  timeout: 8000,
});

service.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore();
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`;
    }
    if (config.idempotencyKey) {
      config.headers['Idempotency-Key'] = config.idempotencyKey;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

service.interceptors.response.use(
  (response) => {
    const { data } = response;
    if (data != null && typeof data.code === 'number') {
      if (data.code !== 200) {
        const err = new Error(
          resolveApiErrorMessage(
            { message: data.message, code: data.code, responseStatus: response.status },
            '请求失败'
          )
        );
        err.code = data.code;
        err.payload = data.data;
        err.responseStatus = response.status;
        if (!response.config?.skipErrorToast) {
          try {
            useToastStore().push(err.message, 'error');
          } catch (_) {
            /* pinia not ready */
          }
        }
        return Promise.reject(err);
      }
      response.data = data.data;
    }
    return response;
  },
  (error) => {
    const res = error.response;
    const cfg = error.config || {};
    const msg = resolveApiErrorMessage(error, '网络异常');
    const err = new Error(msg);
    err.code = resolveApiErrorCode(error);
    err.payload = resolveApiErrorPayload(error);
    if (res?.status != null) {
      err.responseStatus = res.status;
    }
    // Only the identity endpoint is authoritative for session invalidation.
    // Optional startup calls (AI history, notifications, presence, etc.) may
    // fail while the unified service is warming up; logging out on their 401/403
    // races with a successful login and makes the navbar immediately revert to
    // the anonymous state.
    if ((res?.status === 401 || err.code === 401) && isIdentityRequest(cfg.url)) {
      useAuthStore().clearAuth();
    }
    if (!cfg.skipErrorToast) {
      try {
        useToastStore().push(msg, 'error');
      } catch (_) {
        /* pinia not ready */
      }
    }
    return Promise.reject(err);
  }
);

export default service;

function isIdentityRequest(url = '') {
  const normalized = String(url).split('?')[0].replace(/\/+$/, '');
  return normalized === '/user/me' || normalized.endsWith('/user/me');
}
