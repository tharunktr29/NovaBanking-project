import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { apiClient } from '../../api/client';

export interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  username: string | null;
  email: string | null;
  role: string | null;
  customerId: string | null;
  accessTokenExpiresAt: string | null;
  status: 'idle' | 'loading' | 'authenticated' | 'failed';
  error: string | null;
}

interface LoginPayload {
  usernameOrEmail: string;
  password: string;
}

interface RegisterPayload {
  username: string;
  email: string;
  password: string;
}

interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  accessTokenExpiresAt: string;
  customerId: string;
  username: string;
  role: string;
}

interface CurrentUserResponse {
  customerId: string;
  username: string;
  email: string;
  role: string;
}

const storageKeys = [
  'novabank.accessToken',
  'novabank.refreshToken',
  'novabank.username',
  'novabank.email',
  'novabank.role',
  'novabank.customerId',
  'novabank.accessTokenExpiresAt'
];

const persistedAccessToken = sessionStorage.getItem('novabank.accessToken');
const persistedRefreshToken = sessionStorage.getItem('novabank.refreshToken');
const persistedUsername = sessionStorage.getItem('novabank.username');

const initialState: AuthState = {
  accessToken: persistedAccessToken,
  refreshToken: persistedRefreshToken,
  username: persistedUsername,
  email: sessionStorage.getItem('novabank.email'),
  role: sessionStorage.getItem('novabank.role'),
  customerId: sessionStorage.getItem('novabank.customerId'),
  accessTokenExpiresAt: sessionStorage.getItem('novabank.accessTokenExpiresAt'),
  status: persistedAccessToken ? 'authenticated' : 'idle',
  error: null
};

function persistAuth(response: AuthResponse) {
  sessionStorage.setItem('novabank.accessToken', response.accessToken);
  sessionStorage.setItem('novabank.refreshToken', response.refreshToken);
  sessionStorage.setItem('novabank.username', response.username);
  sessionStorage.setItem('novabank.role', response.role);
  sessionStorage.setItem('novabank.customerId', response.customerId);
  sessionStorage.setItem('novabank.accessTokenExpiresAt', response.accessTokenExpiresAt);
}

function clearPersistedAuth() {
  storageKeys.forEach((key) => sessionStorage.removeItem(key));
}

function applyAuthResponse(state: AuthState, response: AuthResponse) {
  state.status = 'authenticated';
  state.accessToken = response.accessToken;
  state.refreshToken = response.refreshToken;
  state.username = response.username;
  state.role = response.role;
  state.customerId = response.customerId;
  state.accessTokenExpiresAt = response.accessTokenExpiresAt;
  state.error = null;
  persistAuth(response);
}

function errorMessage(error: any, fallback: string) {
  return error.response?.data?.message ?? error.response?.data?.detail ?? fallback;
}

export const login = createAsyncThunk<AuthResponse, LoginPayload, { rejectValue: string }>(
  'auth/login',
  async (payload, thunkApi) => {
    try {
      const response = await apiClient.post<AuthResponse>('/auth/login', payload);
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to sign in'));
    }
  }
);

export const registerAccount = createAsyncThunk<AuthResponse, RegisterPayload, { rejectValue: string }>(
  'auth/register',
  async (payload, thunkApi) => {
    try {
      const response = await apiClient.post<AuthResponse>('/auth/register', payload);
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to create account'));
    }
  }
);

export const refreshSession = createAsyncThunk<AuthResponse, void, { state: { auth: AuthState }; rejectValue: string }>(
  'auth/refresh',
  async (_, thunkApi) => {
    const refreshToken = thunkApi.getState().auth.refreshToken;
    if (!refreshToken) {
      return thunkApi.rejectWithValue('No refresh token is available');
    }
    try {
      const response = await apiClient.post<AuthResponse>('/auth/refresh', { refreshToken });
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to refresh session'));
    }
  }
);

export const fetchCurrentUser = createAsyncThunk<CurrentUserResponse, void, { rejectValue: string }>(
  'auth/me',
  async (_, thunkApi) => {
    try {
      const response = await apiClient.get<CurrentUserResponse>('/auth/me');
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to load profile'));
    }
  }
);

export const logoutServer = createAsyncThunk<void, void, { state: { auth: AuthState }; rejectValue: string }>(
  'auth/logout',
  async (_, thunkApi) => {
    const refreshToken = thunkApi.getState().auth.refreshToken;
    if (!refreshToken) {
      return;
    }
    try {
      await apiClient.post('/auth/logout', { refreshToken });
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to revoke session'));
    }
  }
);

export const changePassword = createAsyncThunk<string, ChangePasswordPayload, { rejectValue: string }>(
  'auth/changePassword',
  async (payload, thunkApi) => {
    try {
      const response = await apiClient.post<{ message: string }>('/auth/change-password', payload);
      return response.data.message;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to change password'));
    }
  }
);

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    clearAuthError(state) {
      state.error = null;
    },
    logoutLocal(state) {
      state.accessToken = null;
      state.refreshToken = null;
      state.username = null;
      state.email = null;
      state.role = null;
      state.customerId = null;
      state.accessTokenExpiresAt = null;
      state.status = 'idle';
      state.error = null;
      clearPersistedAuth();
    }
  },
  extraReducers: (builder) => {
    builder
      .addCase(login.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(login.fulfilled, (state, action) => {
        applyAuthResponse(state, action.payload);
      })
      .addCase(login.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload ?? 'Unable to sign in';
      })
      .addCase(registerAccount.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(registerAccount.fulfilled, (state, action) => {
        applyAuthResponse(state, action.payload);
      })
      .addCase(registerAccount.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload ?? 'Unable to create account';
      })
      .addCase(refreshSession.fulfilled, (state, action) => {
        applyAuthResponse(state, action.payload);
      })
      .addCase(refreshSession.rejected, (state, action) => {
        state.status = 'idle';
        state.error = action.payload ?? 'Unable to refresh session';
        state.accessToken = null;
        state.refreshToken = null;
        clearPersistedAuth();
      })
      .addCase(fetchCurrentUser.fulfilled, (state, action) => {
        state.customerId = action.payload.customerId;
        state.username = action.payload.username;
        state.email = action.payload.email;
        state.role = action.payload.role;
        sessionStorage.setItem('novabank.customerId', action.payload.customerId);
        sessionStorage.setItem('novabank.username', action.payload.username);
        sessionStorage.setItem('novabank.email', action.payload.email);
        sessionStorage.setItem('novabank.role', action.payload.role);
      })
      .addCase(logoutServer.fulfilled, (state) => {
        state.accessToken = null;
        state.refreshToken = null;
        state.username = null;
        state.email = null;
        state.role = null;
        state.customerId = null;
        state.accessTokenExpiresAt = null;
        state.status = 'idle';
        state.error = null;
        clearPersistedAuth();
      })
      .addCase(changePassword.pending, (state) => {
        state.error = null;
      })
      .addCase(changePassword.fulfilled, (state) => {
        state.accessToken = null;
        state.refreshToken = null;
        state.username = null;
        state.email = null;
        state.role = null;
        state.customerId = null;
        state.accessTokenExpiresAt = null;
        state.status = 'idle';
        state.error = null;
        clearPersistedAuth();
      })
      .addCase(changePassword.rejected, (state, action) => {
        state.status = 'authenticated';
        state.error = action.payload ?? 'Unable to change password';
      });
  }
});

export const { clearAuthError, logoutLocal } = authSlice.actions;
export default authSlice.reducer;
