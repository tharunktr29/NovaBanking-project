import { Alert, Box, Button, Card, CardContent, Stack, Typography } from '@mui/material';
import { useEffect, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { logoutLocal, logoutServer } from '../features/auth/authSlice';
import { useAppDispatch } from '../hooks';

export function LogoutPage() {
  const dispatch = useAppDispatch();
  const [warning, setWarning] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    dispatch(logoutServer()).then((result) => {
      if (isMounted && logoutServer.rejected.match(result)) {
        setWarning('The local session was cleared, but the backend refresh token revocation did not complete.');
        dispatch(logoutLocal());
      }
    });

    return () => {
      isMounted = false;
    };
  }, [dispatch]);

  return (
    <Box className="center-shell">
      <Card className="narrow-card" elevation={0}>
        <CardContent>
          <Stack spacing={2}>
            <Typography component="h1" variant="h4" fontWeight={800}>You are signed out</Typography>
            {warning && <Alert severity="warning">{warning}</Alert>}
            <Typography color="text.secondary">
              This cleared the local demo session and requested backend refresh-token revocation when a token was available.
            </Typography>
            <Button component={RouterLink} to="/login" variant="contained">Return to sign in</Button>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}
