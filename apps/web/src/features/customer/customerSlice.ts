import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { apiClient } from '../../api/client';

export interface CustomerProfile {
  id: string;
  authUserId: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  addressLine1: string;
  addressLine2: string | null;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  kycStatus: 'PENDING' | 'VERIFIED' | 'RESTRICTED';
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface CustomerPreference {
  id: string;
  customerId: string;
  emailAlerts: boolean;
  smsAlerts: boolean;
  pushAlerts: boolean;
  securityAlerts: boolean;
  paymentAlerts: boolean;
  lowBalanceAlerts: boolean;
  paperlessStatements: boolean;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export type UpdateCustomerProfilePayload = Omit<CustomerProfile, 'id' | 'authUserId' | 'kycStatus' | 'createdAt' | 'updatedAt' | 'version'>;
export type UpdateCustomerPreferencePayload = Omit<CustomerPreference, 'id' | 'customerId' | 'createdAt' | 'updatedAt' | 'version'>;

interface CustomerState {
  profile: CustomerProfile | null;
  preferences: CustomerPreference | null;
  status: 'idle' | 'loading' | 'succeeded' | 'failed';
  saving: boolean;
  message: string | null;
  error: string | null;
}

const initialState: CustomerState = {
  profile: null,
  preferences: null,
  status: 'idle',
  saving: false,
  message: null,
  error: null
};

function errorMessage(error: any, fallback: string) {
  return error.response?.data?.message ?? fallback;
}

export const fetchCustomerProfile = createAsyncThunk<CustomerProfile, void, { rejectValue: string }>(
  'customer/fetchProfile',
  async (_, thunkApi) => {
    try {
      const response = await apiClient.get<CustomerProfile>('/customers/me');
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to load customer profile'));
    }
  }
);

export const updateCustomerProfile = createAsyncThunk<CustomerProfile, UpdateCustomerProfilePayload, { rejectValue: string }>(
  'customer/updateProfile',
  async (payload, thunkApi) => {
    try {
      const response = await apiClient.patch<CustomerProfile>('/customers/me', payload);
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to update customer profile'));
    }
  }
);

export const fetchCustomerPreferences = createAsyncThunk<CustomerPreference, void, { rejectValue: string }>(
  'customer/fetchPreferences',
  async (_, thunkApi) => {
    try {
      const response = await apiClient.get<CustomerPreference>('/customers/me/preferences');
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to load customer preferences'));
    }
  }
);

export const updateCustomerPreferences = createAsyncThunk<CustomerPreference, UpdateCustomerPreferencePayload, { rejectValue: string }>(
  'customer/updatePreferences',
  async (payload, thunkApi) => {
    try {
      const response = await apiClient.patch<CustomerPreference>('/customers/me/preferences', payload);
      return response.data;
    } catch (error: any) {
      return thunkApi.rejectWithValue(errorMessage(error, 'Unable to update customer preferences'));
    }
  }
);

const customerSlice = createSlice({
  name: 'customer',
  initialState,
  reducers: {
    clearCustomerMessage(state) {
      state.message = null;
      state.error = null;
    }
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchCustomerProfile.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(fetchCustomerProfile.fulfilled, (state, action) => {
        state.status = 'succeeded';
        state.profile = action.payload;
      })
      .addCase(fetchCustomerProfile.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload ?? 'Unable to load customer profile';
      })
      .addCase(updateCustomerProfile.pending, (state) => {
        state.saving = true;
        state.message = null;
        state.error = null;
      })
      .addCase(updateCustomerProfile.fulfilled, (state, action) => {
        state.saving = false;
        state.profile = action.payload;
        state.message = 'Profile updated';
      })
      .addCase(updateCustomerProfile.rejected, (state, action) => {
        state.saving = false;
        state.error = action.payload ?? 'Unable to update customer profile';
      })
      .addCase(fetchCustomerPreferences.fulfilled, (state, action) => {
        state.preferences = action.payload;
      })
      .addCase(updateCustomerPreferences.pending, (state) => {
        state.saving = true;
        state.message = null;
        state.error = null;
      })
      .addCase(updateCustomerPreferences.fulfilled, (state, action) => {
        state.saving = false;
        state.preferences = action.payload;
        state.message = 'Preferences updated';
      })
      .addCase(updateCustomerPreferences.rejected, (state, action) => {
        state.saving = false;
        state.error = action.payload ?? 'Unable to update customer preferences';
      });
  }
});

export const { clearCustomerMessage } = customerSlice.actions;
export default customerSlice.reducer;
