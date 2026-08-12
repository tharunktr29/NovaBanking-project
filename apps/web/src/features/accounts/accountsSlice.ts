import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { apiClient } from '../../api/client';

export interface AccountBalance {
  id: string;
  accountId: string;
  currentBalance: string;
  availableBalance: string;
  pendingDebitAmount: string;
  pendingCreditAmount: string;
  asOf: string;
  version: number;
}

export interface Account {
  id: string;
  customerId: string;
  accountType: 'CHECKING' | 'SAVINGS';
  maskedAccountNumber: string;
  nickname: string;
  status: 'ACTIVE' | 'FROZEN' | 'CLOSED';
  currency: string;
  openedDate: string;
  createdAt: string;
  updatedAt: string;
  version: number;
  balance: AccountBalance | null;
}

interface AccountsState {
  accounts: Account[];
  balancesByAccountId: Record<string, AccountBalance>;
  status: 'idle' | 'loading' | 'succeeded' | 'failed';
  savingAccountId: string | null;
  message: string | null;
  error: string | null;
}

const initialState: AccountsState = {
  accounts: [],
  balancesByAccountId: {},
  status: 'idle',
  savingAccountId: null,
  message: null,
  error: null
};

function errorMessage(error: any, fallback: string) {
  return error.response?.data?.message ?? fallback;
}

export const fetchAccounts = createAsyncThunk<Account[], void, { rejectValue: string }>(
  'accounts/fetch',
  async (_, thunkApi) => {
    try {
      const response = await apiClient.get<Account[]>('/accounts');
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to load accounts'));
    }
  }
);

export const updateAccountNickname = createAsyncThunk<Account, { accountId: string; nickname: string }, { rejectValue: string }>(
  'accounts/updateNickname',
  async ({ accountId, nickname }, thunkApi) => {
    try {
      const response = await apiClient.patch<Account>(`/accounts/${accountId}/nickname`, { nickname });
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to update account nickname'));
    }
  }
);

export const fetchAccountBalance = createAsyncThunk<AccountBalance, string, { rejectValue: string }>(
  'accounts/fetchBalance',
  async (accountId, thunkApi) => {
    try {
      const response = await apiClient.get<AccountBalance>(`/accounts/${accountId}/balance`);
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to load account balance'));
    }
  }
);

const accountsSlice = createSlice({
  name: 'accounts',
  initialState,
  reducers: {
    clearAccountsMessage(state) {
      state.message = null;
      state.error = null;
    }
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchAccounts.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(fetchAccounts.fulfilled, (state, action) => {
        state.status = 'succeeded';
        state.accounts = action.payload;
        state.balancesByAccountId = Object.fromEntries(
          action.payload
            .filter((account) => account.balance)
            .map((account) => [account.id, account.balance as AccountBalance])
        );
      })
      .addCase(fetchAccounts.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload ?? 'Unable to load accounts';
      })
      .addCase(updateAccountNickname.pending, (state, action) => {
        state.savingAccountId = action.meta.arg.accountId;
        state.message = null;
        state.error = null;
      })
      .addCase(updateAccountNickname.fulfilled, (state, action) => {
        state.savingAccountId = null;
        state.accounts = state.accounts.map((account) => account.id === action.payload.id ? action.payload : account);
        if (action.payload.balance) {
          state.balancesByAccountId[action.payload.id] = action.payload.balance;
        }
        state.message = 'Account nickname updated';
      })
      .addCase(updateAccountNickname.rejected, (state, action) => {
        state.savingAccountId = null;
        state.error = action.payload ?? 'Unable to update account nickname';
      })
      .addCase(fetchAccountBalance.fulfilled, (state, action) => {
        state.balancesByAccountId[action.payload.accountId] = action.payload;
        state.accounts = state.accounts.map((account) => account.id === action.payload.accountId
          ? { ...account, balance: action.payload }
          : account);
      })
      .addCase(fetchAccountBalance.rejected, (state, action) => {
        state.error = action.payload ?? 'Unable to load account balance';
      });
  }
});

export const { clearAccountsMessage } = accountsSlice.actions;
export default accountsSlice.reducer;
