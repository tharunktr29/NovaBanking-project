import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { apiClient } from '../../api/client';

export interface LoginActivity {
  timestamp: string;
  successful: boolean;
  eventType: string;
  maskedIpAddress: string;
  deviceSummary: string;
}

interface LoginActivityState {
  items: LoginActivity[];
  status: 'idle' | 'loading' | 'succeeded' | 'failed';
  error: string | null;
}

const initialState: LoginActivityState = {
  items: [],
  status: 'idle',
  error: null
};

export const fetchLoginActivity = createAsyncThunk<LoginActivity[], void, { rejectValue: string }>(
  'loginActivity/fetch',
  async (_, thunkApi) => {
    try {
      const response = await apiClient.get<LoginActivity[]>('/auth/login-activity');
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(error.response?.data?.message ?? 'Unable to load login activity');
    }
  }
);

const loginActivitySlice = createSlice({
  name: 'loginActivity',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchLoginActivity.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(fetchLoginActivity.fulfilled, (state, action) => {
        state.status = 'succeeded';
        state.items = action.payload;
      })
      .addCase(fetchLoginActivity.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload ?? 'Unable to load login activity';
      });
  }
});

export default loginActivitySlice.reducer;
