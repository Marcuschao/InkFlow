import axios from 'axios';
import { useAuthStore } from '../stores/auth';

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
        return Promise.reject(err);
      }
      response.data = data.data;
    }
    return response;
  },
  (error) => {
    const res = error.response;
    if (res?.data && typeof res.data.code === 'number') {
      const d = res.data;
      const err = new Error(d.message || error.message || 'Error');
      err.code = d.code;
      err.payload = d.data;
      if (res.status === 401) {
        const url = error.config?.url || '';
        if (!url.includes('/auth/login')) {
          useAuthStore().clearAuth();
        }
      }
      return Promise.reject(err);
    }
    if (res?.status === 401) {
      const url = error.config?.url || '';
      if (!url.includes('/auth/login')) {
        useAuthStore().clearAuth();
      }
    }
    return Promise.reject(error);
  }
);

export default service;
