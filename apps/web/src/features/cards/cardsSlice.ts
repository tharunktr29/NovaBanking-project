import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { apiClient } from '../../api/client';

export type CardType = 'DEBIT' | 'CREDIT';
export type CardNetwork = 'VISA' | 'MASTERCARD';
export type CardStatus = 'PENDING_ACTIVATION' | 'ACTIVE' | 'LOCKED' | 'REPLACEMENT_REQUESTED' | 'EXPIRED' | 'CLOSED';
export type CardAction = 'CREATED' | 'ACTIVATED' | 'LOCKED' | 'UNLOCKED' | 'REPLACEMENT_REQUESTED' | 'CONTROLS_UPDATED' | 'EXPIRED' | 'CLOSED';
export type ReplacementReason = 'LOST' | 'STOLEN' | 'DAMAGED' | 'EXPIRED' | 'NAME_CHANGE' | 'OTHER';

export interface CardControls {
  cardId?: string;
  onlinePurchasesEnabled: boolean;
  contactlessEnabled: boolean;
  internationalPurchasesEnabled: boolean;
  atmWithdrawalsEnabled: boolean;
  dailyPurchaseLimit: string | number;
  dailyAtmLimit: string | number;
  currency: string;
  updatedAt?: string;
}

export interface CreditSummary {
  creditLimit: string | number;
  currentBalance: string | number;
  availableCredit: string | number;
  minimumPaymentDue: string | number;
  paymentDueDate: string;
  currency: string;
  updatedAt: string;
}

export interface DebitBalanceSummary {
  currentBalance: string | number | null;
  availableBalance: string | number | null;
  pendingDebitAmount: string | number | null;
  pendingCreditAmount: string | number | null;
  currency: string | null;
  asOf: string | null;
  unavailable: boolean;
}

export interface LinkedAccountSummary {
  accountId: string;
  nickname: string | null;
  accountType: string | null;
  status: string | null;
  unavailable: boolean;
}

export interface BankCard {
  cardId: string;
  accountId: string;
  cardType: CardType;
  cardNetwork: CardNetwork;
  status: CardStatus;
  cardholderName: string;
  maskedCardNumber: string;
  lastFour: string;
  expirationMonth: number;
  expirationYear: number;
  activatedAt: string | null;
  expiresAt: string;
  controls: Omit<CardControls, 'cardId' | 'updatedAt'> | null;
  credit: CreditSummary | null;
  debitBalance: DebitBalanceSummary | null;
  linkedAccount: LinkedAccountSummary | null;
}

export interface CardHistoryItem {
  action: CardAction;
  previousStatus: CardStatus | null;
  newStatus: CardStatus;
  reasonCode: string | null;
  occurredAt: string;
}

