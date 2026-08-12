import { configureStore } from '@reduxjs/toolkit';
import accountsReducer from './features/accounts/accountsSlice';
import authReducer from './features/auth/authSlice';
import customerReducer from './features/customer/customerSlice';
import loginActivityReducer from './features/loginActivity/loginActivitySlice';
import transactionsReducer from './features/transactions/transactionsSlice';

const reducer = {
  auth: authReducer,
  customer: customerReducer,
  accounts: accountsReducer,
  loginActivity: loginActivityReducer,
  transactions: transactionsReducer
};

export function setupStore() {
  return configureStore({
    reducer
  });
}

export const store = setupStore();

export type AppStore = ReturnType<typeof setupStore>;
export type RootState = ReturnType<AppStore['getState']>;
export type AppDispatch = AppStore['dispatch'];
