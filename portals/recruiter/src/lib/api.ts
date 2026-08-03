import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { clearTokens, getAccessToken, getRefreshToken, setTokens, persistUser } from './session';
import type {
  AuditEventResponse,
  CandidateResponse,
  CreateCandidateRequest,
  CreateInterviewRequest,
  CreatePolicyRequest,
  CreateRecruiterRequest,
  CreateReportRequest,
  FeatureResponse,
  InterviewResponse,
  LoginRequest,
  LoginResponse,
  MfaVerifyRequest,
  NotificationResponse,
  OrganizationResponse,
  PageResponse,
  PermissionResponse,
  PolicyResponse,
  RecruiterResponse,
  RegisterOrganizationRequest,
  ReportResponse,
  ResetPasswordRequest,
  RoleResponse,
  SessionResponse,
  TokenResponse,
  UpdateOrganizationRequest,
  UserResponse,
  VerifyEmailRequest,
  ViolationResponse
} from './types';

export class ApiError extends Error {
  readonly status?: number;

  constructor(message: string, status?: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 20_000,
  headers: { 'Content-Type': 'application/json' }
});

http.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshing: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return null;
  try {
    const response = await axios.post<{ accessToken: string; expiresInSeconds: number }>(
      '/api/v1/auth/refresh',
      { refreshToken }
    );
    const access = response.data.accessToken;
    const refresh = getRefreshToken() ?? refreshToken;
    setTokens(access, refresh, Boolean(localStorage.getItem('ip.remember_me') === '1'));
    return access;
  } catch {
    clearTokens();
    return null;
  }
}

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;
    if (error.response?.status === 401 && original && !original._retry) {
      original._retry = true;
      refreshing = refreshing ?? refreshAccessToken();
      const token = await refreshing;
      refreshing = null;
      if (token) {
        original.headers.Authorization = `Bearer ${token}`;
        return http(original);
      }
    }
    return Promise.reject(error);
  }
);

function toApiError(error: unknown): ApiError {
  if (axios.isAxiosError(error)) {
    return new ApiError(readServerMessage(error), error.response?.status);
  }
  return new ApiError(error instanceof Error ? error.message : 'Unexpected error');
}

function readServerMessage(error: AxiosError): string {
  const data = error.response?.data;
  if (typeof data === 'string') {
    const trimmed = data.trim();
    if (trimmed.length > 0 && trimmed.startsWith('{')) {
      try {
        const parsed = JSON.parse(trimmed) as { message?: unknown; detail?: unknown };
        return firstMessage(parsed.message, parsed.detail) ?? error.message;
      } catch {
        // fall through to the generic message
      }
    }
    return trimmed.length > 0 ? trimmed : error.message;
  }
  if (data && typeof data === 'object') {
    const record = data as { message?: unknown; detail?: unknown };
    return firstMessage(record.message, record.detail) ?? error.message;
  }
  return error.message;
}

function firstMessage(...candidates: unknown[]): string | null {
  for (const candidate of candidates) {
    if (typeof candidate === 'string' && candidate.trim().length > 0) {
      return candidate;
    }
  }
  return null;
}

