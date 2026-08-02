import { createContext, useContext } from 'react';
import type {
  LoginRequest,
  LoginResponse,
  MfaVerifyRequest,
  RegisterOrganizationRequest,
  UserResponse
} from '../lib/types';

export interface AuthContextValue {
  user: UserResponse | null;
  loading: boolean;
  isAuthenticated: boolean;
  login: (payload: LoginRequest, rememberMe: boolean) => Promise<LoginResponse>;
  mfaVerify: (payload: MfaVerifyRequest) => Promise<UserResponse>;
  mfaEmailOtp: (challengeId: string) => Promise<void>;
  register: (payload: RegisterOrganizationRequest) => Promise<UserResponse>;
  logout: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}

export function useCurrentUserId(): string | null {
  const { user } = useAuth();
  return user?.id ?? null;
}
