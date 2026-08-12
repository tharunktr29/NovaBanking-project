import { configureStore } from '@reduxjs/toolkit';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import customerReducer, {
  fetchCustomerPreferences,
  fetchCustomerProfile,
  updateCustomerPreferences,
  updateCustomerProfile
} from './customerSlice';
import { apiClient } from '../../api/client';

vi.mock('../../api/client', () => ({
  apiClient: {
    get: vi.fn(),
    patch: vi.fn()
  }
}));

describe('customerSlice', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('loads the authenticated customer profile from the API', async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({ data: profile() });
    const store = testStore();

    await store.dispatch(fetchCustomerProfile());

    expect(apiClient.get).toHaveBeenCalledWith('/customers/me');
    expect(store.getState().customer.profile?.firstName).toBe('Demo');
    expect(store.getState().customer.profile?.kycStatus).toBe('VERIFIED');
  });

  it('updates customer contact information through the API', async () => {
    vi.mocked(apiClient.patch).mockResolvedValueOnce({ data: { ...profile(), firstName: 'Dana' } });
    const store = testStore();

    await store.dispatch(updateCustomerProfile({
      firstName: 'Dana',
      lastName: 'Customer',
      email: 'dana.customer@novabank.test',
      phone: '+1 555 010 3000',
      addressLine1: '200 Fictional Road',
      addressLine2: '',
      city: 'Indianapolis',
      state: 'IN',
      postalCode: '46204',
      country: 'USA'
    }));

    expect(apiClient.patch).toHaveBeenCalledWith('/customers/me', expect.objectContaining({ firstName: 'Dana' }));
    expect(store.getState().customer.message).toBe('Profile updated');
    expect(store.getState().customer.profile?.firstName).toBe('Dana');
  });

  it('loads and updates customer preferences', async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({ data: preferences() });
    vi.mocked(apiClient.patch).mockResolvedValueOnce({ data: { ...preferences(), paperlessStatements: false } });
    const store = testStore();

    await store.dispatch(fetchCustomerPreferences());
    await store.dispatch(updateCustomerPreferences({
      emailAlerts: true,
      smsAlerts: true,
      pushAlerts: true,
      securityAlerts: true,
      paymentAlerts: true,
      lowBalanceAlerts: true,
      paperlessStatements: false
    }));

    expect(apiClient.get).toHaveBeenCalledWith('/customers/me/preferences');
    expect(apiClient.patch).toHaveBeenCalledWith('/customers/me/preferences', expect.objectContaining({ paperlessStatements: false }));
    expect(store.getState().customer.preferences?.paperlessStatements).toBe(false);
  });

  function testStore() {
    return configureStore({
      reducer: {
        customer: customerReducer
      }
    });
  }

  function profile() {
    return {
      id: '22222222-2222-2222-2222-222222222222',
      authUserId: '11111111-1111-1111-1111-111111111111',
      firstName: 'Demo',
      lastName: 'Customer',
      email: 'demo.user@novabank.test',
      phone: '+1 555 010 2000',
      addressLine1: '100 Fictional Avenue',
      addressLine2: 'Suite 21',
      city: 'Indianapolis',
      state: 'IN',
      postalCode: '46204',
      country: 'USA',
      kycStatus: 'VERIFIED' as const,
      createdAt: '2026-08-11T12:00:00Z',
      updatedAt: '2026-08-11T12:00:00Z',
      version: 0
    };
  }

  function preferences() {
    return {
      id: '33333333-3333-3333-3333-333333333333',
      customerId: '22222222-2222-2222-2222-222222222222',
      emailAlerts: true,
      smsAlerts: false,
      pushAlerts: true,
      securityAlerts: true,
      paymentAlerts: true,
      lowBalanceAlerts: true,
      paperlessStatements: true,
      createdAt: '2026-08-11T12:00:00Z',
      updatedAt: '2026-08-11T12:00:00Z',
      version: 0
    };
  }
});
