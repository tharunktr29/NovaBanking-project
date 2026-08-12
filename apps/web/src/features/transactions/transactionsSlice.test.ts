import { configureStore } from '@reduxjs/toolkit';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import transactionsReducer, {
  clearFilters,
  emptyFilters,
  exportTransactionsCsv,
  fetchRecentTransactions,
  fetchTransactionDetail,
  fetchTransactions,
  setFilters,
  setSorting
} from './transactionsSlice';
import { apiClient } from '../../api/client';

vi.mock('../../api/client', () => ({
  apiClient: {
    get: vi.fn()
  }
}));

type TransactionsState = NonNullable<Parameters<typeof transactionsReducer>[0]>;

describe('transactionsSlice', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => 'blob:url'),
      revokeObjectURL: vi.fn()
    });
  });

  it('loads transaction list from the API instead of mock data', async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({ data: page([sampleTransaction()]) });
    const store = testStore();

    await store.dispatch(fetchTransactions());

    expect(apiClient.get).toHaveBeenCalledWith('/transactions', expect.objectContaining({
      params: expect.objectContaining({ page: 0, size: 20, sort: 'authorizedAt,desc' })
    }));
    expect(store.getState().transactions.transactions).toHaveLength(1);
    expect(store.getState().transactions.transactions[0].merchant?.name).toBe('Maple Market');
  });

  it('loads recent transactions for the dashboard', async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({ data: page([sampleTransaction()], { size: 5 }) });
    const store = testStore();

    await store.dispatch(fetchRecentTransactions());

    expect(apiClient.get).toHaveBeenCalledWith('/transactions', expect.objectContaining({
      params: { page: 0, size: 5, sort: 'authorizedAt,desc' }
    }));
    expect(store.getState().transactions.recentTransactions[0].description).toBe('Groceries');
  });

  it('tracks filters, search, sorting, and pagination through API params', async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({ data: page([], { page: 2, totalPages: 3 }) });
    const store = testStore();

    store.dispatch(setFilters({ status: 'POSTED', category: 'GROCERIES', search: 'maple' }));
    store.dispatch(setSorting('amount,asc'));
    await store.dispatch(fetchTransactions({ page: 2 }));

    expect(apiClient.get).toHaveBeenCalledWith('/transactions', expect.objectContaining({
      params: expect.objectContaining({
        status: 'POSTED',
        category: 'GROCERIES',
        search: 'maple',
        page: 2,
        sort: 'amount,asc'
      })
    }));
    expect(store.getState().transactions.pagination.page).toBe(2);
  });

  it('clears filters back to empty state', () => {
    const store = testStore();

    store.dispatch(setFilters({ merchant: 'Maple', minAmount: '10' }));
    store.dispatch(clearFilters());

    expect(store.getState().transactions.activeFilters).toEqual(emptyFilters);
  });

  it('loads transaction details', async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({ data: sampleTransaction() });
    const store = testStore();

    await store.dispatch(fetchTransactionDetail('20000000-0000-0000-0000-000000000001'));

    expect(apiClient.get).toHaveBeenCalledWith('/transactions/20000000-0000-0000-0000-000000000001', expect.any(Object));
    expect(store.getState().transactions.selected?.transactionId).toBe('20000000-0000-0000-0000-000000000001');
  });

  it('represents empty and failure states', async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({ data: page([]) }).mockRejectedValueOnce({
      response: { data: { message: 'Backend unavailable' } }
    });
    const store = testStore();

    await store.dispatch(fetchTransactions());
    expect(store.getState().transactions.transactions).toEqual([]);

    await store.dispatch(fetchTransactions());
    expect(store.getState().transactions.error).toBe('Backend unavailable');
  });

  it('exports CSV through the API', async () => {
    const click = vi.fn();
    vi.spyOn(document, 'createElement').mockReturnValue({ click } as unknown as HTMLAnchorElement);
    vi.mocked(apiClient.get).mockResolvedValueOnce({
      data: new Blob(['date,amount']),
      headers: { 'content-disposition': 'attachment; filename="transactions.csv"' }
    });
    const store = testStore();

    await store.dispatch(exportTransactionsCsv());

    expect(apiClient.get).toHaveBeenCalledWith('/transactions/export', expect.objectContaining({ responseType: 'blob' }));
    expect(store.getState().transactions.exportState).toBe('succeeded');
    expect(click).toHaveBeenCalled();
  });
});

function testStore(preloaded?: Partial<TransactionsState>) {
  return configureStore({
    reducer: {
      transactions: transactionsReducer
    },
    preloadedState: {
      transactions: {
        ...transactionsReducer(undefined, { type: 'init' }),
        ...preloaded
      }
    }
  });
}

function page(content: ReturnType<typeof sampleTransaction>[], overrides: Partial<{ page: number; size: number; totalElements: number; totalPages: number }> = {}) {
  return {
    content,
    page: overrides.page ?? 0,
    size: overrides.size ?? 20,
    totalElements: overrides.totalElements ?? content.length,
    totalPages: overrides.totalPages ?? (content.length ? 1 : 0),
    first: (overrides.page ?? 0) === 0,
    last: true,
    sort: 'authorizedAt,desc'
  };
}

function sampleTransaction() {
  return {
    transactionId: '20000000-0000-0000-0000-000000000001',
    accountId: '44444444-4444-4444-4444-444444444444',
    status: 'POSTED' as const,
    direction: 'DEBIT' as const,
    type: 'PURCHASE' as const,
    category: 'GROCERIES' as const,
    description: 'Groceries',
    amount: '86.37',
    currency: 'USD',
    merchant: {
      merchantId: '10000000-0000-0000-0000-000000000001',
      name: 'Maple Market',
      merchantCategoryCode: '5411',
      city: 'Indianapolis',
      state: 'IN',
      country: 'US'
    },
    authorizedAt: '2026-08-03T18:42:00Z',
    postedAt: '2026-08-04T10:15:00Z'
  };
}
