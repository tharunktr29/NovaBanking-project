import { Alert, Box, Button, Card, CardContent, FormControl, Grid, InputLabel, LinearProgress, List, ListItem, ListItemText, MenuItem, Select, Stack, TextField, Typography } from '@mui/material';
import { useEffect, useMemo, useState } from 'react';
import type { Account } from '../features/accounts/accountsSlice';
import type { BankCard } from '../features/cards/cardsSlice';
import {
  cancelPayment, createCreditCardPayment, createExternalPayment, createInternalTransfer,
  fetchPayees, fetchPaymentHistory, fetchPayments, type ExecutionType, type MoneyMovementRequest
} from '../features/payments/paymentsSlice';
import { useAppDispatch, useAppSelector } from '../hooks';

type Mode = 'INTERNAL' | 'CARD' | 'EXTERNAL';
const initialDraft = { sourceAccountId: '', destinationAccountId: '', destinationCardId: '', externalPayeeId: '', amount: '', memo: '', executionType: 'IMMEDIATE' as ExecutionType, scheduledFor: '' };
function friendly(value: string) { return value.replace(/_/g, ' '); }
function money(value: string | number | undefined, currency = 'USD') { return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(Number(value ?? 0)); }
function when(value: string | null) { return value ? new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : '—'; }

