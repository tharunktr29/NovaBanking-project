import { Box, Button, Card, CardContent, Stack, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <Box className="center-shell">
      <Card className="narrow-card" elevation={0}>
        <CardContent>
          <Stack spacing={2}>
            <Typography className="brand-kicker">NovaBank</Typography>
            <Typography component="h1" variant="h4" fontWeight={800}>Page not found</Typography>
            <Typography color="text.secondary">
              The route does not exist in this Phase 1 frontend.
            </Typography>
            <Button component={RouterLink} to="/dashboard" variant="contained">Go to dashboard</Button>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}
