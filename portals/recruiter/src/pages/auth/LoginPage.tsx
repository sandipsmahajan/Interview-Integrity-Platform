import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import Button from '@mui/material/Button';
import Checkbox from '@mui/material/Checkbox';
import FormControlLabel from '@mui/material/FormControlLabel';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import toast from 'react-hot-toast';
import { AuthLayout } from '../../components/AuthLayout';
import { useAuth } from '../../hooks/useAuth';
import type { MfaChallengeResponse } from '../../lib/types';

const credentialsSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required').min(8, 'Password must be at least 8 characters'),
  organizationId: z.string().uuid('Enter a valid organization id').optional().or(z.literal('')),
  rememberMe: z.boolean()
});

const challengeSchema = z.object({
  code: z.string().min(6, 'Enter the six digit code').max(32, 'Code is too long'),
  trustDevice: z.boolean()
});

type CredentialsValues = z.infer<typeof credentialsSchema>;
type ChallengeValues = z.infer<typeof challengeSchema>;

const DEVICE_ID = 'web-portal';

export function LoginPage() {
  const { login, mfaVerify, mfaEmailOtp } = useAuth();
  const navigate = useNavigate();
  const [challenge, setChallenge] = useState<MfaChallengeResponse | null>(null);
  const [sendingCode, setSendingCode] = useState(false);
  const [emailForChallenge, setEmailForChallenge] = useState('');

  const credentialsForm = useForm<CredentialsValues>({
    resolver: zodResolver(credentialsSchema),
    defaultValues: { email: '', password: '', organizationId: '', rememberMe: true }
  });

  const challengeForm = useForm<ChallengeValues>({
    resolver: zodResolver(challengeSchema),
    defaultValues: { code: '', trustDevice: true }
  });

  async function onSubmit(values: CredentialsValues) {
    try {
      const response = await login(
        {
          email: values.email,
          password: values.password,
          organizationId: values.organizationId ? values.organizationId : null
        },
        values.rememberMe
      );
      if ('accessToken' in response) {
        toast.success('Signed in successfully');
        navigate('/');
        return;
      }
      setChallenge(response);
      setEmailForChallenge(values.email);
      challengeForm.reset();
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Sign in failed';
      toast.error(message);
    }
  }

  async function onVerifyChallenge(values: ChallengeValues) {
    if (!challenge) return;
    try {
      await mfaVerify({
        challengeId: challenge.challengeId,
        code: values.code,
        trustDevice: values.trustDevice,
        deviceId: DEVICE_ID,
        deviceName: 'Web portal'
      });
      toast.success('Signed in successfully');
      navigate('/');
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Verification failed';
      toast.error(message);
    }
  }

  async function handleSendCode() {
    if (!challenge) return;
    setSendingCode(true);
    try {
      await mfaEmailOtp(challenge.challengeId);
      toast.success('A verification code was sent to your email');
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to send code';
      toast.error(message);
    } finally {
      setSendingCode(false);
    }
  }

  if (challenge) {
    const supportsEmail = challenge.channels.includes('EMAIL');
    return (
      <AuthLayout title="Two-factor verification" subtitle={`Verify your sign in to ${emailForChallenge}.`}>
        <form onSubmit={challengeForm.handleSubmit(onVerifyChallenge)} noValidate>
          <TextField
            label="Verification code"
            placeholder="000000"
            fullWidth
            margin="normal"
            autoFocus
            slotProps={{ htmlInput: { inputMode: 'numeric', autoComplete: 'one-time-code', maxLength: 32 } }}
            {...challengeForm.register('code')}
            error={Boolean(challengeForm.formState.errors.code)}
            helperText={challengeForm.formState.errors.code?.message}
          />
          {supportsEmail && (
            <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
              <Button
                type="button"
                variant="text"
                size="small"
                onClick={handleSendCode}
                disabled={sendingCode}
              >
                {sendingCode ? 'Sending...' : 'Send code by email'}
              </Button>
            </Box>
          )}
          <FormControlLabel
            control={<Checkbox {...challengeForm.register('trustDevice')} />}
            label={<Typography variant="body2">Trust this device for 30 days</Typography>}
            sx={{ mt: 1 }}
          />
          <Stack direction="row" spacing={1} sx={{ mt: 2 }}>
            <Button
              type="submit"
              variant="contained"
              fullWidth
              size="large"
              disabled={challengeForm.formState.isSubmitting}
            >
              {challengeForm.formState.isSubmitting ? 'Verifying...' : 'Verify and continue'}
            </Button>
            <Button
              type="button"
              variant="outlined"
              size="large"
              onClick={() => {
                setChallenge(null);
                challengeForm.reset();
              }}
            >
              Back
            </Button>
          </Stack>
        </form>
        <Box sx={{ mt: 3, textAlign: 'center' }}>
          <Typography variant="body2" color="text.secondary">
            Use a code from your authenticator app, an email code, or a recovery code.
          </Typography>
        </Box>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout title="Welcome back" subtitle="Sign in to your organization workspace.">
      <form onSubmit={credentialsForm.handleSubmit(onSubmit)} noValidate>
        <TextField
          label="Work email"
          type="email"
          fullWidth
          margin="normal"
          autoComplete="email"
          autoFocus
          {...credentialsForm.register('email')}
          error={Boolean(credentialsForm.formState.errors.email)}
          helperText={credentialsForm.formState.errors.email?.message}
        />
        <TextField
          label="Password"
          type="password"
          fullWidth
          margin="normal"
          autoComplete="current-password"
          {...credentialsForm.register('password')}
          error={Boolean(credentialsForm.formState.errors.password)}
          helperText={credentialsForm.formState.errors.password?.message}
        />
        <TextField
          label="Organization ID (optional)"
          placeholder="00000000-0000-0000-0000-000000000000"
          fullWidth
          margin="normal"
          {...credentialsForm.register('organizationId')}
          error={Boolean(credentialsForm.formState.errors.organizationId)}
          helperText={credentialsForm.formState.errors.organizationId?.message}
        />
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mt: 1 }}>
          <FormControlLabel
            control={<Checkbox {...credentialsForm.register('rememberMe')} />}
            label={<Typography variant="body2">Remember me</Typography>}
          />
          <Link to="/forgot-password" style={{ color: 'primary.main' }}>
            <Typography variant="body2" color="primary">
              Forgot password?
            </Typography>
          </Link>
        </Box>
        <Button
          type="submit"
          variant="contained"
          fullWidth
          size="large"
          disabled={credentialsForm.formState.isSubmitting}
          sx={{ mt: 2 }}
        >
          {credentialsForm.formState.isSubmitting ? 'Signing in...' : 'Sign in'}
        </Button>
      </form>
      <Box sx={{ mt: 3, textAlign: 'center' }}>
        <Typography variant="body2" color="text.secondary">
          New to Integrity Pro?{' '}
          <Link to="/register" style={{ color: 'primary.main' }}>
            Create an organization
          </Link>
        </Typography>
      </Box>
    </AuthLayout>
  );
}
