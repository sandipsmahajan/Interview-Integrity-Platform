import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import { Toaster } from 'react-hot-toast';
import { LoginPage } from '../pages/auth/LoginPage';
import { AuthProvider } from '../components/AuthProvider';
import { lightTheme } from '../theme';

vi.mock('../lib/api', () => ({
  api: {
    login: vi.fn().mockResolvedValue({
      user: { id: '1', displayName: 'Admin', email: 'admin@acme.test', roles: ['ORG_ADMIN'] }
    }),
    logout: vi.fn().mockResolvedValue(undefined),
    register: vi.fn().mockResolvedValue({ user: { id: '1', displayName: 'Admin', email: 'a@b.test', roles: [] } }),
    requestPasswordReset: vi.fn().mockResolvedValue({ resetToken: null, expiresInSeconds: 900 }),
    verifyEmail: vi.fn().mockResolvedValue(undefined)
  }
}));

function renderLogin() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } }
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={lightTheme}>
        <AuthProvider>
          <MemoryRouter initialEntries={['/login']}>
            <LoginPage />
          </MemoryRouter>
          <Toaster />
        </AuthProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.history.pushState({}, '', '/login');
  });

  it('renders the sign in form', () => {
    renderLogin();
    expect(screen.getByLabelText(/work email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^sign in$/i })).toBeInTheDocument();
    expect(screen.getByText('Welcome back')).toBeInTheDocument();
  });

  it('does not expose the organization id field on the login form', () => {
    renderLogin();
    expect(screen.queryByLabelText(/organization id/i)).not.toBeInTheDocument();
  });

  it('does not render social login options', () => {
    renderLogin();
    expect(screen.queryByRole('button', { name: /continue with google/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /continue with microsoft/i })).not.toBeInTheDocument();
    expect(screen.queryByText(/or continue with/i)).not.toBeInTheDocument();
  });

  it('leads with the invisible AI copilot detection capability', async () => {
    renderLogin();
    expect(await screen.findByText(/Detect Invisible AI Interview Copilots/i)).toBeInTheDocument();
  });

  it('shows validation errors for empty required fields', async () => {
    renderLogin();
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }));
    expect(await screen.findByText(/email is required/i)).toBeInTheDocument();
    expect(screen.getByText(/password is required/i)).toBeInTheDocument();
  });

  it('validates the email format', async () => {
    renderLogin();
    await userEvent.type(screen.getByLabelText(/work email/i), 'not-an-email');
    await userEvent.type(screen.getByLabelText(/^password/i), 'password123');
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }));
    expect(await screen.findByText(/enter a valid email address/i)).toBeInTheDocument();
  });
});
