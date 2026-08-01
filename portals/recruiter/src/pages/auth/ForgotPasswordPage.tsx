import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router-dom';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import { useState } from 'react';
import toast from 'react-hot-toast';
import { api } from '../../lib/api';
import { AuthLayout } from '../../components/AuthLayout';

const schema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email address')
});

type FormValues = z.infer<typeof schema>;

export function ForgotPasswordPage() {
  const [sent, setSent] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  async function onSubmit(values: FormValues) {
    try {
      await api.requestPasswordReset(values.email);
      setSent(true);
      toast.success('Password reset email sent');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Request failed');
    }
  }

  return (
    <AuthLayout title="Reset your password" subtitle="We will email you a link to create a new password.">
      {sent ? (
        <Box sx={{ textAlign: 'center', py: 2 }}>
          <Typography variant="body1" sx={{ mb: 2 }}>
            If an account exists for that email, a password reset link is on its way.
          </Typography>
          <Link to="/login" style={{ color: 'primary.main' }}>
            <Typography variant="body2" color="primary">
              Back to sign in
            </Typography>
          </Link>
        </Box>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <TextField
            label="Work email"
            type="email"
            fullWidth
            margin="normal"
            autoComplete="email"
            autoFocus
            {...register('email')}
            error={Boolean(errors.email)}
            helperText={errors.email?.message}
          />
          <Button type="submit" variant="contained" fullWidth size="large" disabled={isSubmitting} sx={{ mt: 2 }}>
            {isSubmitting ? 'Sending...' : 'Send reset link'}
          </Button>
        </form>
      )}
    </AuthLayout>
  );
}
