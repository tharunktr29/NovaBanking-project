import { configureStore } from '@reduxjs/toolkit';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import cardsReducer, { activateCard, fetchCardControls, fetchCards, requestReplacement, updateCardControls } from './cardsSlice';
import type { BankCard, CardControls } from './cardsSlice';
import { apiClient } from '../../api/client';

type CardsState = NonNullable<Parameters<typeof cardsReducer>[0]>;

vi.mock('../../api/client', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn()
  }
}));

describe('cardsSlice', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.stubGlobal('crypto', { randomUUID: () => 'idem-key-12345' });
  });

  it('loads real cards from the API', async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({ data: page([sampleDebitCard()]) });
    const store = testStore();

    await store.dispatch(fetchCards(undefined));

    expect(apiClient.get).toHaveBeenCalledWith('/cards', expect.objectContaining({
      params: { page: 0, size: 20, sort: 'createdAt,desc' },
      signal: expect.any(AbortSignal)
    }));
    expect(store.getState().cards.cards[0].maskedCardNumber).toBe('**** **** **** 4242');
    expect(JSON.stringify(store.getState().cards.cards)).not.toContain('full-card-number');
  });

  it('activates a pending card with an idempotency key', async () => {
    const activated = { ...sampleDebitCard(), status: 'ACTIVE' as const };
    vi.mocked(apiClient.post).mockResolvedValueOnce({ data: activated });
    const store = testStore([sampleDebitCard()]);

    await store.dispatch(activateCard(activated.cardId));

    expect(apiClient.post).toHaveBeenCalledWith(`/cards/${activated.cardId}/activate`, undefined, {
      headers: { 'Idempotency-Key': 'idem-key-12345' }
    });
    expect(store.getState().cards.message).toBe('Card activated');
    expect(store.getState().cards.cards[0].status).toBe('ACTIVE');
  });

  it('requests a replacement safely', async () => {
    vi.mocked(apiClient.post).mockResolvedValueOnce({
      data: { cardId: sampleDebitCard().cardId, requestReference: 'RPL-12345678', status: 'REQUESTED', requestedAt: '2026-08-11T12:00:00Z', message: 'Replacement request received' }
    });
    const store = testStore([sampleDebitCard()]);

    await store.dispatch(requestReplacement({ cardId: sampleDebitCard().cardId, reason: 'LOST' }));

    expect(apiClient.post).toHaveBeenCalledWith(`/cards/${sampleDebitCard().cardId}/replacement-requests`, { reason: 'LOST' }, {
      headers: { 'Idempotency-Key': 'idem-key-12345' }
    });
    expect(store.getState().cards.cards[0].status).toBe('REPLACEMENT_REQUESTED');
  });

  it('loads and updates spending controls', async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({ data: controls() });
    vi.mocked(apiClient.put).mockResolvedValueOnce({ data: { ...controls(), dailyPurchaseLimit: '900.00' } });
    const store = testStore([sampleDebitCard()]);

    await store.dispatch(fetchCardControls(sampleDebitCard().cardId));
    await store.dispatch(updateCardControls({ cardId: sampleDebitCard().cardId, controls: { ...controls(), dailyPurchaseLimit: '900.00' } }));

    expect(apiClient.get).toHaveBeenCalledWith(`/cards/${sampleDebitCard().cardId}/controls`, expect.objectContaining({ signal: expect.any(AbortSignal) }));
    expect(apiClient.put).toHaveBeenCalledWith(`/cards/${sampleDebitCard().cardId}/controls`, expect.objectContaining({ dailyPurchaseLimit: '900.00' }), {
      headers: { 'Idempotency-Key': 'idem-key-12345' }
    });
    expect(store.getState().cards.cardControls?.dailyPurchaseLimit).toBe('900.00');
  });

  it('records API failures for retryable card loading', async () => {
    vi.mocked(apiClient.get).mockRejectedValueOnce({ response: { data: { message: 'Service temporarily unavailable' } } });
    const store = testStore();

    await store.dispatch(fetchCards(undefined));

    expect(store.getState().cards.loading).toBe(false);
    expect(store.getState().cards.error).toBe('Service temporarily unavailable');
  });

  function testStore(cards: BankCard[] = []) {
    const preloadedCardsState: CardsState = {
      cards,
      selectedCard: cards[0] ?? null,
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
      pagination: { page: 0, size: 20, totalElements: cards.length, totalPages: 1, first: true, last: true },
      sorting: 'createdAt,desc'
    };

    return configureStore({
      reducer: {
        cards: cardsReducer
      },
      preloadedState: {
        cards: preloadedCardsState
      }
    });
  }

  function page(content: BankCard[]) {
    return {
      content,
      page: 0,
      size: 20,
      totalElements: content.length,
      totalPages: 1,
      first: true,
      last: true,
      sort: 'createdAt,desc'
    };
  }

  function sampleDebitCard(): BankCard {
    return {
      cardId: '88888888-8888-8888-8888-888888888888',
      accountId: '44444444-4444-4444-4444-444444444444',
      cardType: 'DEBIT',
      cardNetwork: 'VISA',
      status: 'PENDING_ACTIVATION',
      cardholderName: 'Demo User',
      maskedCardNumber: '**** **** **** 4242',
      lastFour: '4242',
      expirationMonth: 8,
      expirationYear: 2029,
      activatedAt: null,
      expiresAt: '2029-08-31T23:59:59Z',
      controls: {
        onlinePurchasesEnabled: true,
        contactlessEnabled: true,
        internationalPurchasesEnabled: false,
        atmWithdrawalsEnabled: true,
        dailyPurchaseLimit: '1500.00',
        dailyAtmLimit: '400.00',
        currency: 'USD'
      },
      credit: null,
      debitBalance: {
        currentBalance: '4286.42',
        availableBalance: '4036.42',
        pendingDebitAmount: '250.00',
        pendingCreditAmount: '0.00',
        currency: 'USD',
        asOf: '2026-08-11T12:00:00Z',
        unavailable: false
      },
      linkedAccount: {
        accountId: '44444444-4444-4444-4444-444444444444',
        nickname: 'Daily Checking',
        accountType: 'CHECKING',
        status: 'ACTIVE',
        unavailable: false
      }
    };
  }

  function controls(): CardControls {
    return {
      cardId: sampleDebitCard().cardId,
      onlinePurchasesEnabled: true,
      contactlessEnabled: true,
      internationalPurchasesEnabled: false,
      atmWithdrawalsEnabled: true,
      dailyPurchaseLimit: '1500.00',
      dailyAtmLimit: '400.00',
      currency: 'USD',
      updatedAt: '2026-08-11T12:00:00Z'
    };
  }
});
