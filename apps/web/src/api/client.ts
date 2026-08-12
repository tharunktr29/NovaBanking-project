import axios from 'axios';
import { store } from '../store';

export const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json'
  }
});

apiClient.interceptors.request.use((config) => {
  const token = store.getState().auth.accessToken;
  const correlationId = typeof globalThis.crypto?.randomUUID === 'function'
    ? globalThis.crypto.randomUUID()
    : `web-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  config.headers['X-Correlation-Id'] = correlationId;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
