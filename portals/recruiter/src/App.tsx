import { lazy, Suspense, useEffect, useMemo, useState } from 'react';
import { BrowserRouter, Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import { lightTheme, darkTheme } from './theme';
import { AuthProvider, useAuth } from './hooks/useAuth';
import { AppShell } from './components/AppShell';
import { LoginPage } from './pages/auth/LoginPage';
import { RegisterPage } from './pages/auth/RegisterPage';
import { ForgotPasswordPage } from './pages/auth/ForgotPasswordPage';
import { ResetPasswordPage } from './pages/auth/ResetPasswordPage';
import { VerifyEmailPage } from './pages/auth/VerifyEmailPage';
import { DashboardPage } from './pages/DashboardPage';
import BoxLoader from './components/BoxLoader';

const OnboardingPage = lazy(() => import('./pages/OnboardingPage').then((m) => ({ default: m.OnboardingPage })));
const InterviewsPage = lazy(() => import('./pages/InterviewsPage').then((m) => ({ default: m.InterviewsPage })));
const InterviewDetailPage = lazy(() => import('./pages/InterviewDetailPage').then((m) => ({ default: m.InterviewDetailPage })));
const CandidatesPage = lazy(() => import('./pages/CandidatesPage').then((m) => ({ default: m.CandidatesPage })));
const RecruitersPage = lazy(() => import('./pages/RecruitersPage').then((m) => ({ default: m.RecruitersPage })));
const IntegrityPage = lazy(() => import('./pages/IntegrityPage').then((m) => ({ default: m.IntegrityPage })));
const PoliciesPage = lazy(() => import('./pages/PoliciesPage').then((m) => ({ default: m.PoliciesPage })));
const ReportsPage = lazy(() => import('./pages/ReportsPage').then((m) => ({ default: m.ReportsPage })));
const AnalyticsPage = lazy(() => import('./pages/AnalyticsPage').then((m) => ({ default: m.AnalyticsPage })));
const AuditPage = lazy(() => import('./pages/AuditPage').then((m) => ({ default: m.AuditPage })));
const FeatureFlagsPage = lazy(() => import('./pages/FeatureFlagsPage').then((m) => ({ default: m.FeatureFlagsPage })));
const NotificationsPage = lazy(() => import('./pages/NotificationsPage').then((m) => ({ default: m.NotificationsPage })));
const DownloadsPage = lazy(() => import('./pages/DownloadsPage').then((m) => ({ default: m.DownloadsPage })));
const SettingsPage = lazy(() => import('./pages/SettingsPage').then((m) => ({ default: m.SettingsPage })));
const HelpPage = lazy(() => import('./pages/HelpPage').then((m) => ({ default: m.HelpPage })));

function RequireAuth({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return <>{children}</>;
}

function PublicOnly({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}

function Shell() {
  return (
    <Suspense fallback={<BoxLoader rows={6} height={40} />}>
      <Outlet />
    </Suspense>
  );
}

function AppRoutes() {
  const location = useLocation();
  return (
    <Routes location={location}>
      <Route path="/login" element={<PublicOnly><LoginPage /></PublicOnly>} />
      <Route path="/register" element={<PublicOnly><RegisterPage /></PublicOnly>} />
      <Route path="/forgot-password" element={<PublicOnly><ForgotPasswordPage /></PublicOnly>} />
      <Route path="/reset-password" element={<PublicOnly><ResetPasswordPage /></PublicOnly>} />
      <Route path="/verify-email" element={<PublicOnly><VerifyEmailPage /></PublicOnly>} />

      <Route
        element={
          <RequireAuth>
            <Shell />
          </RequireAuth>
        }
      >
        <Route path="/onboarding" element={<OnboardingPage />} />
        <Route path="/" element={<DashboardPage />} />
        <Route path="/interviews" element={<InterviewsPage />} />
        <Route path="/interviews/:id" element={<InterviewDetailPage />} />
        <Route path="/candidates" element={<CandidatesPage />} />
        <Route path="/recruiters" element={<RecruitersPage />} />
        <Route path="/integrity" element={<IntegrityPage />} />
        <Route path="/policies" element={<PoliciesPage />} />
        <Route path="/reports" element={<ReportsPage />} />
        <Route path="/analytics" element={<AnalyticsPage />} />
        <Route path="/audit" element={<AuditPage />} />
        <Route path="/feature-flags" element={<FeatureFlagsPage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
        <Route path="/downloads" element={<DownloadsPage />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="/help" element={<HelpPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export function App() {
  const [mode, setMode] = useState<'light' | 'dark'>(() => {
    const saved = localStorage.getItem('ip.theme');
    if (saved === 'light' || saved === 'dark') return saved;
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  });

  useEffect(() => {
    localStorage.setItem('ip.theme', mode);
  }, [mode]);

  const theme = useMemo(() => (mode === 'dark' ? darkTheme : lightTheme), [mode]);

  return (
    <ThemeProvider theme={theme}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route
              path="*"
              element={
                <>
                  <PublicRoutes mode={mode} onToggleMode={() => setMode(mode === 'dark' ? 'light' : 'dark')} />
                </>
              }
            />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  );
}

function PublicRoutes({ mode, onToggleMode }: { mode: 'light' | 'dark'; onToggleMode: () => void }) {
  const location = useLocation();
  const isPublic = ['/login', '/register', '/forgot-password', '/reset-password', '/verify-email'].some((path) =>
    location.pathname.startsWith(path)
  );
  if (isPublic) {
    return <AppRoutes />;
  }
  return (
    <AppShell mode={mode} onToggleMode={onToggleMode}>
      <AppRoutes />
    </AppShell>
  );
}
