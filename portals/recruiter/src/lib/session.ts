const ACCESS_KEY = 'ip.access_token';
const REFRESH_KEY = 'ip.refresh_token';
const USER_KEY = 'ip.user';
const REMEMBER_KEY = 'ip.remember_me';

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_KEY);
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_KEY);
}

export function setTokens(
  accessToken: string,
  refreshToken: string,
  rememberMe: boolean
): void {
  if (rememberMe) {
    localStorage.setItem(ACCESS_KEY, accessToken);
    localStorage.setItem(REFRESH_KEY, refreshToken);
  } else {
    sessionStorage.setItem(ACCESS_KEY, accessToken);
    sessionStorage.setItem(REFRESH_KEY, refreshToken);
  }
}

export function clearTokens(): void {
  localStorage.removeItem(ACCESS_KEY);
  localStorage.removeItem(REFRESH_KEY);
  sessionStorage.removeItem(ACCESS_KEY);
  sessionStorage.removeItem(REFRESH_KEY);
  localStorage.removeItem(USER_KEY);
}

export function readStoredUser() {
  const raw = localStorage.getItem(USER_KEY) ?? sessionStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as import('./types').UserResponse;
  } catch {
    return null;
  }
}

export function persistUser(user: import('./types').UserResponse): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user));
  sessionStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function hasRememberedSession(): boolean {
  return Boolean(localStorage.getItem(REFRESH_KEY));
}

export function readRememberMePreference(): boolean {
  return localStorage.getItem(REMEMBER_KEY) === '1';
}

export function writeRememberMePreference(value: boolean): void {
  localStorage.setItem(REMEMBER_KEY, value ? '1' : '0');
}
