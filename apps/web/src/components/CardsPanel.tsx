import {
  Alert, Box, Button, Card, CardContent, Chip, Divider, FormControl, FormControlLabel,
  Grid, InputLabel, LinearProgress, List, ListItem, ListItemButton, ListItemText,
  MenuItem, Select, Stack, Switch, TextField, Typography
} from '@mui/material';
import { useEffect, useState } from 'react';
import {
  activateCard, fetchCard, fetchCardControls, fetchCardHistory, fetchCards, lockCard,
  requestReplacement, unlockCard, updateCardControls,
  type CardControls, type ReplacementReason
} from '../features/cards/cardsSlice';
import { useAppDispatch, useAppSelector } from '../hooks';

const reasons: ReplacementReason[] = ['LOST', 'STOLEN', 'DAMAGED', 'EXPIRED', 'NAME_CHANGE', 'OTHER'];

function money(value: string | number | null | undefined, currency = 'USD') {
  return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(Number(value ?? 0));
}

function label(value: string) { return value.replace(/_/g, ' '); }

export function CardsPanel() {
  const dispatch = useAppDispatch();
  const cards = useAppSelector((state) => state.cards);
  const [controls, setControls] = useState<CardControls | null>(null);
  const [reason, setReason] = useState<ReplacementReason>('DAMAGED');
  const selected = cards.selectedCard;
  const busy = cards.mutationState === 'loading' || cards.replacementState === 'loading';

  useEffect(() => { dispatch(fetchCards(undefined)); }, [dispatch]);
  useEffect(() => { setControls(cards.cardControls); }, [cards.cardControls]);

  const selectCard = (cardId: string) => {
    dispatch(fetchCard(cardId));
    dispatch(fetchCardControls(cardId));
    dispatch(fetchCardHistory(cardId));
  };

  const confirmAction = (message: string, action: () => void) => {
    if (window.confirm(message)) action();
  };

  if (cards.loading) return <LinearProgress aria-label="Loading cards" />;
  if (cards.error) return <Alert severity="error" action={<Button onClick={() => dispatch(fetchCards(undefined))}>Retry</Button>}>{cards.error}</Alert>;
  if (cards.cards.length === 0) return <Alert severity="info">No cards are available for this customer.</Alert>;

  return (
    <Stack spacing={3}>
      {cards.message && <Alert severity="success">{cards.message}</Alert>}
      {cards.mutationError && <Alert severity="error">{cards.mutationError}</Alert>}
      <Grid container spacing={3}>
        <Grid item xs={12} md={4}>
          <Card elevation={0} className="summary-card">
            <CardContent>
              <Typography variant="h6" fontWeight={900}>Your cards</Typography>
              <List aria-label="Card list">
                {cards.cards.map((card) => (
                  <ListItem key={card.cardId} disablePadding divider>
                    <ListItemButton selected={selected?.cardId === card.cardId} onClick={() => selectCard(card.cardId)}>
                      <ListItemText
                        primary={`${card.cardType} ${card.cardNetwork} · ${card.maskedCardNumber}`}
                        secondary={`${label(card.status)} · expires ${String(card.expirationMonth).padStart(2, '0')}/${card.expirationYear}`}
                      />
                    </ListItemButton>
                  </ListItem>
                ))}
              </List>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={8}>
          {selected && <Card elevation={0} className="summary-card"><CardContent><Stack spacing={3}>
            <Stack direction="row" justifyContent="space-between" gap={2}>
              <Box>
                <Typography variant="h6" fontWeight={900}>{selected.cardType} card details</Typography>
                <Typography>{selected.maskedCardNumber}</Typography>
                <Typography color="text.secondary">{selected.cardholderName} · {selected.cardNetwork}</Typography>
              </Box>
              <Chip label={label(selected.status)} color={selected.status === 'ACTIVE' ? 'success' : selected.status === 'LOCKED' ? 'warning' : 'default'} />
            </Stack>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}><Typography color="text.secondary">Linked account</Typography><Typography fontWeight={800}>{selected.linkedAccount?.nickname ?? 'Account temporarily unavailable'}</Typography></Grid>
              <Grid item xs={12} sm={6}><Typography color="text.secondary">{selected.cardType === 'CREDIT' ? 'Available credit' : 'Available balance'}</Typography><Typography fontWeight={800}>{selected.cardType === 'CREDIT' ? money(selected.credit?.availableCredit, selected.credit?.currency) : selected.debitBalance?.unavailable ? 'Unavailable' : money(selected.debitBalance?.availableBalance, selected.debitBalance?.currency ?? 'USD')}</Typography></Grid>
              {selected.credit && <><Grid item xs={6}><Typography color="text.secondary">Current balance</Typography><Typography>{money(selected.credit.currentBalance, selected.credit.currency)}</Typography></Grid><Grid item xs={6}><Typography color="text.secondary">Minimum due</Typography><Typography>{money(selected.credit.minimumPaymentDue, selected.credit.currency)}</Typography></Grid></>}
            </Grid>
            <Stack direction={{ xs: 'column', sm: 'row' }} gap={1}>
              {selected.status === 'PENDING_ACTIVATION' && <Button variant="contained" disabled={busy} onClick={() => confirmAction('Activate this fictional card?', () => dispatch(activateCard(selected.cardId)))}>Activate card</Button>}
              {selected.status === 'ACTIVE' && <Button variant="outlined" disabled={busy} onClick={() => confirmAction('Lock this card? New usage will be blocked.', () => dispatch(lockCard(selected.cardId)))}>Lock card</Button>}
              {selected.status === 'LOCKED' && <Button variant="contained" disabled={busy} onClick={() => confirmAction('Unlock this card?', () => dispatch(unlockCard(selected.cardId)))}>Unlock card</Button>}
              <FormControl size="small" sx={{ minWidth: 170 }}><InputLabel id="replacement-reason">Reason</InputLabel><Select labelId="replacement-reason" label="Reason" value={reason} onChange={(event) => setReason(event.target.value as ReplacementReason)}>{reasons.map((item) => <MenuItem key={item} value={item}>{label(item)}</MenuItem>)}</Select></FormControl>
              <Button color="warning" variant="outlined" disabled={busy || ['CLOSED', 'REPLACEMENT_REQUESTED'].includes(selected.status)} onClick={() => confirmAction('Request a replacement card?', () => dispatch(requestReplacement({ cardId: selected.cardId, reason })))}>Request replacement</Button>
            </Stack>
            <Divider />
            <Typography variant="h6" fontWeight={900}>Spending controls</Typography>
            {cards.controlsLoading && <LinearProgress />}
            {controls && <Stack spacing={1}>
              {([['onlinePurchasesEnabled', 'Online purchases'], ['contactlessEnabled', 'Contactless purchases'], ['internationalPurchasesEnabled', 'International purchases'], ['atmWithdrawalsEnabled', 'ATM withdrawals']] as const).map(([key, text]) => <FormControlLabel key={key} control={<Switch checked={controls[key]} onChange={(event) => setControls({ ...controls, [key]: event.target.checked, ...(key === 'atmWithdrawalsEnabled' && !event.target.checked ? { dailyAtmLimit: 0 } : {}) })} />} label={text} />)}
              <Stack direction={{ xs: 'column', sm: 'row' }} gap={2}><TextField type="number" label="Daily purchase limit" value={controls.dailyPurchaseLimit} inputProps={{ min: 0 }} onChange={(event) => setControls({ ...controls, dailyPurchaseLimit: event.target.value })} /><TextField type="number" label="Daily ATM limit" value={controls.dailyAtmLimit} disabled={!controls.atmWithdrawalsEnabled} inputProps={{ min: 0 }} onChange={(event) => setControls({ ...controls, dailyAtmLimit: event.target.value })} /></Stack>
              <Button variant="contained" disabled={busy || Number(controls.dailyPurchaseLimit) < 0 || Number(controls.dailyAtmLimit) < 0} onClick={() => confirmAction('Save these spending controls?', () => dispatch(updateCardControls({ cardId: selected.cardId, controls })))}>Save controls</Button>
            </Stack>}
            <Divider />
            <Typography variant="h6" fontWeight={900}>Lifecycle history</Typography>
            {cards.historyLoading && <LinearProgress />}
            <List>{cards.cardHistory.map((item, index) => <ListItem key={`${item.occurredAt}-${index}`} divider><ListItemText primary={`${label(item.action)} · ${label(item.newStatus)}`} secondary={new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(item.occurredAt))} /></ListItem>)}</List>
          </Stack></CardContent></Card>}
        </Grid>
      </Grid>
    </Stack>
  );
}
