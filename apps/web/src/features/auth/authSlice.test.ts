import { configureStore } from '@reduxjs/toolkit';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import authReducer, { changePassword } from './authSlice';
import type { AuthState } from './authSlice';
import { apiClient } from '../../api/client';

vi.mock('../../api/client', () => ({
  apiClient: {
    post: vi.fn()
  }
}));

describe('authSlice Phase 2 flows', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    sessionStorage.clear();
  });

  it('clears the authenticated session after password change succeeds', async () => {
    sessionStorage.setItem('novabank.accessToken', 'access-token');
    sessionStorage.setItem('novabank.refreshToken', 'refresh-token');
    vi.mocked(apiClient.post).mockResolvedValueOnce({ data: { message: 'Password changed. Please sign in again.' } });
    const preloadedAuthState: AuthState = {
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      username: 'demo.user',
      email: 'demo.user@novabank.test',
      role: 'CUSTOMER',
      customerId: '11111111-1111-1111-1111-111111111111',
      accessTokenExpiresAt: '2026-08-11T12:00:00Z',
      status: 'authenticated',
      error: null
    };
    const store = configureStore({
      reducer: {
        auth: authReducer
      },
      preloadedState: {
        auth: preloadedAuthState
      }
    });

    await store.dispatch(changePassword({
      currentPassword: 'NovaBankDemo!2026',
      newPassword: 'NovaBankDemo!2027',
      confirmPassword: 'NovaBankDemo!2027'
    }));

    expect(apiClient.post).toHaveBeenCalledWith('/auth/change-password', {
      currentPassword: 'NovaBankDemo!2026',
      newPassword: 'NovaBankDemo!2027',
      confirmPassword: 'NovaBankDemo!2027'
    });
    expect(store.getState().auth.status).toBe('idle');
    expect(store.getState().auth.accessToken).toBeNull();
    expect(sessionStorage.getItem('novabank.refreshToken')).toBeNull();
  });
});
