import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { apiClient } from '../../api/client';

export type PaymentType = 'INTERNAL_TRANSFER' | 'CREDIT_CARD_PAYMENT' | 'EXTERNAL_ACCOUNT_PAYMENT';
export type PaymentStatus = 'SCHEDULED' | 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED' | 'REVERSAL_PENDING' | 'REVERSED';
export type ExecutionType = 'IMMEDIATE' | 'SCHEDULED';
export type ExternalAccountType = 'CHECKING' | 'SAVINGS';
export type PayeeStatus = 'PENDING_VERIFICATION' | 'VERIFIED' | 'DISABLED';

export interface PaymentOrder {
  id: string;
  paymentReference: string;
  paymentType: PaymentType;
  status: PaymentStatus;
  sourceAccountId: string;
  destinationAccountId: string | null;
  destinationCardId: string | null;
  externalPayeeId: string | null;
  amount: string | number;
  currency: string;
  memo: string | null;
  executionType: ExecutionType;
  scheduledFor: string | null;
  processingStartedAt: string | null;
  completedAt: string | null;
  failedAt: string | null;
  failureCode: string | null;
  failureMessage: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PaymentHistoryItem {
  previousStatus: PaymentStatus | null;
  newStatus: PaymentStatus;
  reasonCode: string | null;
  occurredAt: string;
}

export interface Payee {
  id: string;
  payeeReference: string;
  nickname: string;
  bankName: string;
  accountType: ExternalAccountType;
  maskedAccountNumber: string;
  status: PayeeStatus;
  createdAt: string;
  updatedAt: string;
}

export interface PaymentPage {
  content: PaymentOrder[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface MoneyMovementRequest {
  sourceAccountId: string;
  destinationAccountId?: string;
  destinationCardId?: string;
  externalPayeeId?: string;
  amount: string;
  currency: string;
  memo?: string;
  executionType: ExecutionType;
  scheduledFor?: string | null;
}

interface PaymentsState {
  payments: PaymentOrder[];
  selectedPayment: PaymentOrder | null;
  history: PaymentHistoryItem[];
  payees: Payee[];
  status: 'idle' | 'loading' | 'succeeded' | 'failed';
  mutationState: 'idle' | 'loading' | 'succeeded' | 'failed';
  error: string | null;
  message: string | null;
  pagination: Omit<PaymentPage, 'content'>;
}

const initialState: PaymentsState = {
  payments: [],
  selectedPayment: null,
  history: [],
  payees: [],
  status: 'idle',
  mutationState: 'idle',
  error: null,
  message: null,
  pagination: { page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true }
};

function errorMessage(error: any, fallback: string) {
  return error.response?.data?.message ?? fallback;
}

function idempotencyKey(prefix = 'payment') {
  return typeof globalThis.crypto?.randomUUID === 'function'
    ? globalThis.crypto.randomUUID()
    : `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export const fetchPayments = createAsyncThunk<PaymentPage, { page?: number; size?: number; status?: string } | undefined, { rejectValue: string }>(
  'payments/fetch',
  async (arg, thunkApi) => {
    try {
      const response = await apiClient.get<PaymentPage>('/payments', {
        params: { page: arg?.page ?? 0, size: arg?.size ?? 20, status: arg?.status || undefined },
        signal: thunkApi.signal
      });
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to load payments'));
    }
  }
);

export const fetchPayees = createAsyncThunk<Payee[], void, { rejectValue: string }>(
  'payments/fetchPayees',
  async (_, thunkApi) => {
    try {
      const response = await apiClient.get<Payee[]>('/payees', { signal: thunkApi.signal });
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to load payees'));
    }
  }
);

export const fetchPaymentHistory = createAsyncThunk<PaymentHistoryItem[], string, { rejectValue: string }>(
  'payments/fetchHistory',
  async (paymentId, thunkApi) => {
    try {
      const response = await apiClient.get<PaymentHistoryItem[]>(`/payments/${paymentId}/history`, { signal: thunkApi.signal });
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to load payment history'));
    }
  }
);

export const createInternalTransfer = createAsyncThunk<PaymentOrder, MoneyMovementRequest, { rejectValue: string }>(
  'payments/internalTransfer',
  async (request, thunkApi) => {
    try {
      const response = await apiClient.post<PaymentOrder>('/transfers/internal', request, { headers: { 'Idempotency-Key': idempotencyKey('transfer') } });
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to submit transfer'));
    }
  }
);

export const createCreditCardPayment = createAsyncThunk<PaymentOrder, MoneyMovementRequest, { rejectValue: string }>(
  'payments/creditCardPayment',
  async (request, thunkApi) => {
    try {
      const response = await apiClient.post<PaymentOrder>('/payments/credit-card', request, { headers: { 'Idempotency-Key': idempotencyKey('card-pay') } });
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to submit credit card payment'));
    }
  }
);

export const createExternalPayment = createAsyncThunk<PaymentOrder, MoneyMovementRequest, { rejectValue: string }>(
  'payments/externalPayment',
  async (request, thunkApi) => {
    try {
      const response = await apiClient.post<PaymentOrder>('/payments/external', request, { headers: { 'Idempotency-Key': idempotencyKey('external-pay') } });
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to submit external payment'));
    }
  }
);

export const createPayee = createAsyncThunk<Payee, { nickname: string; bankName: string; accountType: ExternalAccountType; maskedAccountNumber: string }, { rejectValue: string }>(
  'payments/createPayee',
  async (request, thunkApi) => {
    try {
      const response = await apiClient.post<Payee>('/payees', request, { headers: { 'Idempotency-Key': idempotencyKey('payee') } });
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to create payee'));
    }
  }
);

export const cancelPayment = createAsyncThunk<PaymentOrder, string, { rejectValue: string }>(
  'payments/cancel',
  async (paymentId, thunkApi) => {
    try {
      const response = await apiClient.post<PaymentOrder>(`/payments/${paymentId}/cancel`, undefined, { headers: { 'Idempotency-Key': idempotencyKey('cancel') } });
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to cancel payment'));
    }
  }
);

const paymentsSlice = createSlice({
  name: 'payments',
  initialState,
  reducers: {
    clearPaymentMessage(state) {
      state.message = null;
      state.error = null;
    }
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchPayments.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(fetchPayments.fulfilled, (state, action) => {
        state.status = 'succeeded';
        state.payments = action.payload.content;
        state.pagination = {
          page: action.payload.page,
          size: action.payload.size,
          totalElements: action.payload.totalElements,
          totalPages: action.payload.totalPages,
          first: action.payload.first,
          last: action.payload.last
        };
      })
      .addCase(fetchPayments.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload ?? 'Unable to load payments';
      })
      .addCase(fetchPayees.fulfilled, (state, action) => {
        state.payees = action.payload;
      })
      .addCase(fetchPayees.rejected, (state, action) => {
        state.error = action.payload ?? 'Unable to load payees';
      })
      .addCase(fetchPaymentHistory.fulfilled, (state, action) => {
        state.history = action.payload;
      })
      .addCase(createInternalTransfer.pending, mutationPending)
      .addCase(createCreditCardPayment.pending, mutationPending)
      .addCase(createExternalPayment.pending, mutationPending)
      .addCase(createPayee.pending, mutationPending)
      .addCase(cancelPayment.pending, mutationPending)
      .addCase(createInternalTransfer.fulfilled, (state, action) => paymentMutationSucceeded(state, action.payload, 'Transfer submitted'))
      .addCase(createCreditCardPayment.fulfilled, (state, action) => paymentMutationSucceeded(state, action.payload, 'Credit card payment submitted'))
      .addCase(createExternalPayment.fulfilled, (state, action) => paymentMutationSucceeded(state, action.payload, 'External payment submitted'))
      .addCase(cancelPayment.fulfilled, (state, action) => paymentMutationSucceeded(state, action.payload, 'Scheduled payment cancelled'))
      .addCase(createPayee.fulfilled, (state, action) => {
        state.mutationState = 'succeeded';
        state.payees.push(action.payload);
        state.message = 'Payee created';
      })
      .addCase(createInternalTransfer.rejected, mutationRejected)
      .addCase(createCreditCardPayment.rejected, mutationRejected)
      .addCase(createExternalPayment.rejected, mutationRejected)
      .addCase(createPayee.rejected, mutationRejected)
      .addCase(cancelPayment.rejected, mutationRejected);
  }
});

function mutationPending(state: PaymentsState) {
  state.mutationState = 'loading';
  state.error = null;
  state.message = null;
}

function paymentMutationSucceeded(state: PaymentsState, payment: PaymentOrder, message: string) {
  state.mutationState = 'succeeded';
  state.selectedPayment = payment;
  state.payments = [payment, ...state.payments.filter((item) => item.id !== payment.id)];
  state.message = message;
}

function mutationRejected(state: PaymentsState, action: { payload?: string }) {
  state.mutationState = 'failed';
  state.error = action.payload ?? 'Payment action failed';
}

export const { clearPaymentMessage } = paymentsSlice.actions;
export default paymentsSlice.reducer;
