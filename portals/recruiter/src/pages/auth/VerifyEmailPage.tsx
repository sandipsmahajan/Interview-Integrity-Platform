import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router-dom';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import { useState } from 'react';
import toast from 'react-hot-toast';
import { api } from '../../lib/api';
import { AuthenticationLayout, AuthHeading } from '../../components/auth/AuthenticationLayout';

const schema = z.object({
  token: z.string().min(1, 'Verification code is required')
});

type FormValues = z.infer<typeof schema>;

export function VerifyEmailPage() {
  const navigate = useNavigate();
  const [done, setDone] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  async function onSubmit(values: FormValues) {
    try {
      await api.verifyEmail({ token: values.token });
      setDone(true);
      toast.success('Email verified');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Verification failed');
    }
  }

  return (
    <AuthenticationLayout>
      <AuthHeading title="Verify your email" subtitle="Enter the one-time verification token from your email." />
      {done ? (
        <Box sx={{ textAlign: 'center', py: 2 }}>
          <Typography variant="body1" sx={{ mb: 2 }}>
            Your email is verified and your organization is ready.
          </Typography>
          <Button variant="contained" onClick={() => navigate('/onboarding')}>
            Continue to onboarding
          </Button>
        </Box>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <TextField
            label="Verification token"
            fullWidth
            margin="normal"
            autoFocus
            {...register('token')}
            error={Boolean(errors.token)}
            helperText={errors.token?.message ?? 'In local development, request the token from the verify flow documentation.'}
          />
          <Button type="submit" variant="contained" fullWidth size="large" disabled={isSubmitting} sx={{ mt: 2 }}>
            {isSubmitting ? 'Verifying...' : 'Verify email'}
          </Button>
        </form>
      )}
      <Box sx={{ mt: 3, textAlign: 'center' }}>
        <Link to="/login" style={{ color: 'primary.main' }}>
          <Typography variant="body2" color="primary">
            Back to sign in
          </Typography>
        </Link>
      </Box>
    </AuthenticationLayout>
  );
}
