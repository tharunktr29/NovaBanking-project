import axios from 'axios';

let accessTokenProvider: () => string | null = () => null;

export function setAccessTokenProvider(provider: () => string | null) {
  accessTokenProvider = provider;
}

export const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json'
  }
});

apiClient.interceptors.request.use((config) => {
  const token = accessTokenProvider();
  const correlationId = typeof globalThis.crypto?.randomUUID === 'function'
    ? globalThis.crypto.randomUUID()
    : `web-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  config.headers['X-Correlation-Id'] = correlationId;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