export interface CardPage {
  content: BankCard[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  sort: string;
}

export interface CardFilters {
  cardType: string;
  status: string;
  accountId: string;
}

interface CardsState {
  cards: BankCard[];
  selectedCard: BankCard | null;
  cardHistory: CardHistoryItem[];
  cardControls: CardControls | null;
  loading: boolean;
  detailLoading: boolean;
  historyLoading: boolean;
  controlsLoading: boolean;
  mutationState: 'idle' | 'loading' | 'succeeded' | 'failed';
  replacementState: 'idle' | 'loading' | 'succeeded' | 'failed';
  error: string | null;
  mutationError: string | null;
  message: string | null;
  filters: CardFilters;
  pagination: Omit<CardPage, 'content' | 'sort'>;
  sorting: string;
}

const initialState: CardsState = {
  cards: [],
  selectedCard: null,
  cardHistory: [],
  cardControls: null,
  loading: false,
  detailLoading: false,
  historyLoading: false,
  controlsLoading: false,
  mutationState: 'idle',
  replacementState: 'idle',
  error: null,
  mutationError: null,
  message: null,
  filters: { cardType: '', status: '', accountId: '' },
  pagination: { page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true },
  sorting: 'createdAt,desc'
};

function errorMessage(error: any, fallback: string) {
  return error.response?.data?.message ?? fallback;
}

function idempotencyKey() {
  return typeof globalThis.crypto?.randomUUID === 'function'
    ? globalThis.crypto.randomUUID()
    : `card-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function filterParams(filters: CardFilters, page: number, size: number, sort: string) {
  const params: Record<string, string | number> = { page, size, sort };
  Object.entries(filters).forEach(([key, value]) => {
    if (value) {
      params[key] = value;
    }
  });
  return params;
}

export const fetchCards = createAsyncThunk<CardPage, { page?: number; size?: number; filters?: CardFilters; sort?: string } | undefined, { rejectValue: string; state: { cards: CardsState } }>(
  'cards/fetch',
  async (arg, thunkApi) => {
    const state = thunkApi.getState().cards;
    try {
      const response = await apiClient.get<CardPage>('/cards', {
        params: filterParams(
          arg?.filters ?? state.filters,
          arg?.page ?? state.pagination.page,
          arg?.size ?? state.pagination.size,
          arg?.sort ?? state.sorting
        ),
        signal: thunkApi.signal
      });
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to load cards'));
    }
  }
);

export const fetchCard = createAsyncThunk<BankCard, string, { rejectValue: string }>(
  'cards/fetchDetail',
  async (cardId, thunkApi) => {
    try {
      const response = await apiClient.get<BankCard>(`/cards/${cardId}`, { signal: thunkApi.signal });
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to load card details'));
    }
  }
);

export const fetchCardHistory = createAsyncThunk<CardHistoryItem[], string, { rejectValue: string }>(
  'cards/fetchHistory',
  async (cardId, thunkApi) => {
    try {
      const response = await apiClient.get<CardHistoryItem[]>(`/cards/${cardId}/history`, { signal: thunkApi.signal });
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to load card history'));
    }
  }
);

export const fetchCardControls = createAsyncThunk<CardControls, string, { rejectValue: string }>(
  'cards/fetchControls',
  async (cardId, thunkApi) => {
    try {
      const response = await apiClient.get<CardControls>(`/cards/${cardId}/controls`, { signal: thunkApi.signal });
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to load card controls'));
    }
  }
);

export const activateCard = createAsyncThunk<BankCard, string, { rejectValue: string }>(
  'cards/activate',
  async (cardId, thunkApi) => mutateCard(cardId, 'activate', thunkApi)
);

export const lockCard = createAsyncThunk<BankCard, string, { rejectValue: string }>(
  'cards/lock',
  async (cardId, thunkApi) => mutateCard(cardId, 'lock', thunkApi)
);

export const unlockCard = createAsyncThunk<BankCard, string, { rejectValue: string }>(
  'cards/unlock',
  async (cardId, thunkApi) => mutateCard(cardId, 'unlock', thunkApi)
);

export const requestReplacement = createAsyncThunk<{ cardId: string; requestReference: string; status: string; requestedAt: string; message: string }, { cardId: string; reason: ReplacementReason }, { rejectValue: string }>(
  'cards/requestReplacement',
  async ({ cardId, reason }, thunkApi) => {
    try {
      const response = await apiClient.post(`/cards/${cardId}/replacement-requests`, { reason }, {
        headers: { 'Idempotency-Key': idempotencyKey() }
      });
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to request replacement card'));
    }
  }
);

export const updateCardControls = createAsyncThunk<CardControls, { cardId: string; controls: CardControls }, { rejectValue: string }>(
  'cards/updateControls',
  async ({ cardId, controls }, thunkApi) => {
    try {
      const response = await apiClient.put<CardControls>(`/cards/${cardId}/controls`, controls, {
        headers: { 'Idempotency-Key': idempotencyKey() }
      });
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to update card controls'));
    }
  }
);

async function mutateCard(cardId: string, action: 'activate' | 'lock' | 'unlock', thunkApi: { rejectWithValue(value: string): any }) {
  try {
    const response = await apiClient.post<BankCard>(`/cards/${cardId}/${action}`, undefined, {
      headers: { 'Idempotency-Key': idempotencyKey() }
    });
    return response.data;
  } catch (error: any) {
    return thunkApi.rejectWithValue(errorMessage(error, `Unable to ${action} card`));
  }
}

const cardsSlice = createSlice({
  name: 'cards',
  initialState,
  reducers: {
    setCardFilters(state, action: { payload: Partial<CardFilters> }) {
      state.filters = { ...state.filters, ...action.payload };
      state.pagination.page = 0;
    },
    clearCardFilters(state) {
      state.filters = { cardType: '', status: '', accountId: '' };
      state.pagination.page = 0;
    },
    setCardSorting(state, action: { payload: string }) {
      state.sorting = action.payload;
      state.pagination.page = 0;
    },
    clearCardsMessage(state) {
      state.message = null;
      state.error = null;
      state.mutationError = null;
    }
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchCards.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchCards.fulfilled, (state, action) => {
        state.loading = false;
        state.cards = action.payload.content;
        state.pagination = {
          page: action.payload.page,
          size: action.payload.size,
          totalElements: action.payload.totalElements,
          totalPages: action.payload.totalPages,
          first: action.payload.first,
          last: action.payload.last
        };
        state.sorting = action.payload.sort;
        if (!state.selectedCard && action.payload.content.length > 0) {
          state.selectedCard = action.payload.content[0];
        }
      })
      .addCase(fetchCards.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload ?? 'Unable to load cards';
      })
      .addCase(fetchCard.pending, (state) => {
        state.detailLoading = true;
      })
      .addCase(fetchCard.fulfilled, (state, action) => {
        state.detailLoading = false;
        state.selectedCard = action.payload;
        state.cards = state.cards.map((card) => card.cardId === action.payload.cardId ? action.payload : card);
      })
      .addCase(fetchCard.rejected, (state, action) => {
        state.detailLoading = false;
        state.error = action.payload ?? 'Unable to load card details';
      })
      .addCase(fetchCardHistory.pending, (state) => {
        state.historyLoading = true;
      })
      .addCase(fetchCardHistory.fulfilled, (state, action) => {
        state.historyLoading = false;
        state.cardHistory = action.payload;
      })
      .addCase(fetchCardControls.pending, (state) => {
        state.controlsLoading = true;
      })
      .addCase(fetchCardControls.fulfilled, (state, action) => {
        state.controlsLoading = false;
        state.cardControls = action.payload;
      })
      .addCase(fetchCardControls.rejected, (state, action) => {
        state.controlsLoading = false;
        state.mutationError = action.payload ?? 'Unable to load card controls';
      })
      .addCase(activateCard.pending, mutationPending)
      .addCase(lockCard.pending, mutationPending)
      .addCase(unlockCard.pending, mutationPending)
      .addCase(updateCardControls.pending, mutationPending)
      .addCase(activateCard.fulfilled, (state, action) => cardMutationSucceeded(state, action.payload, 'Card activated'))
      .addCase(lockCard.fulfilled, (state, action) => cardMutationSucceeded(state, action.payload, 'Card locked'))
      .addCase(unlockCard.fulfilled, (state, action) => cardMutationSucceeded(state, action.payload, 'Card unlocked'))
      .addCase(updateCardControls.fulfilled, (state, action) => {
        state.mutationState = 'succeeded';
        state.cardControls = action.payload;
        if (state.selectedCard) {
          state.selectedCard.controls = action.payload;
        }
        state.cards = state.cards.map((card) => card.cardId === action.payload.cardId ? { ...card, controls: action.payload } : card);
        state.message = 'Card controls updated';
      })
      .addCase(activateCard.rejected, mutationRejected)
      .addCase(lockCard.rejected, mutationRejected)
      .addCase(unlockCard.rejected, mutationRejected)
      .addCase(updateCardControls.rejected, mutationRejected)
      .addCase(requestReplacement.pending, (state) => {
        state.replacementState = 'loading';
        state.mutationError = null;
      })
      .addCase(requestReplacement.fulfilled, (state, action) => {
        state.replacementState = 'succeeded';
        state.cards = state.cards.map((card) => card.cardId === action.payload.cardId ? { ...card, status: 'REPLACEMENT_REQUESTED' } : card);
        if (state.selectedCard?.cardId === action.payload.cardId) {
          state.selectedCard = { ...state.selectedCard, status: 'REPLACEMENT_REQUESTED' };
        }
        state.message = `${action.payload.message}: ${action.payload.requestReference}`;
      })
      .addCase(requestReplacement.rejected, (state, action) => {
        state.replacementState = 'failed';
        state.mutationError = action.payload ?? 'Unable to request replacement card';
      });
  }
});

function mutationPending(state: CardsState) {
  state.mutationState = 'loading';
  state.mutationError = null;
  state.message = null;
}

function cardMutationSucceeded(state: CardsState, card: BankCard, message: string) {
  state.mutationState = 'succeeded';
  state.selectedCard = card;
  state.cards = state.cards.map((item) => item.cardId === card.cardId ? card : item);
  state.message = message;
}

function mutationRejected(state: CardsState, action: { payload?: string }) {
  state.mutationState = 'failed';
  state.mutationError = action.payload ?? 'Card action failed';
}

export const { setCardFilters, clearCardFilters, setCardSorting, clearCardsMessage } = cardsSlice.actions;
export default cardsSlice.reducer;
