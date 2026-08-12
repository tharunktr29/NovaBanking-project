import { Alert, Box, Button, Card, CardContent, Link, Stack, TextField, Typography } from '@mui/material';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link as RouterLink } from 'react-router-dom';
import { apiClient } from '../api/client';

interface ForgotPasswordForm {
  email: string;
}

export function ForgotPasswordPage() {
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<ForgotPasswordForm>();

  const onSubmit = async (values: ForgotPasswordForm) => {
    setError(null);
    setMessage(null);
    try {
      const response = await apiClient.post<{ message: string }>('/auth/forgot-password', values);
      setMessage(response.data.message);
    } catch (apiError: any) {
      setError(apiError.response?.data?.message ?? 'Unable to request password reset');
    }
  };

  return (
    <Box className="center-shell">
      <Card className="narrow-card" elevation={0}>
        <CardContent>
          <Typography component="h1" variant="h4" fontWeight={800}>Reset your password</Typography>
          <Typography color="text.secondary" marginBottom={3}>
            This is a simulated workflow. No real email is sent.
          </Typography>
          {message && <Alert severity="success" sx={{ mb: 2 }}>{message}</Alert>}
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <Stack component="form" spacing={2} onSubmit={handleSubmit(onSubmit)} noValidate>
            <TextField
              label="Email"
              type="email"
              error={Boolean(errors.email)}
              helperText={errors.email?.message}
              {...register('email', {
                required: 'Email is required',
                pattern: { value: /\S+@\S+\.\S+/, message: 'Enter a valid email address' }
              })}
            />
            <Button type="submit" variant="contained" disabled={isSubmitting}>
              {isSubmitting ? 'Submitting...' : 'Send reset instructions'}
            </Button>
            <Typography color="text.secondary">
              Already have a reset token? <Link component={RouterLink} to="/reset-password">Set a new password</Link>
            </Typography>
            <Button component={RouterLink} to="/login">Back to sign in</Button>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}
