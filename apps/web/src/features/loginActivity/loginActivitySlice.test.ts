import { configureStore } from '@reduxjs/toolkit';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import loginActivityReducer, { fetchLoginActivity } from './loginActivitySlice';
import { apiClient } from '../../api/client';

vi.mock('../../api/client', () => ({
  apiClient: {
    get: vi.fn()
  }
}));

describe('loginActivitySlice', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('loads safe login activity from the API', async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({
      data: [
        {
          timestamp: '2026-08-11T12:00:00Z',
          successful: true,
          eventType: 'LOGIN_SUCCESS',
          maskedIpAddress: '192.168.***.***',
          deviceSummary: 'Chrome on Windows'
        }
      ]
    });
    const store = configureStore({
      reducer: {
        loginActivity: loginActivityReducer
      }
    });

    await store.dispatch(fetchLoginActivity());

    expect(apiClient.get).toHaveBeenCalledWith('/auth/login-activity');
    expect(store.getState().loginActivity.status).toBe('succeeded');
    expect(store.getState().loginActivity.items[0].maskedIpAddress).toBe('192.168.***.***');
  });
});
