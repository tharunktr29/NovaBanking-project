import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { apiClient } from '../../api/client';

export type TransactionStatus = 'PENDING' | 'POSTED' | 'REVERSED';
export type TransactionDirection = 'DEBIT' | 'CREDIT';
export type TransactionType = 'PURCHASE' | 'DEPOSIT' | 'WITHDRAWAL' | 'FEE' | 'REFUND' | 'TRANSFER' | 'PAYMENT' | 'INTEREST';
export type TransactionCategory = 'GROCERIES' | 'DINING' | 'GAS' | 'UTILITIES' | 'ENTERTAINMENT' | 'SHOPPING' | 'TRAVEL' | 'HEALTHCARE' | 'INCOME' | 'TRANSFER' | 'FEES' | 'OTHER';

export interface Merchant {
  merchantId: string;
  name: string;
  merchantCategoryCode: string | null;
  city: string | null;
  state: string | null;
  country: string | null;
}

export interface BankTransaction {
  transactionId: string;
  accountId: string;
  status: TransactionStatus;
  direction: TransactionDirection;
  type: TransactionType;
  category: TransactionCategory;
  description: string;
  amount: string;
  currency: string;
  merchant: Merchant | null;
  authorizedAt: string;
  postedAt: string | null;
}

export interface TransactionFilters {
  accountId: string;
  status: string;
  direction: string;
  type: string;
  category: string;
  merchant: string;
  search: string;
  dateFrom: string;
  dateTo: string;
  minAmount: string;
  maxAmount: string;
}

export interface TransactionPage {
  content: BankTransaction[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  sort: string;
}

interface TransactionsState {
  transactions: BankTransaction[];
  selected: BankTransaction | null;
  recentTransactions: BankTransaction[];
  loading: boolean;
  recentLoading: boolean;
  detailLoading: boolean;
  error: string | null;
  detailError: string | null;
  activeFilters: TransactionFilters;
  pagination: Omit<TransactionPage, 'content' | 'sort'>;
  sorting: string;
  exportState: 'idle' | 'loading' | 'succeeded' | 'failed';
  exportError: string | null;
  currentRequestId: string | null;
}

export const emptyFilters: TransactionFilters = {
  accountId: '',
  status: '',
  direction: '',
  type: '',
  category: '',
  merchant: '',
  search: '',
  dateFrom: '',
  dateTo: '',
  minAmount: '',
  maxAmount: ''
};

const initialState: TransactionsState = {
  transactions: [],
  selected: null,
  recentTransactions: [],
  loading: false,
  recentLoading: false,
  detailLoading: false,
  error: null,
  detailError: null,
  activeFilters: emptyFilters,
  pagination: {
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true
  },
  sorting: 'authorizedAt,desc',
  exportState: 'idle',
  exportError: null,
  currentRequestId: null
};

function errorMessage(error: any, fallback: string) {
  return error.response?.data?.message ?? fallback;
}

function params(filters: TransactionFilters, page: number, size: number, sort: string) {
  const values: Record<string, string | number> = { page, size, sort };
  Object.entries(filters).forEach(([key, value]) => {
    if (value) {
      values[key] = value;
    }
  });
  return values;
}

export const fetchTransactions = createAsyncThunk<
  TransactionPage,
  { page?: number; size?: number; filters?: TransactionFilters; sort?: string } | undefined,
  { rejectValue: string; state: { transactions: TransactionsState } }
>('transactions/fetch', async (arg, thunkApi) => {
  const state = thunkApi.getState().transactions;
  const filters = arg?.filters ?? state.activeFilters;
  const page = arg?.page ?? state.pagination.page;
  const size = arg?.size ?? state.pagination.size;
  const sort = arg?.sort ?? state.sorting;
  try {
    const response = await apiClient.get<TransactionPage>('/transactions', {
      params: params(filters, page, size, sort),
      signal: thunkApi.signal
    });
    return response.data;
  } catch (error: any) {
    if (error.name === 'CanceledError') {
      throw error;
    }
    return thunkApi.rejectWithValue(errorMessage(error, 'Unable to load transactions'));
  }
});

export const fetchRecentTransactions = createAsyncThunk<BankTransaction[], void, { rejectValue: string }>(
  'transactions/fetchRecent',
  async (_, thunkApi) => {
    try {
      const response = await apiClient.get<TransactionPage>('/transactions', {
        params: { page: 0, size: 5, sort: 'authorizedAt,desc' },
        signal: thunkApi.signal
      });
      return response.data.content;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to load recent transactions'));
    }
  }
);

export const fetchTransactionDetail = createAsyncThunk<BankTransaction, string, { rejectValue: string }>(
  'transactions/fetchDetail',
  async (transactionId, thunkApi) => {
    try {
      const response = await apiClient.get<BankTransaction>(`/transactions/${transactionId}`, { signal: thunkApi.signal });
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to load transaction details'));
    }
  }
);

export const exportTransactionsCsv = createAsyncThunk<
  string,
  void,
  { rejectValue: string; state: { transactions: TransactionsState } }
>('transactions/exportCsv', async (_, thunkApi) => {
  const state = thunkApi.getState().transactions;
  try {
    const response = await apiClient.get<Blob>('/transactions/export', {
      params: params(state.activeFilters, 0, state.pagination.size, state.sorting),
      responseType: 'blob'
    });
    const disposition = response.headers['content-disposition'] as string | undefined;
    const filename = disposition?.match(/filename="([^"]+)"/)?.[1] ?? 'novabank-transactions.csv';
    const url = URL.createObjectURL(response.data);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
    return filename;
  } catch (error: any) {
    return thunkApi.rejectWithValue(errorMessage(error, 'Unable to export transactions'));
  }
});

