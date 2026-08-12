import { Alert, Box, Button, Card, CardContent, Link, Stack, TextField, Typography } from '@mui/material';
import { useForm } from 'react-hook-form';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { registerAccount } from '../features/auth/authSlice';
import { useAppDispatch, useAppSelector } from '../hooks';

interface RegisterForm {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
}

const passwordRules = {
  required: 'Password is required',
  minLength: { value: 12, message: 'Use at least 12 characters' },
  pattern: {
    value: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).+$/,
    message: 'Include uppercase, lowercase, number, and symbol'
  }
};

export function RegisterPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const auth = useAppSelector((state) => state.auth);
  const { register, handleSubmit, watch, formState: { errors } } = useForm<RegisterForm>();

  const onSubmit = async ({ confirmPassword: _confirmPassword, ...values }: RegisterForm) => {
    const result = await dispatch(registerAccount(values));
    if (registerAccount.fulfilled.match(result)) {
      navigate('/dashboard');
    }
  };

  return (
    <Box className="auth-shell">
      <Card className="auth-card" elevation={0}>
        <CardContent>
          <Typography component="p" className="brand-kicker">NovaBank</Typography>
          <Typography component="h1" variant="h4" fontWeight={800} gutterBottom>
            Create demo account
          </Typography>
          <Typography color="text.secondary" marginBottom={3}>
            Register a fictional customer profile for the local training environment.
          </Typography>

          {auth.error && <Alert severity="error" sx={{ mb: 2 }}>{auth.error}</Alert>}

          <Stack component="form" spacing={2.5} onSubmit={handleSubmit(onSubmit)} noValidate>
            <TextField
              label="Username"
              autoComplete="username"
              error={Boolean(errors.username)}
              helperText={errors.username?.message}
              {...register('username', {
                required: 'Username is required',
                minLength: { value: 4, message: 'Use at least 4 characters' },
                maxLength: { value: 80, message: 'Use 80 characters or fewer' }
              })}
            />
            <TextField
              label="Email"
              type="email"
              autoComplete="email"
              error={Boolean(errors.email)}
              helperText={errors.email?.message}
              {...register('email', {
                required: 'Email is required',
                maxLength: { value: 160, message: 'Use 160 characters or fewer' },
                pattern: { value: /\S+@\S+\.\S+/, message: 'Enter a valid email address' }
              })}
            />
            <TextField
              label="Password"
              type="password"
              autoComplete="new-password"
              error={Boolean(errors.password)}
              helperText={errors.password?.message}
              {...register('password', passwordRules)}
            />
            <TextField
              label="Confirm password"
              type="password"
              autoComplete="new-password"
              error={Boolean(errors.confirmPassword)}
              helperText={errors.confirmPassword?.message}
              {...register('confirmPassword', {
                required: 'Confirm your password',
                validate: (value) => value === watch('password') || 'Passwords must match'
              })}
            />
            <Button type="submit" size="large" variant="contained" disabled={auth.status === 'loading'}>
              {auth.status === 'loading' ? 'Creating account...' : 'Create account'}
            </Button>
          </Stack>

          <Typography marginTop={3}>
            Already registered? <Link component={RouterLink} to="/login">Sign in</Link>
          </Typography>
        </CardContent>
      </Card>
      <Box className="auth-aside">
        <Typography variant="h3" component="p" fontWeight={900}>
          Build banking features without handling real money.
        </Typography>
        <Typography>
          This frontend talks to the auth, customer, and account services through the gateway. Card and payment workflows remain intentionally unavailable until later phases.
        </Typography>
      </Box>
    </Box>
  );
}
