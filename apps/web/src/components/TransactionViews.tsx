import DownloadIcon from '@mui/icons-material/Download';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  LinearProgress,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  MenuItem,
  Pagination,
  Stack,
  TextField,
  Typography
} from '@mui/material';
import { useEffect, useMemo, useState } from 'react';
import type { Account } from '../features/accounts/accountsSlice';
import {
  clearFilters,
  clearSelectedTransaction,
  exportTransactionsCsv,
  fetchRecentTransactions,
  fetchTransactionDetail,
  fetchTransactions,
  setFilters,
  setSorting
} from '../features/transactions/transactionsSlice';
import type { BankTransaction, TransactionCategory } from '../features/transactions/transactionsSlice';
import { useAppDispatch, useAppSelector } from '../hooks';

const categories: TransactionCategory[] = [
  'GROCERIES',
  'DINING',
  'GAS',
  'UTILITIES',
  'ENTERTAINMENT',
  'SHOPPING',
  'TRAVEL',
  'HEALTHCARE',
  'INCOME',
  'TRANSFER',
  'FEES',
  'OTHER'
];

const sortOptions = [
  { value: 'authorizedAt,desc', label: 'Newest authorized' },
  { value: 'authorizedAt,asc', label: 'Oldest authorized' },
  { value: 'postedAt,desc', label: 'Newest posted' },
  { value: 'amount,desc', label: 'Amount high to low' },
  { value: 'amount,asc', label: 'Amount low to high' },
  { value: 'merchant,asc', label: 'Merchant A-Z' },
  { value: 'status,asc', label: 'Status' }
];

function formatMoney(value: string | number | undefined, currency = 'USD', direction?: string) {
  const amount = Number(value ?? 0);
  const formatted = new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(amount);
  if (direction === 'DEBIT') {
    return `− ${formatted}`;
  }
  if (direction === 'CREDIT') {
    return `+ ${formatted}`;
  }
  return formatted;
}

function formatDate(value?: string | null) {
  if (!value) {
    return 'Not available';
  }
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
}

function accountLabel(account?: Account) {
  if (!account) {
    return 'Unknown account';
  }
  return `${account.nickname} ${account.maskedAccountNumber}`;
}

function merchantOrDescription(transaction: BankTransaction) {
  return transaction.merchant?.name ?? transaction.description;
}

function TransactionAmount({ transaction }: { transaction: BankTransaction }) {
  const color = transaction.direction === 'DEBIT' ? 'error.main' : 'success.main';
  const label = transaction.direction === 'DEBIT' ? 'Debit' : 'Credit';
  return (
    <Typography fontWeight={900} color={color} aria-label={`${label} ${transaction.amount} ${transaction.currency}`}>
      {formatMoney(transaction.amount, transaction.currency, transaction.direction)}
    </Typography>
  );
}

export function RecentTransactionsCard({ accounts, onViewAll }: { accounts: Account[]; onViewAll: () => void }) {
  const dispatch = useAppDispatch();
  const { recentTransactions, recentLoading, error } = useAppSelector((state) => state.transactions);
  const accessToken = useAppSelector((state) => state.auth.accessToken);
  const accountsById = useMemo(() => Object.fromEntries(accounts.map((account) => [account.id, account])), [accounts]);

  useEffect(() => {
    if (accessToken) {
      dispatch(fetchRecentTransactions());
    }
  }, [accessToken, dispatch]);

  return (
    <Card elevation={0} className="summary-card">
      <CardContent>
        <Stack spacing={2}>
          <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={1}>
            <Box>
              <Typography variant="h6" fontWeight={900}>Recent transactions</Typography>
              <Typography color="text.secondary">Latest posted and pending activity from transaction-service.</Typography>
            </Box>
            <Button onClick={onViewAll} variant="outlined">View all transactions</Button>
          </Stack>
          {recentLoading && <LinearProgress />}
          {error && <Alert severity="error">{error}</Alert>}
          {!recentLoading && recentTransactions.length === 0 && <Alert severity="info">No transactions found.</Alert>}
          <List>
            {recentTransactions.map((transaction) => (
              <ListItem key={transaction.transactionId} divider secondaryAction={<TransactionAmount transaction={transaction} />}>
                <ListItemText
                  primary={`${merchantOrDescription(transaction)} · ${transaction.status}`}
                  secondary={`${accountLabel(accountsById[transaction.accountId])} · ${transaction.direction} · ${formatDate(transaction.authorizedAt)}`}
                />
              </ListItem>
            ))}
          </List>
        </Stack>
      </CardContent>
    </Card>
  );
}