const transactionsSlice = createSlice({
  name: 'transactions',
  initialState,
  reducers: {
    setFilters(state, action: { payload: Partial<TransactionFilters> }) {
      state.activeFilters = { ...state.activeFilters, ...action.payload };
      state.pagination.page = 0;
    },
    clearFilters(state) {
      state.activeFilters = emptyFilters;
      state.pagination.page = 0;
    },
    setSorting(state, action: { payload: string }) {
      state.sorting = action.payload;
      state.pagination.page = 0;
    },
    clearSelectedTransaction(state) {
      state.selected = null;
      state.detailError = null;
    }
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchTransactions.pending, (state, action) => {
        state.loading = true;
        state.error = null;
        state.currentRequestId = action.meta.requestId;
      })
      .addCase(fetchTransactions.fulfilled, (state, action) => {
        if (state.currentRequestId !== action.meta.requestId) {
          return;
        }
        state.loading = false;
        state.transactions = action.payload.content;
        state.pagination = {
          page: action.payload.page,
          size: action.payload.size,
          totalElements: action.payload.totalElements,
          totalPages: action.payload.totalPages,
          first: action.payload.first,
          last: action.payload.last
        };
        state.sorting = action.payload.sort;
        state.currentRequestId = null;
      })
      .addCase(fetchTransactions.rejected, (state, action) => {
        if (state.currentRequestId !== action.meta.requestId) {
          return;
        }
        state.loading = false;
        state.error = action.payload ?? 'Unable to load transactions';
        state.currentRequestId = null;
      })
      .addCase(fetchRecentTransactions.pending, (state) => {
        state.recentLoading = true;
      })
      .addCase(fetchRecentTransactions.fulfilled, (state, action) => {
        state.recentLoading = false;
        state.recentTransactions = action.payload;
      })
      .addCase(fetchRecentTransactions.rejected, (state, action) => {
        state.recentLoading = false;
        state.error = action.payload ?? 'Unable to load recent transactions';
      })
      .addCase(fetchTransactionDetail.pending, (state) => {
        state.detailLoading = true;
        state.detailError = null;
      })
      .addCase(fetchTransactionDetail.fulfilled, (state, action) => {
        state.detailLoading = false;
        state.selected = action.payload;
      })
      .addCase(fetchTransactionDetail.rejected, (state, action) => {
        state.detailLoading = false;
        state.detailError = action.payload ?? 'Unable to load transaction details';
      })
      .addCase(exportTransactionsCsv.pending, (state) => {
        state.exportState = 'loading';
        state.exportError = null;
      })
      .addCase(exportTransactionsCsv.fulfilled, (state) => {
        state.exportState = 'succeeded';
      })
      .addCase(exportTransactionsCsv.rejected, (state, action) => {
        state.exportState = 'failed';
        state.exportError = action.payload ?? 'Unable to export transactions';
      });
  }
});

export const { setFilters, clearFilters, setSorting, clearSelectedTransaction } = transactionsSlice.actions;
export default transactionsSlice.reducer;
