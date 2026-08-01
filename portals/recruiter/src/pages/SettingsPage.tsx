import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  CreditCard,
  Download,
  Group,
  Key,
  Notifications,
  Palette,
  Security,
  Shield,
  Tune
} from '@mui/icons-material';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import toast from 'react-hot-toast';
import { api } from '../lib/api';
import { formatDateTime } from '../lib/format';
import { PageHeader } from '../components/PageHeader';
import { StatusChip } from '../components/StatusChip';
import BoxLoader from '../components/BoxLoader';
import { useAuth } from '../hooks/useAuth';

const TABS = [
  { key: 'organization', label: 'Organization', icon: <Tune /> },
  { key: 'branding', label: 'Branding', icon: <Palette /> },
  { key: 'users', label: 'Users', icon: <Group /> },
  { key: 'roles', label: 'Roles & Permissions', icon: <Shield /> },
  { key: 'sso', label: 'SSO', icon: <Key /> },
  { key: 'api-keys', label: 'API Keys', icon: <Key /> },
  { key: 'notifications', label: 'Notifications', icon: <Notifications /> },
  { key: 'downloads', label: 'Downloads', icon: <Download /> },
  { key: 'billing', label: 'Billing', icon: <CreditCard /> },
  { key: 'security', label: 'Security', icon: <Security /> }
];

export function SettingsPage() {
  const [params, setParams] = useSearchParams();
  const requested = params.get('tab') ?? 'organization';
  const tab = TABS.some((item) => item.key === requested) ? requested : 'organization';

  function switchTab(key: string) {
    setParams({ tab: key });
  }

  return (
    <Box>
      <PageHeader title="Settings" subtitle="Manage your organization, team and security." />
      <Box sx={{ display: 'flex', gap: 3, flexDirection: { xs: 'column', md: 'row' } }}>
        <Card variant="outlined" sx={{ width: { md: 260 }, flexShrink: 0, alignSelf: 'flex-start' }}>
          <List dense>
            {TABS.map((item) => (
              <ListItemButton key={item.key} selected={tab === item.key} onClick={() => switchTab(item.key)}>
                <ListItemIcon sx={{ minWidth: 36 }}>{item.icon}</ListItemIcon>
                <ListItemText primary={item.label} slotProps={{ primary: { sx: { fontWeight: 600, fontSize: 14 } } }} />
              </ListItemButton>
            ))}
          </List>
        </Card>
        <Box sx={{ flex: 1, minWidth: 0 }}>
          {tab === 'organization' ? <OrganizationTab /> : null}
          {tab === 'branding' ? <BrandingTab /> : null}
          {tab === 'users' ? <UsersTab /> : null}
          {tab === 'roles' ? <RolesTab /> : null}
          {tab === 'sso' ? <SsoTab /> : null}
          {tab === 'api-keys' ? <ApiKeysTab /> : null}
          {tab === 'notifications' ? <NotificationsTab /> : null}
          {tab === 'downloads' ? <DownloadsTab /> : null}
          {tab === 'billing' ? <BillingTab /> : null}
          {tab === 'security' ? <SecurityTab /> : null}
        </Box>
      </Box>
    </Box>
  );
}

function CardTitle({ children }: { children: React.ReactNode }) {
  return (
    <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
      {children}
    </Typography>
  );
}