export function TransactionsPanel({ accounts }: { accounts: Account[] }) {
  const dispatch = useAppDispatch();
  const state = useAppSelector((root) => root.transactions);
  const [searchText, setSearchText] = useState(state.activeFilters.search);
  const accountsById = useMemo(() => Object.fromEntries(accounts.map((account) => [account.id, account])), [accounts]);

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      dispatch(setFilters({ search: searchText }));
    }, 400);
    return () => window.clearTimeout(timeout);
  }, [dispatch, searchText]);

  useEffect(() => {
    const promise = dispatch(fetchTransactions());
    return () => promise.abort();
  }, [dispatch, state.activeFilters, state.pagination.page, state.pagination.size, state.sorting]);

  const updateFilter = (key: keyof typeof state.activeFilters, value: string) => {
    dispatch(setFilters({ [key]: value }));
  };

  const openDetail = (transactionId: string) => {
    dispatch(fetchTransactionDetail(transactionId));
  };

  const clear = () => {
    setSearchText('');
    dispatch(clearFilters());
  };

  return (
    <Stack spacing={3}>
      <Card elevation={0} className="summary-card">
        <CardContent>
          <Stack spacing={2}>
            <Stack direction={{ xs: 'column', md: 'row' }} justifyContent="space-between" gap={2}>
              <Box>
                <Typography variant="h6" fontWeight={900}>Transactions</Typography>
                <Typography color="text.secondary">Search, filter, sort, paginate, inspect details, and export CSV.</Typography>
              </Box>
              <Button
                startIcon={<DownloadIcon />}
                variant="outlined"
                disabled={state.exportState === 'loading'}
                onClick={() => dispatch(exportTransactionsCsv())}
              >
                Export CSV
              </Button>
            </Stack>

            <Grid container spacing={2}>
              <Grid item xs={12} md={4}>
                <TextField select label="Account" value={state.activeFilters.accountId} onChange={(e) => updateFilter('accountId', e.target.value)} fullWidth>
                  <MenuItem value="">All accounts</MenuItem>
                  {accounts.map((account) => (
                    <MenuItem key={account.id} value={account.id}>{accountLabel(account)}</MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6} md={2}>
                <TextField select label="Status" value={state.activeFilters.status} onChange={(e) => updateFilter('status', e.target.value)} fullWidth>
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="POSTED">Posted</MenuItem>
                  <MenuItem value="PENDING">Pending</MenuItem>
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6} md={2}>
                <TextField select label="Direction" value={state.activeFilters.direction} onChange={(e) => updateFilter('direction', e.target.value)} fullWidth>
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="DEBIT">Debit</MenuItem>
                  <MenuItem value="CREDIT">Credit</MenuItem>
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6} md={2}>
                <TextField select label="Category" value={state.activeFilters.category} onChange={(e) => updateFilter('category', e.target.value)} fullWidth>
                  <MenuItem value="">All</MenuItem>
                  {categories.map((category) => <MenuItem key={category} value={category}>{category}</MenuItem>)}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6} md={2}>
                <TextField select label="Sort" value={state.sorting} onChange={(e) => dispatch(setSorting(e.target.value))} fullWidth>
                  {sortOptions.map((option) => <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>)}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <TextField label="Date from" type="date" value={state.activeFilters.dateFrom} onChange={(e) => updateFilter('dateFrom', e.target.value)} InputLabelProps={{ shrink: true }} fullWidth />
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <TextField label="Date to" type="date" value={state.activeFilters.dateTo} onChange={(e) => updateFilter('dateTo', e.target.value)} InputLabelProps={{ shrink: true }} fullWidth />
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <TextField label="Min amount" type="number" value={state.activeFilters.minAmount} onChange={(e) => updateFilter('minAmount', e.target.value)} fullWidth />
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <TextField label="Max amount" type="number" value={state.activeFilters.maxAmount} onChange={(e) => updateFilter('maxAmount', e.target.value)} fullWidth />
              </Grid>
              <Grid item xs={12} md={6}>
                <TextField label="Merchant name" value={state.activeFilters.merchant} onChange={(e) => updateFilter('merchant', e.target.value)} fullWidth />
              </Grid>
              <Grid item xs={12} md={6}>
                <TextField label="Search description or merchant" value={searchText} onChange={(e) => setSearchText(e.target.value)} fullWidth />
              </Grid>
            </Grid>
            <Stack direction="row" gap={1}>
              <Button onClick={clear}>Clear filters</Button>
              <Button onClick={() => dispatch(fetchTransactions())}>Retry</Button>
            </Stack>
          </Stack>
        </CardContent>
      </Card>

      {state.loading && <LinearProgress />}
      {state.error && <Alert severity="error" action={<Button color="inherit" onClick={() => dispatch(fetchTransactions())}>Retry</Button>}>{state.error}</Alert>}
      {state.exportError && <Alert severity="error">{state.exportError}</Alert>}

      <Card elevation={0} className="summary-card">
        <CardContent>
          {!state.loading && state.transactions.length === 0 && <Alert severity="info">No transactions match the current filters.</Alert>}
          <List>
            {state.transactions.map((transaction) => (
              <ListItemButton key={transaction.transactionId} divider onClick={() => openDetail(transaction.transactionId)}>
                <ListItemText
                  primary={
                    <Stack direction={{ xs: 'column', sm: 'row' }} gap={1} alignItems={{ xs: 'flex-start', sm: 'center' }}>
                      <Typography fontWeight={900}>{merchantOrDescription(transaction)}</Typography>
                      <Chip size="small" label={transaction.status} color={transaction.status === 'PENDING' ? 'warning' : 'success'} />
                      <Chip size="small" label={transaction.direction} variant="outlined" />
                    </Stack>
                  }
                  secondary={`${transaction.category} · ${accountLabel(accountsById[transaction.accountId])} · ${formatDate(transaction.authorizedAt)}`}
                />
                <TransactionAmount transaction={transaction} />
              </ListItemButton>
            ))}
          </List>
          {state.pagination.totalPages > 1 && (
            <Stack alignItems="center" mt={2}>
              <Pagination
                count={state.pagination.totalPages}
                page={state.pagination.page + 1}
                onChange={(_, page) => dispatch(fetchTransactions({ page: page - 1 }))}
              />
            </Stack>
          )}
          <Typography color="text.secondary" mt={2}>
            Page {state.pagination.page + 1} of {Math.max(1, state.pagination.totalPages)} · {state.pagination.totalElements} transactions
          </Typography>
        </CardContent>
      </Card>

      <TransactionDetailsDialog accounts={accountsById} />
    </Stack>
  );
}

