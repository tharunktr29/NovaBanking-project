import { Alert, Box, Button, Card, CardContent, Stack, TextField, Typography } from '@mui/material';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link as RouterLink } from 'react-router-dom';
import { apiClient } from '../api/client';

interface MfaForm {
  challengeId: string;
  code: string;
}

export function MfaPage() {
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<MfaForm>({
    defaultValues: { challengeId: 'demo-challenge', code: '000000' }
  });

  const onSubmit = async (values: MfaForm) => {
    setError(null);
    try {
      const response = await apiClient.post<{ message: string }>('/auth/mfa/verify', values);
      setMessage(response.data.message);
    } catch (apiError: any) {
      setError(apiError.response?.data?.message ?? 'Unable to verify MFA code');
    }
  };

  return (
    <Box className="center-shell">
      <Card className="narrow-card" elevation={0}>
        <CardContent>
          <Typography component="h1" variant="h4" fontWeight={800}>Mock MFA verification</Typography>
          <Typography color="text.secondary" marginBottom={3}>
            Use code 000000 for the Phase 1 simulated MFA path.
          </Typography>
          {message && <Alert severity="success" sx={{ mb: 2 }}>{message}</Alert>}
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <Stack component="form" spacing={2} onSubmit={handleSubmit(onSubmit)} noValidate>
            <TextField label="Challenge ID" {...register('challengeId', { required: 'Challenge ID is required' })} />
            <TextField
              label="Six-digit code"
              inputProps={{ inputMode: 'numeric' }}
              error={Boolean(errors.code)}
              helperText={errors.code?.message}
              {...register('code', {
                required: 'Code is required',
                pattern: { value: /^\d{6}$/, message: 'Enter a six-digit code' }
              })}
            />
            <Button type="submit" variant="contained" disabled={isSubmitting}>
              {isSubmitting ? 'Verifying...' : 'Verify'}
            </Button>
            <Button component={RouterLink} to="/login">Back to sign in</Button>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}
