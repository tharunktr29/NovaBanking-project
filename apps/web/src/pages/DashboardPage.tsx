import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import CreditCardIcon from '@mui/icons-material/CreditCard';
import LockIcon from '@mui/icons-material/Lock';
import PaymentsIcon from '@mui/icons-material/Payments';
import NotificationsIcon from '@mui/icons-material/Notifications';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import SendIcon from '@mui/icons-material/Send';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Divider,
  FormControlLabel,
  Grid,
  LinearProgress,
  List,
  ListItem,
  ListItemText,
  Stack,
  Switch,
  Tab,
  Tabs,
  TextField,
  Typography
} from '@mui/material';
import type { ReactNode } from 'react';
import { useEffect, useMemo, useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { RecentTransactionsCard, TransactionsPanel } from '../components/TransactionViews';
import { CardsPanel } from '../components/CardsPanel';
import { PaymentsPanel } from '../components/PaymentsPanel';
import { PhaseSixPanel } from '../components/PhaseSixPanel';
import type { Account } from '../features/accounts/accountsSlice';
import { fetchAccounts, updateAccountNickname } from '../features/accounts/accountsSlice';
import { changePassword, fetchCurrentUser, refreshSession } from '../features/auth/authSlice';
import {
  fetchCustomerPreferences,
  fetchCustomerProfile,
  updateCustomerPreferences,
  updateCustomerProfile
} from '../features/customer/customerSlice';
import { fetchLoginActivity } from '../features/loginActivity/loginActivitySlice';
import { useAppDispatch, useAppSelector } from '../hooks';

function TabPanel({ active, value, children }: { active: string; value: string; children: ReactNode }) {
  return active === value ? <Box sx={{ mt: 3 }}>{children}</Box> : null;
}

function formatMoney(value: string | number | undefined, currency = 'USD') {
  const amount = Number(value ?? 0);
  return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(amount);
}

function formatDateTime(value?: string) {
  if (!value) {
    return 'Not available';
  }
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
}

function AccountCard({ account }: { account: Account }) {
  const pendingTotal = Number(account.balance?.pendingDebitAmount ?? 0) + Number(account.balance?.pendingCreditAmount ?? 0);
  return (
    <Card elevation={0} className="summary-card">
      <CardContent>
        <Stack spacing={2}>
          <Stack direction="row" justifyContent="space-between" gap={2}>
            <Box>
              <Typography color="text.secondary">{account.accountType}</Typography>
              <Typography variant="h5" fontWeight={900}>{account.nickname}</Typography>
              <Typography color="text.secondary">{account.maskedAccountNumber}</Typography>
            </Box>
            <Chip label={account.status} color={account.status === 'ACTIVE' ? 'success' : 'warning'} size="small" />
          </Stack>
          <Divider />
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <Typography color="text.secondary">Current balance</Typography>
              <Typography variant="h5" fontWeight={900}>{formatMoney(account.balance?.currentBalance, account.currency)}</Typography>
            </Grid>
            <Grid item xs={12} sm={6}>
              <Typography color="text.secondary">Available balance</Typography>
              <Typography variant="h5" fontWeight={900}>{formatMoney(account.balance?.availableBalance, account.currency)}</Typography>
            </Grid>
          </Grid>
          <Typography color="text.secondary">
            Pending: {formatMoney(pendingTotal, account.currency)} · As of {formatDateTime(account.balance?.asOf)}
          </Typography>
        </Stack>
      </CardContent>
    </Card>
  );
}

function ComingSoon({ title, copy }: { title: string; copy: string }) {
  return (
    <Card elevation={0} className="summary-card">
      <CardContent>
        <Stack spacing={1}>
          <Typography variant="h6" fontWeight={900}>{title}</Typography>
          <Typography color="text.secondary">{copy}</Typography>
          <Chip label="Coming in Phase 3+" sx={{ alignSelf: 'flex-start' }} />
        </Stack>
      </CardContent>
    </Card>
  );
}

export function DashboardPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const auth = useAppSelector((state) => state.auth);
  const customer = useAppSelector((state) => state.customer);
  const accounts = useAppSelector((state) => state.accounts);
  const loginActivity = useAppSelector((state) => state.loginActivity);
  const [activeTab, setActiveTab] = useState('overview');
  const [editingNickname, setEditingNickname] = useState<Record<string, string>>({});
  const [profileDraft, setProfileDraft] = useState(customer.profile);
  const [passwordDraft, setPasswordDraft] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [passwordMessage, setPasswordMessage] = useState<string | null>(null);

  useEffect(() => {
    if (auth.accessToken) {
      dispatch(fetchCurrentUser());
      dispatch(fetchCustomerProfile());
      dispatch(fetchCustomerPreferences());
      dispatch(fetchAccounts());
      dispatch(fetchLoginActivity());
    }
  }, [auth.accessToken, dispatch]);

  useEffect(() => {
    setProfileDraft(customer.profile);
  }, [customer.profile]);

  const greeting = customer.profile ? `${customer.profile.firstName} ${customer.profile.lastName}` : auth.username ?? 'customer';
  const checking = useMemo(() => accounts.accounts.find((account) => account.accountType === 'CHECKING'), [accounts.accounts]);
  const savings = useMemo(() => accounts.accounts.find((account) => account.accountType === 'SAVINGS'), [accounts.accounts]);

  const retryAll = () => {
    dispatch(fetchCustomerProfile());
    dispatch(fetchCustomerPreferences());
    dispatch(fetchAccounts());
    dispatch(fetchLoginActivity());
  };

  const saveProfile = () => {
    if (!profileDraft) {
      return;
    }
    dispatch(updateCustomerProfile({
      firstName: profileDraft.firstName,
      lastName: profileDraft.lastName,
      email: profileDraft.email,
      phone: profileDraft.phone ?? '',
      addressLine1: profileDraft.addressLine1,
      addressLine2: profileDraft.addressLine2 ?? '',
      city: profileDraft.city,
      state: profileDraft.state,
      postalCode: profileDraft.postalCode,
      country: profileDraft.country
    }));
  };

  const savePassword = async () => {
    setPasswordMessage(null);
    const result = await dispatch(changePassword(passwordDraft));
    if (changePassword.fulfilled.match(result)) {
      setPasswordMessage('Password changed. Sign in again with the new password.');
      navigate('/login');
    }
  };

  return (
    <Box className="dashboard-shell">
      <Stack direction={{ xs: 'column', md: 'row' }} justifyContent="space-between" gap={2} marginBottom={4}>
        <Box>
          <Typography className="brand-kicker">NovaBank</Typography>
          <Typography component="h1" variant="h4" fontWeight={900}>Welcome, {greeting}</Typography>
          <Typography color="text.secondary">
            KYC {customer.profile?.kycStatus ?? 'loading'} · Session expires {formatDateTime(auth.accessTokenExpiresAt ?? undefined)}
          </Typography>
        </Box>
        <Stack direction="row" gap={1} alignItems="center">
          <Button onClick={() => dispatch(refreshSession())} variant="outlined">Refresh session</Button>
          <Stack direction="row" spacing={1}>{['FRAUD_ANALYST','OPERATIONS_ADMIN','SUPPORT_AGENT'].includes(auth.role ?? '') && <Button component={RouterLink} to="/risk-operations" variant="outlined">Risk Operations</Button>}<Button component={RouterLink} to="/logout" variant="contained">Log out</Button></Stack>
        </Stack>
      </Stack>

      {(customer.status === 'loading' || accounts.status === 'loading') && <LinearProgress sx={{ mb: 2 }} />}
      {(customer.error || accounts.error || auth.error) && (
        <Alert severity="error" sx={{ mb: 2 }} action={<Button color="inherit" onClick={retryAll}>Retry</Button>}>
          {customer.error ?? accounts.error ?? auth.error}
        </Alert>
      )}
      {(customer.message || accounts.message || passwordMessage) && (
        <Alert severity="success" sx={{ mb: 2 }}>{customer.message ?? accounts.message ?? passwordMessage}</Alert>
      )}

      <Card elevation={0} className="dashboard-nav-card">
        <Tabs value={activeTab} onChange={(_, value) => setActiveTab(value)} variant="scrollable" scrollButtons="auto">
          <Tab icon={<TrendingUpIcon />} iconPosition="start" label="Overview" value="overview" />
          <Tab icon={<AccountBalanceIcon />} iconPosition="start" label="Accounts" value="accounts" />
          <Tab icon={<ReceiptLongIcon />} iconPosition="start" label="Transactions" value="transactions" />
          <Tab icon={<SendIcon />} iconPosition="start" label="Transfers" value="transfers" />
          <Tab icon={<CreditCardIcon />} iconPosition="start" label="Cards" value="cards" />
          <Tab icon={<PaymentsIcon />} iconPosition="start" label="Payments" value="payments" />
          <Tab icon={<NotificationsIcon />} iconPosition="start" label="Statements & Disputes" value="engagement" />
          <Tab icon={<LockIcon />} iconPosition="start" label="Security" value="security" />
        </Tabs>
      </Card>

      <TabPanel active={activeTab} value="overview">
        {accounts.status === 'succeeded' && accounts.accounts.length === 0 && (
          <Alert severity="info">No checking or savings accounts exist for this customer yet.</Alert>
        )}
        <Grid container spacing={3}>
          {checking && <Grid item xs={12} md={6}><AccountCard account={checking} /></Grid>}
          {savings && <Grid item xs={12} md={6}><AccountCard account={savings} /></Grid>}
          <Grid item xs={12}>
            <RecentTransactionsCard accounts={accounts.accounts} onViewAll={() => setActiveTab('transactions')} />
          </Grid>
        </Grid>
      </TabPanel>

      <TabPanel active={activeTab} value="accounts">
        <Grid container spacing={3}>
          {accounts.accounts.map((account) => {
            const draft = editingNickname[account.id] ?? account.nickname;
            const isEditing = Object.prototype.hasOwnProperty.call(editingNickname, account.id);
            return (
              <Grid item xs={12} md={6} key={account.id}>
                <Card elevation={0} className="summary-card">
                  <CardContent>
                    <Stack spacing={2}>
                      <AccountCard account={account} />
                      {isEditing ? (
                        <Stack direction={{ xs: 'column', sm: 'row' }} gap={1}>
                          <TextField
                            label="Account nickname"
                            value={draft}
                            onChange={(event) => setEditingNickname({ ...editingNickname, [account.id]: event.target.value })}
                            error={draft.trim().length < 2}
                            helperText={draft.trim().length < 2 ? 'Use at least 2 characters' : ' '}
                            fullWidth
                          />
                          <Button
                            variant="contained"
                            disabled={draft.trim().length < 2 || accounts.savingAccountId === account.id}
                            onClick={() => dispatch(updateAccountNickname({ accountId: account.id, nickname: draft }))}
                          >
                            Save
                          </Button>
                          <Button onClick={() => {
                            const next = { ...editingNickname };
                            delete next[account.id];
                            setEditingNickname(next);
                          }}>
                            Cancel
                          </Button>
                        </Stack>
                      ) : (
                        <Button
                          variant="outlined"
                          disabled={account.status === 'CLOSED'}
                          onClick={() => setEditingNickname({ ...editingNickname, [account.id]: account.nickname })}
                        >
                          Edit nickname
                        </Button>
                      )}
                    </Stack>
                  </CardContent>
                </Card>
              </Grid>
            );
          })}
        </Grid>
      </TabPanel>

      <TabPanel active={activeTab} value="transactions">
        <TransactionsPanel accounts={accounts.accounts} />
      </TabPanel>

      <TabPanel active={activeTab} value="transfers">
        <PaymentsPanel initialMode="INTERNAL" />
      </TabPanel>

      <TabPanel active={activeTab} value="cards">
        <CardsPanel />
      </TabPanel>

      <TabPanel active={activeTab} value="payments">
        <PaymentsPanel initialMode="CARD" />
      </TabPanel>

      <TabPanel active={activeTab} value="engagement">
        <PhaseSixPanel accounts={accounts.accounts} />
      </TabPanel>

      <TabPanel active={activeTab} value="security">
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <Card elevation={0} className="summary-card">
              <CardContent>
                <Stack spacing={2}>
                  <Typography variant="h6" fontWeight={900}>Customer profile</Typography>
                  {profileDraft && (
                    <>
                      <Stack direction={{ xs: 'column', sm: 'row' }} gap={2}>
                        <TextField label="First name" value={profileDraft.firstName} onChange={(e) => setProfileDraft({ ...profileDraft, firstName: e.target.value })} fullWidth />
                        <TextField label="Last name" value={profileDraft.lastName} onChange={(e) => setProfileDraft({ ...profileDraft, lastName: e.target.value })} fullWidth />
                      </Stack>
                      <TextField label="Email" value={profileDraft.email} onChange={(e) => setProfileDraft({ ...profileDraft, email: e.target.value })} />
                      <TextField label="Phone" value={profileDraft.phone ?? ''} onChange={(e) => setProfileDraft({ ...profileDraft, phone: e.target.value })} />
                      <TextField label="Address line 1" value={profileDraft.addressLine1} onChange={(e) => setProfileDraft({ ...profileDraft, addressLine1: e.target.value })} />
                      <TextField label="Address line 2" value={profileDraft.addressLine2 ?? ''} onChange={(e) => setProfileDraft({ ...profileDraft, addressLine2: e.target.value })} />
                      <Stack direction={{ xs: 'column', sm: 'row' }} gap={2}>
                        <TextField label="City" value={profileDraft.city} onChange={(e) => setProfileDraft({ ...profileDraft, city: e.target.value })} fullWidth />
                        <TextField label="State" value={profileDraft.state} onChange={(e) => setProfileDraft({ ...profileDraft, state: e.target.value.toUpperCase() })} inputProps={{ maxLength: 2 }} />
                        <TextField label="Postal code" value={profileDraft.postalCode} onChange={(e) => setProfileDraft({ ...profileDraft, postalCode: e.target.value })} />
                      </Stack>
                      <TextField label="Country" value={profileDraft.country} onChange={(e) => setProfileDraft({ ...profileDraft, country: e.target.value })} />
                      <Typography color="text.secondary">KYC status is read-only: <strong>{profileDraft.kycStatus}</strong></Typography>
                      <Button variant="contained" disabled={customer.saving} onClick={saveProfile}>Save profile</Button>
                    </>
                  )}
                </Stack>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={6}>
            <Stack spacing={3}>
              <Card elevation={0} className="summary-card">
                <CardContent>
                  <Stack spacing={1}>
                    <Typography variant="h6" fontWeight={900}>Preferences</Typography>
                    {customer.preferences && (
                      <>
                        {(['emailAlerts', 'smsAlerts', 'pushAlerts', 'securityAlerts', 'paymentAlerts', 'lowBalanceAlerts', 'paperlessStatements'] as const).map((key) => (
                          <FormControlLabel
                            key={key}
                            control={<Switch checked={customer.preferences?.[key] ?? false} onChange={(e) => dispatch(updateCustomerPreferences({ ...customer.preferences!, [key]: e.target.checked }))} />}
                            label={key.replace(/([A-Z])/g, ' $1').replace(/^./, (value) => value.toUpperCase())}
                          />
                        ))}
                      </>
                    )}
                  </Stack>
                </CardContent>
              </Card>

              <Card elevation={0} className="summary-card">
                <CardContent>
                  <Stack spacing={2}>
                    <Typography variant="h6" fontWeight={900}>Change password</Typography>
                    <TextField label="Current password" type="password" value={passwordDraft.currentPassword} onChange={(e) => setPasswordDraft({ ...passwordDraft, currentPassword: e.target.value })} />
                    <TextField label="New password" type="password" value={passwordDraft.newPassword} onChange={(e) => setPasswordDraft({ ...passwordDraft, newPassword: e.target.value })} />
                    <TextField label="Confirm new password" type="password" value={passwordDraft.confirmPassword} onChange={(e) => setPasswordDraft({ ...passwordDraft, confirmPassword: e.target.value })} />
                    <Button variant="outlined" onClick={savePassword}>Change password</Button>
                  </Stack>
                </CardContent>
              </Card>
            </Stack>
          </Grid>

          <Grid item xs={12}>
            <Card elevation={0} className="summary-card">
              <CardContent>
                <Typography variant="h6" fontWeight={900} gutterBottom>Login activity</Typography>
                {loginActivity.status === 'loading' && <LinearProgress />}
                {loginActivity.error && <Alert severity="error">{loginActivity.error}</Alert>}
                <List>
                  {loginActivity.items.map((item) => (
                    <ListItem key={`${item.timestamp}-${item.eventType}`} divider>
                      <ListItemText
                        primary={`${item.eventType} · ${item.successful ? 'Successful' : 'Failed'}`}
                        secondary={`${formatDateTime(item.timestamp)} · ${item.maskedIpAddress} · ${item.deviceSummary}`}
                      />
                    </ListItem>
                  ))}
                </List>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </TabPanel>
    </Box>
  );
}