export function PaymentsPanel({ initialMode = 'INTERNAL' }: { initialMode?: Mode }) {
  const dispatch = useAppDispatch();
  const accounts = useAppSelector((state) => state.accounts.accounts).filter((item) => item.status === 'ACTIVE');
  const cards = useAppSelector((state) => state.cards.cards).filter((item) => item.cardType === 'CREDIT' && item.status === 'ACTIVE');
  const payments = useAppSelector((state) => state.payments);
  const [mode, setMode] = useState<Mode>(initialMode);
  const [draft, setDraft] = useState(initialDraft);
  const busy = payments.mutationState === 'loading';
  useEffect(() => { dispatch(fetchPayments(undefined)); dispatch(fetchPayees()); }, [dispatch]);

  const source = useMemo(() => accounts.find((item) => item.id === draft.sourceAccountId) ?? accounts[0], [accounts, draft.sourceAccountId]);
  const selectedCard = cards.find((item) => item.cardId === draft.destinationCardId);
  const amount = Number(draft.amount);
  const invalidAmount = !Number.isFinite(amount) || amount <= 0 || amount > Number(source?.balance?.availableBalance ?? 0);
  const overpayment = mode === 'CARD' && amount > Number(selectedCard?.credit?.currentBalance ?? 0);
  const sameAccount = mode === 'INTERNAL' && source?.id === draft.destinationAccountId;
  const scheduleInvalid = draft.executionType === 'SCHEDULED' && (!draft.scheduledFor || new Date(draft.scheduledFor) <= new Date());

  const submit = () => {
    if (!source || invalidAmount || overpayment || sameAccount || scheduleInvalid) return;
    const request: MoneyMovementRequest = { sourceAccountId: source.id, amount: draft.amount, currency: source.currency, memo: draft.memo || undefined, executionType: draft.executionType, scheduledFor: draft.executionType === 'SCHEDULED' ? new Date(draft.scheduledFor).toISOString() : null };
    if (!window.confirm(`Review: submit this ${friendly(mode).toLowerCase()} money movement for ${money(amount, source.currency)}?`)) return;
    if (mode === 'INTERNAL') dispatch(createInternalTransfer({ ...request, destinationAccountId: draft.destinationAccountId }));
    if (mode === 'CARD') dispatch(createCreditCardPayment({ ...request, destinationCardId: draft.destinationCardId }));
    if (mode === 'EXTERNAL') dispatch(createExternalPayment({ ...request, externalPayeeId: draft.externalPayeeId }));
  };

  return <Stack spacing={3}>
    <Alert severity="info">NovaBank is fictional. External payments are locally simulated and never contact a financial network.</Alert>
    {payments.message && <Alert severity="success">{payments.message}</Alert>}
    {payments.error && <Alert severity="error" action={<Button onClick={() => dispatch(fetchPayments(undefined))}>Retry</Button>}>{payments.error}</Alert>}
    <Grid container spacing={3}>
      <Grid item xs={12} md={5}><Card elevation={0} className="summary-card"><CardContent><Stack spacing={2}>
        <Typography variant="h6" fontWeight={900}>Move money</Typography>
        <FormControl fullWidth><InputLabel id="movement-type">Payment type</InputLabel><Select labelId="movement-type" label="Payment type" value={mode} onChange={(event) => setMode(event.target.value as Mode)}><MenuItem value="INTERNAL">Internal transfer</MenuItem><MenuItem value="CARD">Pay credit card</MenuItem><MenuItem value="EXTERNAL">External payment (simulated)</MenuItem></Select></FormControl>
        <FormControl fullWidth><InputLabel id="source-account">From account</InputLabel><Select labelId="source-account" label="From account" value={source?.id ?? ''} onChange={(event) => setDraft({ ...draft, sourceAccountId: event.target.value })}>{accounts.map((account: Account) => <MenuItem key={account.id} value={account.id}>{account.nickname} · available {money(account.balance?.availableBalance, account.currency)}</MenuItem>)}</Select></FormControl>
        {mode === 'INTERNAL' && <FormControl fullWidth><InputLabel id="destination-account">To account</InputLabel><Select labelId="destination-account" label="To account" value={draft.destinationAccountId} onChange={(event) => setDraft({ ...draft, destinationAccountId: event.target.value })}>{accounts.map((account) => <MenuItem key={account.id} value={account.id}>{account.nickname}</MenuItem>)}</Select></FormControl>}
        {mode === 'CARD' && <FormControl fullWidth><InputLabel id="destination-card">Credit card</InputLabel><Select labelId="destination-card" label="Credit card" value={draft.destinationCardId} onChange={(event) => setDraft({ ...draft, destinationCardId: event.target.value })}>{cards.map((card: BankCard) => <MenuItem key={card.cardId} value={card.cardId}>{card.maskedCardNumber} · balance {money(card.credit?.currentBalance, card.credit?.currency)}</MenuItem>)}</Select></FormControl>}
        {mode === 'EXTERNAL' && <FormControl fullWidth><InputLabel id="external-payee">Verified payee</InputLabel><Select labelId="external-payee" label="Verified payee" value={draft.externalPayeeId} onChange={(event) => setDraft({ ...draft, externalPayeeId: event.target.value })}>{payments.payees.filter((payee) => payee.status === 'VERIFIED').map((payee) => <MenuItem key={payee.id} value={payee.id}>{payee.nickname} · {payee.maskedAccountNumber}</MenuItem>)}</Select></FormControl>}
        <TextField label="Amount" type="number" value={draft.amount} onChange={(event) => setDraft({ ...draft, amount: event.target.value })} error={!!draft.amount && (invalidAmount || overpayment)} helperText={overpayment ? 'Payment cannot exceed the card balance' : invalidAmount && draft.amount ? 'Enter a positive amount within the available balance' : ' '} inputProps={{ min: 0.01, step: 0.01 }} />
        <TextField label="Memo (optional)" value={draft.memo} inputProps={{ maxLength: 160 }} onChange={(event) => setDraft({ ...draft, memo: event.target.value })} />
        <FormControl fullWidth><InputLabel id="execution-type">When</InputLabel><Select labelId="execution-type" label="When" value={draft.executionType} onChange={(event) => setDraft({ ...draft, executionType: event.target.value as ExecutionType })}><MenuItem value="IMMEDIATE">Immediately</MenuItem><MenuItem value="SCHEDULED">Schedule once</MenuItem></Select></FormControl>
        {draft.executionType === 'SCHEDULED' && <TextField label="Scheduled date and time" type="datetime-local" InputLabelProps={{ shrink: true }} value={draft.scheduledFor} onChange={(event) => setDraft({ ...draft, scheduledFor: event.target.value })} error={scheduleInvalid} helperText={scheduleInvalid ? 'Choose a future time' : 'Stored and processed in UTC'} />}
        <Button variant="contained" disabled={busy || invalidAmount || overpayment || sameAccount || scheduleInvalid || (mode === 'INTERNAL' && !draft.destinationAccountId) || (mode === 'CARD' && !draft.destinationCardId) || (mode === 'EXTERNAL' && !draft.externalPayeeId)} onClick={submit}>Review and submit</Button>
      </Stack></CardContent></Card></Grid>
      <Grid item xs={12} md={7}><Card elevation={0} className="summary-card"><CardContent><Stack spacing={2}>
        <Stack direction="row" justifyContent="space-between"><Box><Typography variant="h6" fontWeight={900}>Payment history</Typography><Typography color="text.secondary">Database-backed orders and status tracking</Typography></Box><Button onClick={() => dispatch(fetchPayments(undefined))}>Refresh</Button></Stack>
        {payments.status === 'loading' && <LinearProgress />}
        {payments.payments.length === 0 ? <Alert severity="info">No payments or transfers yet.</Alert> : <List>{payments.payments.map((payment) => <ListItem key={payment.id} divider secondaryAction={payment.status === 'SCHEDULED' ? <Button disabled={busy} onClick={() => window.confirm('Cancel this scheduled payment?') && dispatch(cancelPayment(payment.id))}>Cancel</Button> : undefined}><ListItemText onClick={() => dispatch(fetchPaymentHistory(payment.id))} sx={{ cursor: 'pointer' }} primary={`${friendly(payment.paymentType)} · ${money(payment.amount, payment.currency)} · ${friendly(payment.status)}`} secondary={`${payment.paymentReference} · scheduled ${when(payment.scheduledFor)} · completed ${when(payment.completedAt)}`} /></ListItem>)}</List>}
        {payments.history.length > 0 && <><Typography fontWeight={900}>Selected status timeline</Typography><List>{payments.history.map((item, index) => <ListItem key={`${item.occurredAt}-${index}`} divider><ListItemText primary={`${item.previousStatus ?? 'START'} → ${item.newStatus}`} secondary={`${item.reasonCode ?? ''} ${when(item.occurredAt)}`} /></ListItem>)}</List></>}
      </Stack></CardContent></Card></Grid>
    </Grid>
  </Stack>;
}
