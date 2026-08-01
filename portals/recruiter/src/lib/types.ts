export interface UserResponse {
  id: string;
  organizationId: string;
  email: string;
  displayName: string;
  status: string;
  emailVerifiedAt?: string | null;
  lastLoginAt?: string | null;
  createdAt: string;
  roles: string[];
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  tokenType: string;
  user: UserResponse;
}

export interface LoginRequest {
  email: string;
  password: string;
  organizationId?: string | null;
  deviceId?: string | null;
  userAgent?: string | null;
}

export interface RegisterOrganizationRequest {
  companyName: string;
  adminEmail: string;
  adminPassword: string;
  adminDisplayName: string;
}

export interface VerifyEmailRequest {
  token: string;
}

export interface RequestPasswordResetRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface PasswordResetResponse {
  resetToken: string | null;
  expiresInSeconds: number;
}

export interface OrganizationResponse {
  id: string;
  name: string;
  slug: string;
  legalName: string;
  status: string;
  settings: string;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateOrganizationRequest {
  name: string;
  legalName?: string | null;
  settings?: string | null;
}

export interface RecruiterResponse {
  id: string;
  organizationId: string;
  userId: string;
  fullName: string;
  email: string;
  title: string;
  status: string;
  createdAt: string;
}

export interface CreateRecruiterRequest {
  userId: string | null;
  fullName: string;
  email: string;
  title: string;
}

export interface CandidateResponse {
  id: string;
  organizationId: string;
  userId: string | null;
  email: string;
  fullName: string;
  phone: string;
  status: string;
  source: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCandidateRequest {
  userId?: string | null;
  email: string;
  fullName: string;
  phone?: string | null;
  source?: string | null;
}

export interface InterviewResponse {
  id: string;
  organizationId: string;
  candidateId: string;
  recruiterId: string;
  roundNumber: number;
  title: string;
  status: string;
  mode: string;
  meetingUrl: string;
  startsAt: string;
  endsAt: string;
  timezone: string;
  metadata: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateInterviewRequest {
  candidateId: string;
  recruiterId: string;
  roundNumber: number;
  title: string;
  mode: string;
  meetingUrl?: string | null;
  startsAt: string;
  endsAt: string;
  timezone?: string | null;
  metadata?: string | null;
}

export interface ScheduleInterviewRequest {
  startsAt: string;
  endsAt: string;
  timezone?: string | null;
  meetingUrl?: string | null;
}

export interface ViolationResponse {
  id: string;
  sessionId: string;
  interviewId: string;
  policyId: string;
  ruleCode: string;
  severity: string;
  message: string;
  status: string;
  evidence: string;
  occurredAt: string;
  detectedBy: string;
  createdAt: string;
}

export interface PolicyResponse {
  id: string;
  organizationId: string;
  code: string;
  name: string;
  description: string;
  status: string;
  defaultSeverity: string;
  priority: number;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface CreatePolicyRequest {
  code: string;
  name: string;
  description?: string | null;
  defaultSeverity: string;
  priority: number;
}

export interface ReportResponse {
  id: string;
  organizationId: string;
  type: string;
  title: string;
  status: string;
  format: string;
  score: number | null;
  filters: string;
  requestedBy: string;
  requestedAt: string;
  generatedAt: string | null;
  expiresAt: string | null;
  storageObjectId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateReportRequest {
  type: string;
  title: string;
  format: string;
  filters?: string | null;
}

export interface NotificationResponse {
  id: string;
  organizationId: string;
  userId: string;
  notificationType: string;
  channel: string;
  subject: string;
  body: string;
  priority: string;
  status: string;
  scheduledAt: string | null;
  sentAt: string | null;
  readAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AuditEventResponse {
  id: string;
  organizationId: string;
  actorId: string | null;
  actorType: string;
  action: string;
  resourceType: string;
  resourceId: string | null;
  outcome: string;
  occurredAt: string;
  requestId: string | null;
  ipAddress: string | null;
  userAgent: string | null;
  metadata: string;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
}

export interface FeatureResponse {
  id: string;
  organizationId: string;
  code: string;
  name: string;
  description: string;
  kind: string;
  createdAt: string;
}

export interface RoleResponse {
  id: string;
  organizationId: string;
  code: string;
  name: string;
  description: string;
  system: boolean;
  createdAt: string;
  permissionCodes: string[];
}

export interface PermissionResponse {
  id: string;
  code: string;
  name: string;
  description: string;
}

export interface SessionResponse {
  id: string;
  deviceId: string;
  ipAddress: string;
  userAgent: string;
  status: string;
  issuedAt: string;
  expiresAt: string;
  lastUsedAt: string | null;
}

export interface OrganizationSummary {
  interviews: number;
  completed: number;
  integrityScore: number;
  [key: string]: string | number | null;
}
