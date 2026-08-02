import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api } from '../lib/api';
import { clearTokens, getRefreshToken, readStoredUser, writeRememberMePreference } from '../lib/session';
import { AuthContext, type AuthContextValue } from '../hooks/useAuth';
import type { LoginRequest, MfaVerifyRequest, RegisterOrganizationRequest, UserResponse } from '../lib/types';

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
    const response = await api.login({ ...payload, deviceId: payload.deviceId ?? 'web-portal' });
    if ('accessToken' in response) {
      setUser(response.user);
    }
    return response;
  }, []);

  const mfaVerify = useCallback(async (payload: MfaVerifyRequest) => {
    const tokens = await api.mfaVerify({ ...payload, deviceId: payload.deviceId ?? 'web-portal' });
    setUser(tokens.user);
    return tokens.user;
  }, []);

  const mfaEmailOtp = useCallback(async (challengeId: string) => {
    await api.mfaEmailOtp(challengeId);
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
    () => ({ user, loading, isAuthenticated: Boolean(user), login, mfaVerify, mfaEmailOtp, register, logout }),
    [user, loading, login, mfaVerify, mfaEmailOtp, register, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
