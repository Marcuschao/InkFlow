import axios from 'axios';
import { useAuthStore } from '../stores/auth';
import { useToastStore } from '../stores/toast';

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
    return config;
  },
  (error) => Promise.reject(error)
);

service.interceptors.response.use(
  (response) => {
    const { data } = response;
    if (data != null && typeof data.code === 'number') {
      if (data.code !== 200) {
        const err = new Error(data.message || 'Error');
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
    let err = error;
    if (res?.data && typeof res.data.code === 'number') {
      const d = res.data;
      err = new Error(d.message || error.message || 'Error');
      err.code = d.code;
      err.payload = d.data;
      if (res.status === 401) {
        const url = error.config?.url || '';
        if (!url.includes('/auth/login')) {
          useAuthStore().clearAuth();
        }
      }
    } else if (res?.status === 401) {
      const url = error.config?.url || '';
      if (!url.includes('/auth/login')) {
        useAuthStore().clearAuth();
      }
      err = new Error(error.message || '未授权');
    } else {
      err = new Error(error.message || '网络异常');
    }
    if (res?.status != null) {
      err.responseStatus = res.status;
    }
    if (!cfg.skipErrorToast) {
      try {
        useToastStore().push(err.message || '网络异常', 'error');
      } catch (_) {
        /* pinia not ready */
      }
    }
    return Promise.reject(err);
  }
);

export default service;
