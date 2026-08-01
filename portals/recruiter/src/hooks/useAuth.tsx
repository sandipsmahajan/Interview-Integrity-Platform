import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode
} from 'react';
import { api } from '../lib/api';
import { clearTokens, getRefreshToken, readStoredUser, writeRememberMePreference } from '../lib/session';
import type { LoginRequest, RegisterOrganizationRequest, UserResponse } from '../lib/types';

interface AuthContextValue {
  user: UserResponse | null;
  loading: boolean;
  isAuthenticated: boolean;
  login: (payload: LoginRequest, rememberMe: boolean) => Promise<UserResponse>;
  register: (payload: RegisterOrganizationRequest) => Promise<UserResponse>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(() => readStoredUser());
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    const timer = window.setTimeout(() => setLoading(false), 0);
    return () => window.clearTimeout(timer);
  }, []);

  const login = useCallback(async (payload: LoginRequest, rememberMe: boolean) => {
    writeRememberMePreference(rememberMe);
    const tokens = await api.login({ ...payload, deviceId: payload.deviceId ?? 'web-portal' });
    setUser(tokens.user);
    return tokens.user;
  }, []);

  const register = useCallback(async (payload: RegisterOrganizationRequest) => {
    const tokens = await api.register(payload);
    setUser(tokens.user);
    return tokens.user;
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = getRefreshToken();
    await api.logout(refreshToken);
    clearTokens();
    setUser(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ user, loading, isAuthenticated: Boolean(user), login, register, logout }),
    [user, loading, login, register, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}

export function useCurrentUserId(): string | null {
  const { user } = useAuth();
  return user?.id ?? null;
}
