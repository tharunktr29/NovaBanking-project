import { configureStore } from '@reduxjs/toolkit';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import accountsReducer, { fetchAccountBalance, fetchAccounts, updateAccountNickname } from './accountsSlice';
import type { Account } from './accountsSlice';
import { apiClient } from '../../api/client';

type AccountsState = NonNullable<Parameters<typeof accountsReducer>[0]>;

vi.mock('../../api/client', () => ({
  apiClient: {
    get: vi.fn(),
    patch: vi.fn()
  }
}));

describe('accountsSlice', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('loads checking and savings accounts from the API', async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({
      data: [sampleAccount()]
    });
    const store = testStore();

    await store.dispatch(fetchAccounts());

    expect(apiClient.get).toHaveBeenCalledWith('/accounts');
    expect(store.getState().accounts.status).toBe('succeeded');
    expect(store.getState().accounts.accounts[0].maskedAccountNumber).toBe('**** 4821');
    expect(store.getState().accounts.balancesByAccountId['44444444-4444-4444-4444-444444444444'].availableBalance).toBe('4036.42');
  });

  it('loads a single account balance from the API', async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({
      data: sampleAccount().balance
    });
    const store = testStore();

    await store.dispatch(fetchAccountBalance('44444444-4444-4444-4444-444444444444'));

    expect(apiClient.get).toHaveBeenCalledWith('/accounts/44444444-4444-4444-4444-444444444444/balance');
    expect(store.getState().accounts.balancesByAccountId['44444444-4444-4444-4444-444444444444'].currentBalance).toBe('4286.42');
  });

  it('updates an account nickname through the API', async () => {
    const account = {
      id: '44444444-4444-4444-4444-444444444444',
      customerId: '11111111-1111-1111-1111-111111111111',
      accountType: 'CHECKING' as const,
      maskedAccountNumber: '**** 4821',
      nickname: 'Bills Checking',
      status: 'ACTIVE' as const,
      currency: 'USD',
      openedDate: '2024-01-16',
      createdAt: '2024-01-16T00:00:00Z',
      updatedAt: '2026-08-11T12:00:00Z',
      version: 1,
      balance: null
    };
    vi.mocked(apiClient.patch).mockResolvedValueOnce({ data: account });
    const store = testStore([{ ...account, nickname: 'Daily Checking', version: 0 }]);

    await store.dispatch(updateAccountNickname({ accountId: account.id, nickname: account.nickname }));

    expect(apiClient.patch).toHaveBeenCalledWith(`/accounts/${account.id}/nickname`, { nickname: 'Bills Checking' });
    expect(store.getState().accounts.message).toBe('Account nickname updated');
    expect(store.getState().accounts.accounts[0].nickname).toBe('Bills Checking');
  });

  function testStore(accounts: Account[] = []) {
    const preloadedAccountsState: AccountsState = {
      accounts,
      balancesByAccountId: {},
      status: 'idle',
      savingAccountId: null,
      message: null,
      error: null
    };

    return configureStore({
      reducer: {
        accounts: accountsReducer
      },
      preloadedState: {
        accounts: preloadedAccountsState
      }
    });
  }

  function sampleAccount(): Account {
    return {
      id: '44444444-4444-4444-4444-444444444444',
      customerId: '11111111-1111-1111-1111-111111111111',
      accountType: 'CHECKING',
      maskedAccountNumber: '**** 4821',
      nickname: 'Daily Checking',
      status: 'ACTIVE',
      currency: 'USD',
      openedDate: '2024-01-16',
      createdAt: '2024-01-16T00:00:00Z',
      updatedAt: '2024-01-16T00:00:00Z',
      version: 0,
      balance: {
        id: '66666666-6666-6666-6666-666666666666',
        accountId: '44444444-4444-4444-4444-444444444444',
        currentBalance: '4286.42',
        availableBalance: '4036.42',
        pendingDebitAmount: '250.00',
        pendingCreditAmount: '0.00',
        asOf: '2026-08-11T12:00:00Z',
        version: 0
      }
    };
  }
});
