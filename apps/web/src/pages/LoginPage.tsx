import { Alert, Box, Button, Card, CardContent, Link, Stack, TextField, Typography } from '@mui/material';
import { useForm } from 'react-hook-form';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { login } from '../features/auth/authSlice';
import { useAppDispatch, useAppSelector } from '../hooks';

interface LoginForm {
  usernameOrEmail: string;
  password: string;
}

export function LoginPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const auth = useAppSelector((state) => state.auth);
  const { register, handleSubmit, formState: { errors } } = useForm<LoginForm>({
    defaultValues: {
      usernameOrEmail: 'demo.user',
      password: 'NovaBankDemo!2026'
    }
  });

  const onSubmit = async (values: LoginForm) => {
    const result = await dispatch(login(values));
    if (login.fulfilled.match(result)) {
      navigate('/dashboard');
    }
  };

  return (
    <Box className="auth-shell">
      <Card className="auth-card" elevation={0}>
        <CardContent>
          <Typography component="p" className="brand-kicker">NovaBank</Typography>
          <Typography component="h1" variant="h4" fontWeight={800} gutterBottom>
            Secure sign in
          </Typography>
          <Typography color="text.secondary" marginBottom={3}>
            Access your mock banking workspace. Demo credentials are pre-filled for local development.
          </Typography>

          {auth.error && <Alert severity="error" sx={{ mb: 2 }}>{auth.error}</Alert>}

          <Stack component="form" spacing={2.5} onSubmit={handleSubmit(onSubmit)} noValidate>
            <TextField
              label="Username or email"
              autoComplete="username"
              error={Boolean(errors.usernameOrEmail)}
              helperText={errors.usernameOrEmail?.message}
              {...register('usernameOrEmail', { required: 'Username or email is required' })}
            />
            <TextField
              label="Password"
              type="password"
              autoComplete="current-password"
              error={Boolean(errors.password)}
              helperText={errors.password?.message}
              {...register('password', { required: 'Password is required' })}
            />
            <Button type="submit" size="large" variant="contained" disabled={auth.status === 'loading'}>
              {auth.status === 'loading' ? 'Signing in...' : 'Sign in'}
            </Button>
          </Stack>

          <Stack direction="row" justifyContent="space-between" marginTop={3}>
            <Link component={RouterLink} to="/forgot-password">Forgot password?</Link>
            <Link component={RouterLink} to="/mfa">Use mock MFA</Link>
          </Stack>
          <Typography marginTop={2}>
            New to NovaBank? <Link component={RouterLink} to="/register">Create a demo account</Link>
          </Typography>
        </CardContent>
      </Card>
      <Box className="auth-aside">
        <Typography variant="h3" component="p" fontWeight={900}>
          Banking workflows without real banking risk.
        </Typography>
        <Typography>
          NovaBank uses fictional data and simulated money movement so you can study secure full-stack banking patterns.
        </Typography>
      </Box>
    </Box>
  );
}
