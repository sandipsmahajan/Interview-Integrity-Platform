import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import { useState } from 'react';
import toast from 'react-hot-toast';
import { api } from '../../lib/api';
import { AuthLayout } from '../../components/AuthLayout';

const schema = z
  .object({
    token: z.string().min(1, 'Reset token is required'),
    newPassword: z.string().min(8, 'Password must be at least 8 characters').max(128),
    confirmPassword: z.string().min(1, 'Confirm your password')
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword']
  });

type FormValues = z.infer<typeof schema>;

export function ResetPasswordPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [done, setDone] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { token: params.get('token') ?? '', newPassword: '', confirmPassword: '' }
  });

  async function onSubmit(values: FormValues) {
    try {
      await api.resetPassword({ token: values.token, newPassword: values.newPassword });
      setDone(true);
      toast.success('Password updated');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Reset failed');
    }
  }

  return (
    <AuthLayout title="Choose a new password" subtitle="Enter the reset token from your email.">
      {done ? (
        <Box sx={{ textAlign: 'center', py: 2 }}>
          <Typography variant="body1" sx={{ mb: 2 }}>
            Your password has been updated. You can now sign in.
          </Typography>
          <Button variant="contained" onClick={() => navigate('/login')}>
            Go to sign in
          </Button>
        </Box>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <TextField
            label="Reset token"
            fullWidth
            margin="normal"
            {...register('token')}
            error={Boolean(errors.token)}
            helperText={errors.token?.message ?? 'In local development the token is returned by the reset-request API.'}
          />
          <TextField
            label="New password"
            type="password"
            fullWidth
            margin="normal"
            autoComplete="new-password"
            {...register('newPassword')}
            error={Boolean(errors.newPassword)}
            helperText={errors.newPassword?.message}
          />
          <TextField
            label="Confirm new password"
            type="password"
            fullWidth
            margin="normal"
            autoComplete="new-password"
            {...register('confirmPassword')}
            error={Boolean(errors.confirmPassword)}
            helperText={errors.confirmPassword?.message}
          />
          <Button type="submit" variant="contained" fullWidth size="large" disabled={isSubmitting} sx={{ mt: 2 }}>
            {isSubmitting ? 'Updating...' : 'Update password'}
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
    </AuthLayout>
  );
}