export const api = {
  // ---- Auth ----
  async login(payload: LoginRequest): Promise<LoginResponse> {
    try {
      const { data } = await http.post<LoginResponse>('/v1/auth/login', payload);
      if ('accessToken' in data) {
        setTokens(data.accessToken, data.refreshToken, Boolean(payload.deviceId));
        persistUser(data.user);
      }
      return data;
    } catch (e) {
      throw toApiError(e);
    }
  },
  async mfaVerify(payload: MfaVerifyRequest): Promise<TokenResponse> {
    try {
      const { data } = await http.post<TokenResponse>('/v1/auth/mfa/verify', payload);
      setTokens(data.accessToken, data.refreshToken, Boolean(payload.deviceId));
      persistUser(data.user);
      return data;
    } catch (e) {
      throw toApiError(e);
    }
  },
  async mfaEmailOtp(challengeId: string): Promise<void> {
    try {
      await http.post('/v1/auth/mfa/email-otp', { challengeId });
    } catch (e) {
      throw toApiError(e);
    }
  },
  async register(payload: RegisterOrganizationRequest): Promise<TokenResponse> {
    try {
      const { data } = await http.post<TokenResponse>('/v1/auth/register', payload);
      setTokens(data.accessToken, data.refreshToken, false);
      persistUser(data.user);
      return data;
    } catch (e) {
      throw toApiError(e);
    }
  },
  async verifyEmail(payload: VerifyEmailRequest): Promise<void> {
    try {
      await http.post('/v1/auth/verify-email', payload);
    } catch (e) {
      throw toApiError(e);
    }
  },
  async requestPasswordReset(email: string): Promise<{ resetToken: string | null; expiresInSeconds: number }> {
    try {
      const { data } = await http.post('/v1/auth/password/reset-request', { email });
      return data;
    } catch (e) {
      throw toApiError(e);
    }
  },
  async resetPassword(payload: ResetPasswordRequest): Promise<void> {
    try {
      await http.post('/v1/auth/password/reset', payload);
    } catch (e) {
      throw toApiError(e);
    }
  },
  async logout(refreshToken: string | null): Promise<void> {
    try {
      if (refreshToken) await http.post('/v1/auth/logout', { refreshToken });
    } catch {
      // local logout must still proceed
    }
    clearTokens();
  },

  // ---- Organization ----
  async getOrganization(): Promise<OrganizationResponse> {
    const { data } = await http.get<OrganizationResponse>('/v1/organizations');
    return data;
  },
  async updateOrganization(payload: UpdateOrganizationRequest): Promise<OrganizationResponse> {
    const { data } = await http.patch<OrganizationResponse>('/v1/organizations', payload);
    return data;
  },

  // ---- Recruiters ----
  async listRecruiters(status?: string): Promise<RecruiterResponse[]> {
    const { data } = await http.get<RecruiterResponse[]>('/v1/recruiters', {
      params: status ? { status } : undefined
    });
    return data;
  },
  async createRecruiter(payload: CreateRecruiterRequest): Promise<RecruiterResponse> {
    const { data } = await http.post<RecruiterResponse>('/v1/recruiters', payload);
    return data;
  },
  async updateRecruiter(id: string, payload: { fullName: string; email: string; title: string }): Promise<RecruiterResponse> {
    const { data } = await http.patch<RecruiterResponse>(`/v1/recruiters/${id}`, payload);
    return data;
  },
  async changeRecruiterStatus(id: string, status: string): Promise<RecruiterResponse> {
    const { data } = await http.post<RecruiterResponse>(`/v1/recruiters/${id}/status`, { status });
    return data;
  },
  async getMyRecruiterProfile(): Promise<RecruiterResponse> {
    const { data } = await http.get<RecruiterResponse>('/v1/recruiters/me');
    return data;
  },

  // ---- Candidates ----
  async listCandidates(status?: string): Promise<CandidateResponse[]> {
    const { data } = await http.get<CandidateResponse[]>('/v1/candidates', {
      params: status ? { status } : undefined
    });
    return data;
  },
  async createCandidate(payload: CreateCandidateRequest): Promise<CandidateResponse> {
    const { data } = await http.post<CandidateResponse>('/v1/candidates', payload);
    return data;
  },
  async changeCandidateStatus(id: string, status: string): Promise<CandidateResponse> {
    const { data } = await http.post<CandidateResponse>(`/v1/candidates/${id}/status`, { status });
    return data;
  },

  // ---- Interviews ----
  async listInterviews(status?: string): Promise<InterviewResponse[]> {
    const { data } = await http.get<InterviewResponse[]>('/v1/interviews', {
      params: status ? { status } : undefined
    });
    return data;
  },
  async getInterview(id: string): Promise<InterviewResponse> {
    const { data } = await http.get<InterviewResponse>(`/v1/interviews/${id}`);
    return data;
  },
  async createInterview(payload: CreateInterviewRequest): Promise<InterviewResponse> {
    const { data } = await http.post<InterviewResponse>('/v1/interviews', payload);
    return data;
  },
  async scheduleInterview(id: string, payload: { startsAt: string; endsAt: string; timezone?: string | null; meetingUrl?: string | null }): Promise<InterviewResponse> {
    const { data } = await http.post<InterviewResponse>(`/v1/interviews/${id}/schedule`, payload);
    return data;
  },
  async cancelInterview(id: string): Promise<InterviewResponse> {
    const { data } = await http.post<InterviewResponse>(`/v1/interviews/${id}/cancel`);
    return data;
  },
  async markInterviewNoShow(id: string): Promise<InterviewResponse> {
    const { data } = await http.post<InterviewResponse>(`/v1/interviews/${id}/no-show`);
    return data;
  },

  // ---- Violations ----
  async listViolations(status?: string, severity?: string): Promise<ViolationResponse[]> {
    const { data } = await http.get<ViolationResponse[]>('/v1/violations', {
      params: { status, severity }
    });
    return data;
  },
  async reviewViolation(id: string, payload: { action: string; comment?: string | null; escalatedTo?: string | null }): Promise<ViolationResponse> {
    const { data } = await http.post<ViolationResponse>(`/v1/violations/${id}/review`, payload);
    return data;
  },

  // ---- Policies ----
  async listPolicies(): Promise<PolicyResponse[]> {
    const { data } = await http.get<PolicyResponse[]>('/v1/policies');
    return data;
  },
  async createPolicy(payload: CreatePolicyRequest): Promise<PolicyResponse> {
    const { data } = await http.post<PolicyResponse>('/v1/policies', payload);
    return data;
  },
  async changePolicyStatus(id: string, status: string): Promise<PolicyResponse> {
    const { data } = await http.post<PolicyResponse>(`/v1/policies/${id}/status`, { status });
    return data;
  },

  // ---- Reports ----
  async listReports(status?: string, type?: string): Promise<ReportResponse[]> {
    const { data } = await http.get<ReportResponse[]>('/v1/reports', {
      params: { status, type }
    });
    return data;
  },
  async createReport(payload: CreateReportRequest): Promise<ReportResponse> {
    const { data } = await http.post<ReportResponse>('/v1/reports', payload);
    return data;
  },
  async generateReport(id: string): Promise<ReportResponse> {
    const { data } = await http.post<ReportResponse>(`/v1/reports/${id}/generate`);
    return data;
  },

  // ---- Notifications ----
  async listNotifications(userId: string, status?: string): Promise<NotificationResponse[]> {
    const { data } = await http.get<NotificationResponse[]>('/v1/notifications', {
      params: { userId, status }
    });
    return data;
  },
  async markNotificationRead(id: string): Promise<NotificationResponse> {
    const { data } = await http.post<NotificationResponse>(`/v1/notifications/${id}/read`);
    return data;
  },

  // ---- Audit ----
  async listAuditEvents(params: {
    page?: number;
    size?: number;
    resourceType?: string;
    outcome?: string;
  }): Promise<PageResponse<AuditEventResponse>> {
    const { data } = await http.get<PageResponse<AuditEventResponse>>('/v1/audit-events', { params });
    return data;
  },

  // ---- Features ----
  async listFeatures(): Promise<FeatureResponse[]> {
    const { data } = await http.get<FeatureResponse[]>('/v1/features');
    return data;
  },
  async createFeature(payload: { code: string; name: string; description: string | null; kind: string }): Promise<FeatureResponse> {
    const { data } = await http.post<FeatureResponse>('/v1/features', payload);
    return data;
  },

  // ---- Identity / IAM ----
  async listRoles(): Promise<RoleResponse[]> {
    const { data } = await http.get<RoleResponse[]>('/v1/roles');
    return data;
  },
  async listPermissions(): Promise<PermissionResponse[]> {
    const { data } = await http.get<PermissionResponse[]>('/v1/permissions');
    return data;
  },
  async listUsers(page = 0, size = 20): Promise<PageResponse<UserResponse>> {
    const { data } = await http.get<PageResponse<UserResponse>>('/v1/users', { params: { page, size } });
    return data;
  },
  async listMySessions(): Promise<SessionResponse[]> {
    const { data } = await http.get<SessionResponse[]>('/v1/auth/sessions');
    return data;
  },
  async revokeSession(id: string): Promise<void> {
    await http.delete(`/v1/auth/sessions/${id}`);
  },
  async revokeAllSessions(): Promise<void> {
    await http.delete('/v1/auth/sessions');
  }
};

export { http, toApiError };
