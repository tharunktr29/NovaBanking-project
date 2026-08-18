import paymentsReducer, { cancelPayment, createInternalTransfer, fetchPayments } from './paymentsSlice';
import { describe, expect, it } from 'vitest';

describe('paymentsSlice', () => {
  it('stores loaded payments', () => {
    const state = paymentsReducer(undefined, fetchPayments.fulfilled({
      content: [{
        id: 'p1',
        paymentReference: 'PMT-1',
        paymentType: 'INTERNAL_TRANSFER',
        status: 'COMPLETED',
        sourceAccountId: 'a1',
        destinationAccountId: 'a2',
        destinationCardId: null,
        externalPayeeId: null,
        amount: '25.00',
        currency: 'USD',
        memo: null,
        executionType: 'IMMEDIATE',
        scheduledFor: null,
        processingStartedAt: null,
        completedAt: null,
        failedAt: null,
        failureCode: null,
        failureMessage: null,
        createdAt: '2026-08-13T00:00:00Z',
        updatedAt: '2026-08-13T00:00:00Z',
      }],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true
    }, '', undefined));

    expect(state.payments).toHaveLength(1);
    expect(state.status).toBe('succeeded');
  });

  it('places a submitted transfer at the top of history', () => {
    const payment = {
      id: 'p2',
      paymentReference: 'PMT-2',
      paymentType: 'INTERNAL_TRANSFER' as const,
      status: 'COMPLETED' as const,
      sourceAccountId: 'a1',
      destinationAccountId: 'a2',
      destinationCardId: null,
      externalPayeeId: null,
      amount: '10.00',
      currency: 'USD',
      memo: null,
      executionType: 'IMMEDIATE' as const,
      scheduledFor: null,
      processingStartedAt: null,
      completedAt: null,
      failedAt: null,
      failureCode: null,
      failureMessage: null,
      createdAt: '2026-08-13T00:00:00Z',
      updatedAt: '2026-08-13T00:00:00Z',
    };

    const state = paymentsReducer(undefined, createInternalTransfer.fulfilled(payment, '', {
      sourceAccountId: 'a1',
      destinationAccountId: 'a2',
      amount: '10.00',
      currency: 'USD',
      executionType: 'IMMEDIATE'
    }));

    expect(state.selectedPayment?.id).toBe('p2');
    expect(state.message).toMatch(/Transfer/);
  });

  it('stores cancellation errors', () => {
    const state = paymentsReducer(undefined, cancelPayment.rejected(null, '', 'p1', 'Only scheduled payments can be cancelled'));
    expect(state.mutationState).toBe('failed');
    expect(state.error).toBe('Only scheduled payments can be cancelled');
  });
});
