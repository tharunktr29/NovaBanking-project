import { configureStore } from '@reduxjs/toolkit';
import { setAccessTokenProvider } from './api/client';
import accountsReducer from './features/accounts/accountsSlice';
import authReducer from './features/auth/authSlice';
import cardsReducer from './features/cards/cardsSlice';
import customerReducer from './features/customer/customerSlice';
import loginActivityReducer from './features/loginActivity/loginActivitySlice';
import paymentsReducer from './features/payments/paymentsSlice';
import transactionsReducer from './features/transactions/transactionsSlice';

const reducer = {
  auth: authReducer,
  customer: customerReducer,
  accounts: accountsReducer,
  cards: cardsReducer,
  loginActivity: loginActivityReducer,
  payments: paymentsReducer,
  transactions: transactionsReducer
};

export function setupStore() {
  return configureStore({
    reducer
  });
}

export const store = setupStore();
setAccessTokenProvider(() => store.getState().auth.accessToken);

export type AppStore = ReturnType<typeof setupStore>;
export type RootState = ReturnType<AppStore['getState']>;
export type AppDispatch = AppStore['dispatch'];
