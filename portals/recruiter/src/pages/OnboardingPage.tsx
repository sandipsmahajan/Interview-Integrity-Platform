import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { Business, CheckCircle, Palette, Shield, VerifiedUser } from '@mui/icons-material';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import MenuItem from '@mui/material/MenuItem';
import Stepper from '@mui/material/Stepper';
import Step from '@mui/material/Step';
import StepLabel from '@mui/material/StepLabel';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import Grid from '@mui/material/Grid';
import toast from 'react-hot-toast';
import { api } from '../lib/api';
import { PageHeader } from '../components/PageHeader';
import BoxLoader from '../components/BoxLoader';
import { useAuth } from '../hooks/useAuth';

const INDUSTRIES = ['Technology', 'Finance', 'Healthcare', 'Education', 'Retail', 'Manufacturing', 'Other'];
const SIZES = ['1-50', '51-200', '201-1000', '1001-5000', '5000+'];
const TIMEZONES = ['UTC', 'America/New_York', 'America/Los_Angeles', 'Europe/London', 'Europe/Berlin', 'Asia/Kolkata', 'Asia/Singapore', 'Australia/Sydney'];

const detailsSchema = z.object({
  name: z.string().min(1, 'Company name is required').max(120),
  legalName: z.string().max(160).optional().or(z.literal('')),
  industry: z.string().min(1, 'Select an industry'),
  size: z.string().min(1, 'Select a size'),
  timezone: z.string().min(1, 'Select a timezone'),
  address: z.string().max(300).optional().or(z.literal(''))
});

type DetailsValues = z.infer<typeof detailsSchema>;

const STEPS = ['Company details', 'Branding & notifications', 'SSO & security', 'Review'];

