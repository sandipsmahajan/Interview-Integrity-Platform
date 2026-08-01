import type { ReactNode } from 'react';
import {
  Analytics,
  Assessment,
  Download,
  FactCheck,
  Flag,
  Groups,
  Help,
  ListAlt,
  Notifications,
  PersonSearch,
  Policy,
  Settings,
  Shield,
  SpaceDashboard
} from '@mui/icons-material';

export interface NavItem {
  label: string;
  path: string;
  icon: ReactNode;
  group: string;
  keywords?: string[];
}

export const NAV_GROUPS = ['Overview', 'Operations', 'Governance', 'Platform'] as const;

export const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', path: '/', icon: <SpaceDashboard />, group: 'Overview', keywords: ['home', 'overview', 'kpi', 'stats'] },
  { label: 'Interviews', path: '/interviews', icon: <ListAlt />, group: 'Operations', keywords: ['schedule', 'sessions', 'live'] },
  { label: 'Candidates', path: '/candidates', icon: <PersonSearch />, group: 'Operations', keywords: ['people', 'applicants', 'talent'] },
  { label: 'Recruiters', path: '/recruiters', icon: <Groups />, group: 'Operations', keywords: ['team', 'staff', 'users', 'members'] },
  { label: 'Integrity', path: '/integrity', icon: <Shield />, group: 'Operations', keywords: ['violations', 'alerts', 'compliance', 'risk'] },
  { label: 'Policies', path: '/policies', icon: <Policy />, group: 'Operations', keywords: ['rules', 'policy engine', 'severity'] },
  { label: 'Reports', path: '/reports', icon: <Assessment />, group: 'Operations', keywords: ['pdf', 'export', 'summaries'] },
  { label: 'Analytics', path: '/analytics', icon: <Analytics />, group: 'Operations', keywords: ['charts', 'trends', 'performance'] },
  { label: 'Audit Logs', path: '/audit', icon: <FactCheck />, group: 'Governance', keywords: ['history', 'login history', 'activity'] },
  { label: 'Feature Flags', path: '/feature-flags', icon: <Flag />, group: 'Governance', keywords: ['experiments', 'flags', 'features'] },
  { label: 'Notifications', path: '/notifications', icon: <Notifications />, group: 'Platform', keywords: ['inbox', 'alerts', 'unread'] },
  { label: 'Downloads', path: '/downloads', icon: <Download />, group: 'Platform', keywords: ['client', 'desktop', 'windows', 'macos', 'linux', 'install'] },
  { label: 'Settings', path: '/settings', icon: <Settings />, group: 'Platform', keywords: ['organization', 'branding', 'billing', 'security', 'sso', 'api keys'] },
  { label: 'Help Center', path: '/help', icon: <Help />, group: 'Platform', keywords: ['docs', 'support', 'guide'] }
];

export function navItemForPath(pathname: string): NavItem | undefined {
  return NAV_ITEMS.find((item) => item.path === pathname || pathname.startsWith(`${item.path}/`));
}