function OrganizationTab() {
  const queryClient = useQueryClient();
  const organization = useQuery({ queryKey: ['organization'], queryFn: () => api.getOrganization() });
  const [name, setName] = useState('');
  const [legalName, setLegalName] = useState('');

  const updateMutation = useMutation({
    mutationFn: (payload: { name: string; legalName: string }) => api.updateOrganization({ name: payload.name, legalName: payload.legalName || null }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organization'] });
      toast.success('Organization updated');
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Update failed')
  });

  if (organization.isLoading) return <BoxLoader rows={3} />;
  const data = organization.data;
  if (!data) return <Typography>Organization not found.</Typography>;

  const localName = name || data.name;
  const localLegalName = legalName || data.legalName;

  return (
    <Card variant="outlined">
      <CardContent>
        <CardTitle>Organization</CardTitle>
        <Stack spacing={2} sx={{ maxWidth: 480 }}>
          <TextField label="Company name" value={localName} onChange={(event) => setName(event.target.value)} />
          <TextField label="Legal name" value={localLegalName} onChange={(event) => setLegalName(event.target.value)} />
          <TextField label="Slug" value={data.slug} disabled helperText="Slug cannot be changed after creation." />
          <Box>
            <Typography variant="caption" color="text.secondary">
              Status
            </Typography>
            <StatusChip status={data.status} />
          </Box>
          <Button
            variant="contained"
            disabled={updateMutation.isPending}
            onClick={() => updateMutation.mutate({ name: localName, legalName: localLegalName })}
            sx={{ alignSelf: 'flex-start' }}
          >
            {updateMutation.isPending ? 'Saving...' : 'Save changes'}
          </Button>
        </Stack>
      </CardContent>
    </Card>
  );
}

function BrandingTab() {
  return (
    <Card variant="outlined">
      <CardContent>
        <CardTitle>Branding</CardTitle>
        <Stack spacing={2} sx={{ maxWidth: 480 }}>
          <TextField label="Primary color" defaultValue="#2563eb" helperText="Used on the login screen and email templates." />
          <TextField label="Logo URL" placeholder="https://..." helperText="Upload your logo to object storage and reference the signed URL." />
          <TextField label="Company tagline" placeholder="Hire with integrity" />
          <Typography variant="body2" color="text.secondary">
            Branding values are rendered by the email template engine when generating transactional emails.
          </Typography>
        </Stack>
      </CardContent>
    </Card>
  );
}

function UsersTab() {
  const users = useQuery({ queryKey: ['users'], queryFn: () => api.listUsers(0, 100) });
  if (users.isLoading) return <BoxLoader rows={3} />;
  const items = users.data?.items ?? [];
  return (
    <Card variant="outlined">
      <CardContent>
        <CardTitle>Users</CardTitle>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          {items.length} user(s) in your organization.
        </Typography>
        <Stack divider={<Divider />} spacing={1.5}>
          {items.map((user) => (
            <Box key={user.id} sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Box sx={{ flex: 1 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                  {user.displayName}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {user.email} · {user.roles.join(', ')}
                </Typography>
              </Box>
              <StatusChip status={user.status} />
            </Box>
          ))}
        </Stack>
      </CardContent>
    </Card>
  );
}

function RolesTab() {
  const roles = useQuery({ queryKey: ['roles'], queryFn: () => api.listRoles() });
  const permissions = useQuery({ queryKey: ['permissions'], queryFn: () => api.listPermissions() });
  if (roles.isLoading || permissions.isLoading) return <BoxLoader rows={3} />;
  return (
    <Card variant="outlined">
      <CardContent>
        <CardTitle>Roles & Permissions</CardTitle>
        <Stack spacing={2}>
          {(roles.data ?? []).map((role) => (
            <Box key={role.id} sx={{ p: 2, border: 1, borderColor: 'divider', borderRadius: 2 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                  {role.name}
                </Typography>
                <Chip size="small" label={role.code} variant="outlined" />
                {role.system ? <Chip size="small" label="System" color="primary" variant="outlined" /> : null}
              </Box>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                {role.description}
              </Typography>
              <Stack direction="row" spacing={0.5} sx={{ flexWrap: 'wrap', gap: 0.5 }}>
                {role.permissionCodes.map((code) => (
                  <Chip key={code} size="small" label={code} />
                ))}
              </Stack>
            </Box>
          ))}
          <Typography variant="caption" color="text.secondary">
            {permissions.data?.length ?? 0} permission codes available in the catalog.
          </Typography>
        </Stack>
      </CardContent>
    </Card>
  );
}

function SsoTab() {
  return (
    <Card variant="outlined">
      <CardContent>
        <CardTitle>Single Sign-On</CardTitle>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          SSO configuration is captured during onboarding. Connect your identity provider to enforce
          SAML 2.0 or OIDC sign-in for your domain.
        </Typography>
        <Button variant="contained" onClick={() => toast.success('SSO connection flow opened')}>
          Configure identity provider
        </Button>
      </CardContent>
    </Card>
  );
}

function ApiKeysTab() {
  return (
    <Card variant="outlined">
      <CardContent>
        <CardTitle>API Keys</CardTitle>
        <Typography variant="body2" color="text.secondary">
          API keys are managed by the identity service in the production deployment. Rotate keys
          regularly and scope them to the minimum permissions required.
        </Typography>
      </CardContent>
    </Card>
  );
}

function NotificationsTab() {
  return (
    <Card variant="outlined">
      <CardContent>
        <CardTitle>Notification preferences</CardTitle>
        <Typography variant="body2" color="text.secondary">
          Per-user notification preferences are served by the notification service
          ({'/api/v1/notification-preferences'}). Configure which channels you receive for
          interview reminders, integrity alerts and report notifications.
        </Typography>
      </CardContent>
    </Card>
  );
}

function DownloadsTab() {
  return (
    <Card variant="outlined">
      <CardContent>
        <CardTitle>Downloads</CardTitle>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Download the desktop client for Windows, macOS and Linux from the Downloads page.
        </Typography>
        <Button component="a" href="#/downloads" variant="outlined">
          Go to Downloads
        </Button>
      </CardContent>
    </Card>
  );
}

function BillingTab() {
  return (
    <Card variant="outlined">
      <CardContent>
        <CardTitle>Billing</CardTitle>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Subscriptions are managed through the organization service. Contact your account team to
          change plans, review invoices or update payment methods.
        </Typography>
        <Button variant="contained" onClick={() => toast.success('Billing portal opened')}>
          Open billing portal
        </Button>
      </CardContent>
    </Card>
  );
}

function SecurityTab() {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  const sessions = useQuery({ queryKey: ['sessions'], queryFn: () => api.listMySessions() });

  const revokeMutation = useMutation({
    mutationFn: api.revokeSession,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sessions'] });
      toast.success('Session revoked');
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Revoke failed')
  });

  const revokeAllMutation = useMutation({
    mutationFn: api.revokeAllSessions,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sessions'] });
      toast.success('All sessions revoked');
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Revoke failed')
  });

  return (
    <Stack spacing={2}>
      <Card variant="outlined">
        <CardContent>
          <CardTitle>Account security</CardTitle>
          <Stack spacing={2} sx={{ maxWidth: 480 }}>
            <Typography variant="body2" color="text.secondary">
              Signed in as <strong>{user?.email}</strong>
            </Typography>
            <Button variant="outlined" onClick={() => toast.success('Password reset email sent')}>
              Change password
            </Button>
          </Stack>
        </CardContent>
      </Card>
      <Card variant="outlined">
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
            <CardTitle>Active sessions</CardTitle>
            <Button size="small" variant="outlined" color="error" onClick={() => revokeAllMutation.mutate()}>
              Revoke all
            </Button>
          </Box>
          {sessions.isLoading ? (
            <BoxLoader rows={2} />
          ) : (sessions.data ?? []).length === 0 ? (
            <Typography variant="body2" color="text.secondary">
              No active sessions.
            </Typography>
          ) : (
            <Stack divider={<Divider />} spacing={1.5}>
              {sessions.data?.map((session) => (
                <Box key={session.id} sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <Box sx={{ flex: 1 }}>
                    <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                      {session.deviceId || session.userAgent || 'Unknown device'}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {session.ipAddress ?? 'Unknown IP'} · Last used {formatDateTime(session.lastUsedAt)}
                    </Typography>
                  </Box>
                  <StatusChip status={session.status} />
                  {session.status === 'ACTIVE' ? (
                    <Button size="small" color="error" onClick={() => revokeMutation.mutate(session.id)}>
                      Revoke
                    </Button>
                  ) : null}
                </Box>
              ))}
            </Stack>
          )}
        </CardContent>
      </Card>
    </Stack>
  );
}
