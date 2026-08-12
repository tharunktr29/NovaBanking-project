import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { setupStore } from '../store';
import { LoginPage } from './LoginPage';

describe('LoginPage', () => {
  it('renders the NovaBank sign-in form', () => {
    const store = setupStore();

    render(
      <Provider store={store}>
        <MemoryRouter>
          <LoginPage />
        </MemoryRouter>
      </Provider>
    );

    expect(screen.getByRole('heading', { name: /secure sign in/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/username or email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
  });
});
