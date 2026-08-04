import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router-dom';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import toast from 'react-hot-toast';
import { AuthenticationLayout, AuthHeading } from '../../components/auth/AuthenticationLayout';
import { useAuth } from '../../hooks/useAuth';

const schema = z
  .object({
    companyName: z.string().min(1, 'Company name is required').max(120),
    adminDisplayName: z.string().min(1, 'Your name is required').max(120),
    adminEmail: z.string().min(1, 'Email is required').email('Enter a valid email address'),
    adminPassword: z.string().min(8, 'Password must be at least 8 characters').max(128),
    confirmPassword: z.string().min(1, 'Confirm your password')
  })
  .refine((data) => data.adminPassword === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword']
  });

type FormValues = z.infer<typeof schema>;

export function RegisterPage() {
  const { register: registerOrg } = useAuth();
  const navigate = useNavigate();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      companyName: '',
      adminDisplayName: '',
      adminEmail: '',
      adminPassword: '',
      confirmPassword: ''
    }
  });

  async function onSubmit(values: FormValues) {
    try {
      await registerOrg({
        companyName: values.companyName,
        adminDisplayName: values.adminDisplayName,
        adminEmail: values.adminEmail,
        adminPassword: values.adminPassword
      });
      toast.success('Organization registered. Welcome to Integrity Pro.');
      navigate('/verify-email');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Registration failed');
    }
  }

  return (
    <AuthenticationLayout>
      <AuthHeading title="Create your workspace" subtitle="Register your organization and first administrator." />
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <TextField
          label="Company name"
          fullWidth
          margin="normal"
          autoFocus
          {...register('companyName')}
          error={Boolean(errors.companyName)}
          helperText={errors.companyName?.message}
        />
        <TextField
          label="Your full name"
          fullWidth
          margin="normal"
          {...register('adminDisplayName')}
          error={Boolean(errors.adminDisplayName)}
          helperText={errors.adminDisplayName?.message}
        />
        <TextField
          label="Work email"
          type="email"
          fullWidth
          margin="normal"
          autoComplete="email"
          {...register('adminEmail')}
          error={Boolean(errors.adminEmail)}
          helperText={errors.adminEmail?.message}
        />
        <TextField
          label="Password"
          type="password"
          fullWidth
          margin="normal"
          autoComplete="new-password"
          {...register('adminPassword')}
          error={Boolean(errors.adminPassword)}
          helperText={errors.adminPassword?.message}
        />
        <TextField
          label="Confirm password"
          type="password"
          fullWidth
          margin="normal"
          autoComplete="new-password"
          {...register('confirmPassword')}
          error={Boolean(errors.confirmPassword)}
          helperText={errors.confirmPassword?.message}
        />
        <Button type="submit" variant="contained" fullWidth size="large" disabled={isSubmitting} sx={{ mt: 2 }}>
          {isSubmitting ? 'Creating workspace...' : 'Create workspace'}
        </Button>
      </form>
      <Box sx={{ mt: 3, textAlign: 'center' }}>
        <Typography variant="body2" color="text.secondary">
          Already have an account?{' '}
          <Link to="/login" style={{ color: 'primary.main' }}>
            Sign in
          </Link>
        </Typography>
      </Box>
    </AuthenticationLayout>
  );
}
