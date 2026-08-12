import type { ReactElement } from 'react';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { setupStore } from '../store';
import { DashboardPage } from './DashboardPage';
import { ForgotPasswordPage } from './ForgotPasswordPage';
import { LogoutPage } from './LogoutPage';
import { MfaPage } from './MfaPage';
import { NotFoundPage } from './NotFoundPage';
import { RegisterPage } from './RegisterPage';
import { ResetPasswordPage } from './ResetPasswordPage';

function renderPage(page: ReactElement) {
  return render(
    <Provider store={setupStore()}>
      <MemoryRouter>
        {page}
      </MemoryRouter>
    </Provider>
  );
}

describe('application pages', () => {
  it('renders the registration form', () => {
    renderPage(<RegisterPage />);

    expect(screen.getByRole('heading', { name: /create demo account/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/^username$/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^email$/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^password$/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument();
  });

  it('renders the forgot password form', () => {
    renderPage(<ForgotPasswordPage />);

    expect(screen.getByRole('heading', { name: /reset your password/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/^email$/i)).toBeInTheDocument();
  });

  it('renders the reset password form', () => {
    renderPage(<ResetPasswordPage />);

    expect(screen.getByRole('heading', { name: /set a new password/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/reset token/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^new password$/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/confirm new password/i)).toBeInTheDocument();
  });

  it('renders the MFA verification form', () => {
    renderPage(<MfaPage />);

    expect(screen.getByRole('heading', { name: /mock mfa verification/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/challenge id/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/six-digit code/i)).toBeInTheDocument();
  });

  it('renders the dashboard shell', () => {
    renderPage(<DashboardPage />);

    expect(screen.getByRole('heading', { name: /welcome, customer/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /overview/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /security/i })).toBeInTheDocument();
  });

  it('renders the logout confirmation', () => {
    renderPage(<LogoutPage />);

    expect(screen.getByRole('heading', { name: /you are signed out/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /return to sign in/i })).toBeInTheDocument();
  });

  it('renders the not found page', () => {
    renderPage(<NotFoundPage />);

    expect(screen.getByRole('heading', { name: /page not found/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /go to dashboard/i })).toBeInTheDocument();
  });
});
