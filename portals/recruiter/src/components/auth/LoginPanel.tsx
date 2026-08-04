import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Checkbox from '@mui/material/Checkbox';
import CircularProgress from '@mui/material/CircularProgress';
import FormControlLabel from '@mui/material/FormControlLabel';
import IconButton from '@mui/material/IconButton';
import InputAdornment from '@mui/material/InputAdornment';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { Visibility, VisibilityOff } from '@mui/icons-material';
import toast from 'react-hot-toast';
import { useAuth } from '../../hooks/useAuth';
import type { MfaChallengeResponse } from '../../lib/types';

const credentialsSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required').min(8, 'Password must be at least 8 characters'),
  rememberMe: z.boolean()
});

const challengeSchema = z.object({
  code: z.string().min(6, 'Enter the six digit code').max(32, 'Code is too long'),
  trustDevice: z.boolean()
});

type CredentialsValues = z.infer<typeof credentialsSchema>;
type ChallengeValues = z.infer<typeof challengeSchema>;

const DEVICE_ID = 'web-portal';

/**
 * Right-hand authentication panel for the login screen. Owns the credentials
 * and MFA challenge forms while keeping the exact backend auth workflow.
 * Enterprise email authentication only — no social login options.
 */
export function LoginPanel() {
  const { login, mfaVerify, mfaEmailOtp } = useAuth();
  const navigate = useNavigate();
  const [challenge, setChallenge] = useState<MfaChallengeResponse | null>(null);
  const [sendingCode, setSendingCode] = useState(false);
  const [emailForChallenge, setEmailForChallenge] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [capsLock, setCapsLock] = useState(false);

  const credentialsForm = useForm<CredentialsValues>({
    resolver: zodResolver(credentialsSchema),
    defaultValues: { email: '', password: '', rememberMe: true }
  });

  const challengeForm = useForm<ChallengeValues>({
    resolver: zodResolver(challengeSchema),
    defaultValues: { code: '', trustDevice: true }
  });

  async function onSubmit(values: CredentialsValues) {
    try {
      const response = await login(
        { email: values.email, password: values.password, organizationId: null },
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
      toast.error(error instanceof Error ? error.message : 'Sign in failed');
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
      toast.error(error instanceof Error ? error.message : 'Verification failed');
    }
  }

  async function handleSendCode() {
    if (!challenge) return;
    setSendingCode(true);
    try {
      await mfaEmailOtp(challenge.challengeId);
      toast.success('A verification code was sent to your email');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to send code');
    } finally {
      setSendingCode(false);
    }
  }

  if (challenge) {
    const supportsEmail = challenge.channels.includes('EMAIL');
    return (
      <Box>
        <Typography variant="h5" sx={{ fontWeight: 800, mb: 0.5 }}>
          Two-factor verification
        </Typography>
        <Typography variant="body2" sx={{ color: 'text.secondary', mb: 3 }}>
          Verify your sign in to {emailForChallenge}.
        </Typography>
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
              <Button type="button" variant="text" size="small" onClick={handleSendCode} disabled={sendingCode}>
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
      </Box>
    );
  }

  const passwordError = credentialsForm.formState.errors.password?.message;

  return (
    <Box>
      <Typography variant="h5" sx={{ fontWeight: 800, mb: 0.5 }}>
        Welcome back
      </Typography>
      <Typography variant="body2" sx={{ color: 'text.secondary', mb: 3 }}>
        Sign in to your organization workspace.
      </Typography>

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
          type={showPassword ? 'text' : 'password'}
          fullWidth
          margin="normal"
          autoComplete="current-password"
          slotProps={{
            input: {
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                    onClick={() => setShowPassword((value) => !value)}
                    onMouseDown={(event) => event.preventDefault()}
                    edge="end"
                  >
                    {showPassword ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              )
            }
          }}
          onKeyUp={(event) => {
            setCapsLock(event.nativeEvent.getModifierState?.('CapsLock') ?? false);
          }}
          {...credentialsForm.register('password')}
          error={Boolean(passwordError)}
          helperText={capsLock ? 'Caps Lock is on' : passwordError}
        />
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mt: 1 }}>
          <FormControlLabel
            control={<Checkbox {...credentialsForm.register('rememberMe')} />}
            label={<Typography variant="body2">Remember me</Typography>}
          />
          <Link to="/forgot-password">
            <Typography variant="body2" sx={{ color: 'primary.main', fontWeight: 600 }}>
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
          sx={{ mt: 2, py: 1.2 }}
        >
          {credentialsForm.formState.isSubmitting && <CircularProgress size={16} color="inherit" sx={{ mr: 1 }} />}
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
    </Box>
  );
}