export function OnboardingPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const [step, setStep] = useState(0);
  const [branding, setBranding] = useState({ primaryColor: '#2563eb', logo: '' });
  const [notifications, setNotifications] = useState({ interviewReminders: true, alerts: true, reports: true });
  const [sso, setSso] = useState({ enabled: false, provider: 'SAML 2.0', domain: '' });

  const organization = useQuery({ queryKey: ['organization'], queryFn: () => api.getOrganization() });

  const updateMutation = useMutation({
    mutationFn: api.updateOrganization,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organization'] });
      toast.success('Organization updated');
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Update failed')
  });

  const settings = useMemo(() => {
    try {
      return organization.data ? JSON.parse(organization.data.settings || '{}') : {};
    } catch {
      return {};
    }
  }, [organization.data]);

  const detailsForm = useForm<DetailsValues>({
    resolver: zodResolver(detailsSchema),
    values: {
      name: organization.data?.name ?? '',
      legalName: organization.data?.legalName ?? '',
      industry: (settings.industry as string) ?? '',
      size: (settings.size as string) ?? '',
      timezone: (settings.timezone as string) ?? '',
      address: (settings.address as string) ?? ''
    }
  });

  if (organization.isLoading) return <BoxLoader rows={5} />;

  function saveDetails(values: DetailsValues) {
    const nextSettings = { ...settings, industry: values.industry, size: values.size, timezone: values.timezone, address: values.address };
    updateMutation.mutate(
      { name: values.name, legalName: values.legalName || null, settings: JSON.stringify(nextSettings) },
      { onSuccess: () => setStep(1) }
    );
  }

  function finish() {
    toast.success('Onboarding complete. Welcome to Integrity Pro.');
    navigate('/');
  }

  return (
    <Box>
      <PageHeader
        title="Set up your workspace"
        subtitle="Complete a few steps to configure your organization."
      />
      <Stepper activeStep={step} alternativeLabel sx={{ mb: 4 }}>
        {STEPS.map((label) => (
          <Step key={label}>
            <StepLabel>{label}</StepLabel>
          </Step>
        ))}
      </Stepper>

      <Card variant="outlined" sx={{ maxWidth: 720, mx: 'auto' }}>
        <CardContent sx={{ p: 4 }}>
          {step === 0 ? (
            <form onSubmit={detailsForm.handleSubmit(saveDetails)}>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                <Business color="primary" />
                Company details
              </Typography>
              <Grid container spacing={2}>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField label="Company name" fullWidth {...detailsForm.register('name')} error={Boolean(detailsForm.formState.errors.name)} helperText={detailsForm.formState.errors.name?.message} />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField label="Legal name" fullWidth {...detailsForm.register('legalName')} error={Boolean(detailsForm.formState.errors.legalName)} helperText={detailsForm.formState.errors.legalName?.message} />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField select label="Industry" fullWidth {...detailsForm.register('industry')} error={Boolean(detailsForm.formState.errors.industry)} helperText={detailsForm.formState.errors.industry?.message}>
                    {INDUSTRIES.map((industry) => (
                      <MenuItem key={industry} value={industry}>
                        {industry}
                      </MenuItem>
                    ))}
                  </TextField>
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField select label="Company size" fullWidth {...detailsForm.register('size')} error={Boolean(detailsForm.formState.errors.size)} helperText={detailsForm.formState.errors.size?.message}>
                    {SIZES.map((size) => (
                      <MenuItem key={size} value={size}>
                        {size}
                      </MenuItem>
                    ))}
                  </TextField>
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField select label="Timezone" fullWidth {...detailsForm.register('timezone')} error={Boolean(detailsForm.formState.errors.timezone)} helperText={detailsForm.formState.errors.timezone?.message}>
                    {TIMEZONES.map((timezone) => (
                      <MenuItem key={timezone} value={timezone}>
                        {timezone}
                      </MenuItem>
                    ))}
                  </TextField>
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField label="Address" fullWidth {...detailsForm.register('address')} error={Boolean(detailsForm.formState.errors.address)} helperText={detailsForm.formState.errors.address?.message} />
                </Grid>
              </Grid>
              <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 3 }}>
                <Button type="submit" variant="contained" disabled={updateMutation.isPending}>
                  {updateMutation.isPending ? 'Saving...' : 'Save and continue'}
                </Button>
              </Box>
            </form>
          ) : null}

          {step === 1 ? (
            <Box>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                <Palette color="primary" />
                Branding & notifications
              </Typography>
              <TextField label="Primary color" fullWidth value={branding.primaryColor} onChange={(event) => setBranding({ ...branding, primaryColor: event.target.value })} sx={{ mb: 2 }} />
              <TextField label="Company logo URL" fullWidth value={branding.logo} onChange={(event) => setBranding({ ...branding, logo: event.target.value })} placeholder="https://..." helperText="Used in email templates and login screen." sx={{ mb: 3 }} />
              <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                Email notifications
              </Typography>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                {(
                  [
                    ['interviewReminders', 'Interview reminders', notifications.interviewReminders],
                    ['alerts', 'Integrity alerts', notifications.alerts],
                    ['reports', 'Report ready', notifications.reports]
                  ] as const
                ).map(([key, label, value]) => (
                  <Button
                    key={key}
                    variant={value ? 'contained' : 'outlined'}
                    onClick={() => setNotifications({ ...notifications, [key]: !value })}
                    sx={{ justifyContent: 'flex-start' }}
                  >
                    {value ? <CheckCircle sx={{ mr: 1, fontSize: 18 }} /> : null} {label}: {value ? 'On' : 'Off'}
                  </Button>
                ))}
              </Box>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 2 }}>
                Saved locally for this wizard. Backend delivery preference endpoints are planned in the notification service.
              </Typography>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 3 }}>
                <Button onClick={() => setStep(0)}>Back</Button>
                <Button variant="contained" onClick={() => setStep(2)}>
                  Continue
                </Button>
              </Box>
            </Box>
          ) : null}

          {step === 2 ? (
            <Box>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                <Shield color="primary" />
                SSO & security
              </Typography>
              <TextField select label="SSO provider" fullWidth value={sso.provider} onChange={(event) => setSso({ ...sso, provider: event.target.value })} sx={{ mb: 2 }}>
                {['SAML 2.0', 'OIDC'].map((provider) => (
                  <MenuItem key={provider} value={provider}>
                    {provider}
                  </MenuItem>
                ))}
              </TextField>
              <TextField label="Company domain" fullWidth placeholder="acme.com" value={sso.domain} onChange={(event) => setSso({ ...sso, domain: event.target.value })} helperText="Users on this domain can sign in via SSO." sx={{ mb: 2 }} />
              <Button variant={sso.enabled ? 'contained' : 'outlined'} onClick={() => setSso({ ...sso, enabled: !sso.enabled })} fullWidth sx={{ justifyContent: 'flex-start' }}>
                {sso.enabled ? <CheckCircle sx={{ mr: 1, fontSize: 18 }} /> : null} Enforce SSO: {sso.enabled ? 'On' : 'Off'}
              </Button>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 2 }}>
                SSO enforcement requires an IdP connection. This wizard records the preference for your identity provider integration.
              </Typography>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 3 }}>
                <Button onClick={() => setStep(1)}>Back</Button>
                <Button variant="contained" onClick={() => setStep(3)}>
                  Continue
                </Button>
              </Box>
            </Box>
          ) : null}

          {step === 3 ? (
            <Box>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                <VerifiedUser color="success" />
                Review & complete
              </Typography>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, mb: 3 }}>
                <Typography variant="body2">
                  Organization: <strong>{organization.data?.name}</strong> ({organization.data?.slug})
                </Typography>
                <Typography variant="body2">
                  Industry: <strong>{detailsForm.getValues('industry')}</strong> · Size:{' '}
                  <strong>{detailsForm.getValues('size')}</strong>
                </Typography>
                <Typography variant="body2">
                  Branding color: <strong>{branding.primaryColor}</strong>
                </Typography>
                <Typography variant="body2">
                  SSO: <strong>{sso.enabled ? `Enforced via ${sso.provider}` : 'Not enforced'}</strong>
                </Typography>
                <Typography variant="body2">
                  Administrator: <strong>{user?.displayName}</strong> ({user?.email})
                </Typography>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                <Button onClick={() => setStep(2)}>Back</Button>
                <Button variant="contained" onClick={finish}>
                  Complete setup
                </Button>
              </Box>
            </Box>
          ) : null}
        </CardContent>
      </Card>
    </Box>
  );
}