function TransactionDetailsDialog({ accounts }: { accounts: Record<string, Account> }) {
  const dispatch = useAppDispatch();
  const { selected, detailLoading, detailError } = useAppSelector((state) => state.transactions);
  const account = selected ? accounts[selected.accountId] : undefined;

  return (
    <Dialog open={Boolean(selected) || detailLoading || Boolean(detailError)} onClose={() => dispatch(clearSelectedTransaction())} maxWidth="sm" fullWidth>
      <DialogTitle>Transaction details</DialogTitle>
      <DialogContent>
        {detailLoading && <LinearProgress />}
        {detailError && <Alert severity="error">{detailError}</Alert>}
        {selected && (
          <Stack spacing={1.5} mt={1}>
            <Typography variant="h6" fontWeight={900}>{merchantOrDescription(selected)}</Typography>
            <Typography color="text.secondary">{selected.description}</Typography>
            <Detail label="Merchant" value={selected.merchant?.name ?? 'Not available'} />
            <Detail label="Category" value={selected.category} />
            <Detail label="Type" value={selected.type} />
            <Detail label="Status" value={selected.status} />
            <Detail label="Direction" value={selected.direction} />
            <Detail label="Amount" value={formatMoney(selected.amount, selected.currency, selected.direction)} />
            <Detail label="Currency" value={selected.currency} />
            <Detail label="Authorized date" value={formatDate(selected.authorizedAt)} />
            <Detail label="Posted date" value={formatDate(selected.postedAt)} />
            <Detail label="Account" value={accountLabel(account)} />
          </Stack>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={() => dispatch(clearSelectedTransaction())}>Close</Button>
      </DialogActions>
    </Dialog>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <Box>
      <Typography variant="caption" color="text.secondary">{label}</Typography>
      <Typography>{value}</Typography>
    </Box>
  );
}
